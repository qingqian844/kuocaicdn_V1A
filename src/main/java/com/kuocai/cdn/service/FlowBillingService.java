package com.kuocai.cdn.service;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kuocai.cdn.api.huawei.cdn.constant.DomainStatus;
import com.kuocai.cdn.async.SmsAsync;
import com.kuocai.cdn.common.mongo.entity.FlowBillingCarry;
import com.kuocai.cdn.common.mongo.entity.FlowBillingLogic;
import com.kuocai.cdn.constant.PurchasedFlowConstants;
import com.kuocai.cdn.constant.TransactionOrderPayType;
import com.kuocai.cdn.constant.TransactionOrderStatus;
import com.kuocai.cdn.constant.TransactionOrderType;
import com.kuocai.cdn.entity.*;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.domain.operation.ICdnPlatformService;
import com.kuocai.cdn.service.factory.CdnPlatformFactory;
import com.kuocai.cdn.util.*;
import com.kuocai.cdn.vo.PurchasedFlowVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FlowBillingService {

    private static final BigDecimal FLOW_MIN_BILLING_AMOUNT = BigDecimal.ONE;
    private static final BigDecimal BYTES_PER_GB = BigDecimal.valueOf(1024L).pow(3);
    private static final int FLOW_BILLING_FLOW_SCALE = 8;
    private static final int FLOW_BILLING_AMOUNT_SCALE = 6;

    private final SysUserService sysUserService;
    private final CdnDomainService cdnDomainService;
    private final PurchasedFlowService purchasedFlowService;
    private final CdnDomainStatisticsService cdnDomainStatisticsService;
    private final TransactionOrderService transactionOrderService;
    private final SysUserAccountService sysUserAccountService;
    private final SmsAsync smsAsync;
    private final PurchasedFlowDetailService purchasedFlowDetailService;
    private final MongoTemplate mongoTemplate;

    FlowBillingService(SysUserService sysUserService, CdnDomainService cdnDomainService, PurchasedFlowService purchasedFlowService,
                       CdnDomainStatisticsService cdnDomainStatisticsService, TransactionOrderService transactionOrderService, SysUserAccountService sysUserAccountService,
                       SmsAsync smsAsync, PurchasedFlowDetailService purchasedFlowDetailService, MongoTemplate mongoTemplate) {
        this.sysUserService = sysUserService;
        this.cdnDomainService = cdnDomainService;
        this.purchasedFlowService = purchasedFlowService;
        this.cdnDomainStatisticsService = cdnDomainStatisticsService;
        this.transactionOrderService = transactionOrderService;
        this.sysUserAccountService = sysUserAccountService;
        this.smsAsync = smsAsync;
        this.purchasedFlowDetailService = purchasedFlowDetailService;
        this.mongoTemplate = mongoTemplate;
    }

    private DateTime getCurrentHour() {
        return DateUtil.beginOfHour(DateUtil.date());
    }

    private String toDomainNames(List<CdnDomain> userCdnDomains) {
        return userCdnDomains.stream().map(CdnDomain::getDomainName).sorted().collect(Collectors.joining(","));
    }

    private String getResourceCacheKey(Long userId, String domainNames, DateTime start, DateTime end) {
        return String.format("Statistics:%d:%s:%s:%s->%s", userId, "Resource", domainNames.hashCode(), start.getTime(), end.getTime());
    }

    private Long getUsedFlow(JSONObject resource) {
        log.debug("getUsedFlow - resource: {}", resource);
        return extractUsedFlow(resource);
    }

    public static Long extractUsedFlow(JSONObject resource) {
        // 检查resource是否为空
        if (resource == null) {
            log.error("resource is null");
            return 0L;
        }

        // 首先获取Resource对象
        JSONObject resourceData = resource.containsKey("Resource")
                ? resource.getJSONObject("Resource")
                : resource;
        if (resourceData == null) {
            log.error("Resource object is null");
            return 0L;
        }

        // 然后从Resource对象中获取resource_summary
        if (!resourceData.containsKey("resource_summary")) {
            log.error("resource_summary key not found in Resource: {}", resourceData.keySet());
            return 0L;
        }

        Object resourceSummaryObj = resourceData.get("resource_summary");
        log.debug("resourceSummaryObj type: {}, value: {}",
                resourceSummaryObj != null ? resourceSummaryObj.getClass().getName() : "null",
                resourceSummaryObj);

        // 尝试不同的方式获取resource_summary
        JSONObject resourceSummary = null;
        if (resourceSummaryObj instanceof JSONObject) {
            resourceSummary = (JSONObject) resourceSummaryObj;
        } else if (resourceSummaryObj != null) {
            try {
                resourceSummary = resourceData.getJSONObject("resource_summary");
            } catch (Exception e) {
                log.error("Failed to get resource_summary as JSONObject: {}", e.getMessage());
                // 尝试其他方式
                try {
                    String jsonStr = resourceSummaryObj.toString();
                    resourceSummary = JSONObject.parseObject(jsonStr);
                } catch (Exception e2) {
                    log.error("Failed to parse resource_summary from string: {}", e2.getMessage());
                    return 0L;
                }
            }
        }

        if (resourceSummary == null) {
            log.error("resourceSummary is null after all attempts");
            return 0L;
        }

        log.debug("resourceSummary: {}", resourceSummary);

        if (resourceSummary.containsKey("access_flux_byte")) {
            Long accessFluxByte = resourceSummary.getLong("access_flux_byte");
            log.debug("access_flux_byte value: {}", accessFluxByte);
            return accessFluxByte != null ? accessFluxByte : 0L;
        }

        Long fluxByte = resourceSummary.getLong("flux_byte");
        if (fluxByte == null && !resourceSummary.containsKey("flux_byte")) {
            log.error("billing access flow byte key not found in resourceSummary: {}", resourceSummary.keySet());
        }
        log.debug("flux_byte value: {}", fluxByte);

        return ObjectUtil.defaultIfNull(fluxByte, 0L);
    }

    public static BigDecimal flowBytesToBillingGB(long flowBytes) {
        if (flowBytes <= 0) {
            return BigDecimal.ZERO.setScale(FLOW_BILLING_FLOW_SCALE);
        }
        return BigDecimal.valueOf(flowBytes).divide(BYTES_PER_GB, FLOW_BILLING_FLOW_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateFlowBillingAmount(BigDecimal flowPrice, long flowBytes) {
        if (flowPrice == null || flowBytes <= 0) {
            return BigDecimal.ZERO.setScale(FLOW_BILLING_AMOUNT_SCALE);
        }
        return flowPrice.multiply(flowBytesToBillingGB(flowBytes))
                .setScale(FLOW_BILLING_AMOUNT_SCALE, RoundingMode.HALF_UP);
    }

    private static String formatBillingDecimal(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        BigDecimal normalized = value.stripTrailingZeros();
        if (normalized.scale() < 0) {
            normalized = normalized.setScale(0);
        }
        return normalized.toPlainString();
    }

    private JSONObject getResource(Long userId, List<CdnDomain> userCdnDomains, DateTime start, DateTime end) throws BusinessException {
        String domainNames = toDomainNames(userCdnDomains);
        String cacheKey = getResourceCacheKey(userId, domainNames, start, end);
        log.debug("getResource - userId: {}, domainNames: {}, cacheKey: {}", userId, domainNames, cacheKey);
        JedisUtil.delKey(cacheKey);

        JSONObject result = cdnDomainStatisticsService.mergeAllPlatForm(userCdnDomains, start, end, "Resource", userId);
        log.debug("getResource - result from mergeAllPlatForm: {}", result);

        JSONObject resourceData = result != null && result.containsKey("Resource")
                ? result.getJSONObject("Resource")
                : result;
        if (resourceData != null && resourceData.containsKey("resource_summary")) {
            Object resourceSummary = resourceData.get("resource_summary");
            log.debug("getResource - resource_summary type: {}, value: {}",
                    resourceSummary != null ? resourceSummary.getClass().getName() : "null",
                    resourceSummary);
        } else {
            log.error("getResource - result is null or doesn't contain resource_summary key");
        }

        return result;
    }

    private Map<Long, List<CdnDomain>> groupByUserId(List<CdnDomain> cdnDomains) {
        return cdnDomains.stream().collect(Collectors.groupingBy(CdnDomain::getUserId));
    }

    private boolean domainsIsEqual(String domainNames, String domains) {
        String[] domainNamesArray = domainNames.split(",");
        String[] domainsArray = domains.split(",");
        return Arrays.equals(domainNamesArray, domainsArray);
    }

    private String mergeDomains(String domainNames, String domains) {
        Set<String> domainSet = new HashSet<>();
        domainSet.addAll(Arrays.asList(domainNames.split(",")));
        domainSet.addAll(Arrays.asList(domains.split(",")));
        return String.join(",", domainSet);
    }

    public void flowBillingHourPre() {
        log.info("Flow billing hour pre task start");
        // 获取当前时间
        DateTime currentHour = getCurrentHour();
        // 获取所有的域名
        List<CdnDomain> cdnDomains = cdnDomainService.queryAll();
        if (Assert.isEmpty(cdnDomains)) {
            log.info("Flow billing hour pre task end");
            return;
        }
        Map<Long, List<CdnDomain>> userIdMaps = groupByUserId(cdnDomains);
        for (Map.Entry<Long, List<CdnDomain>> userIdEntry : userIdMaps.entrySet()) {
            // 先查询 flowBillingLogic 是否存在
            Long userId = userIdEntry.getKey();
            List<CdnDomain> userCdnDomains = userIdEntry.getValue();
            String domainNames = toDomainNames(userCdnDomains);
            String hourStr = currentHour.toString("yyyy-MM-dd HH:mm:ss");
            // userId time promise 查询
            Query query = Query.query(Criteria.where("userId").is(userId).and("time").is(hourStr).and("promise").is("pending"));
            FlowBillingLogic flowBillingLogic = mongoTemplate.findOne(query, FlowBillingLogic.class);
            if (Assert.isEmpty(flowBillingLogic)) {
                flowBillingLogic = new FlowBillingLogic();
                flowBillingLogic.setUserId(userId);
                flowBillingLogic.setDomains(domainNames);
                flowBillingLogic.setSummary(0L);
                flowBillingLogic.setTime(hourStr);
                mongoTemplate.save(flowBillingLogic);
            }
            if (!domainsIsEqual(flowBillingLogic.getDomains(), domainNames)) {
                flowBillingLogic.setDomains(mergeDomains(flowBillingLogic.getDomains(), domainNames));
                flowBillingLogic.setUpdateTime(KuocaiDateUtil.getCurrentTime());
                mongoTemplate.save(flowBillingLogic);
            }
        }
        log.info("Flow billing hour pre task end");
    }

    private List<CdnDomain> queryByDomainNames(String domainNames) {
        return cdnDomainService.queryByDomainNames(domainNames);
    }

    // 前两个小时的补单
    public void flowBillingLastTwoHour() {
        // 上两个小时 补单 从 mongo 中查询一个 time 不等于 最近两个小时的数据
        DateTime currentHour = getCurrentHour();
        DateTime oneTime = DateUtil.offsetHour(currentHour, -1);
        DateTime twoTime = DateUtil.offsetHour(getCurrentHour(), -2);
        Criteria criteria = new Criteria().orOperator(
                Criteria.where("time").ne(oneTime.toString("yyyy-MM-dd HH:mm:ss")),
                Criteria.where("time").ne(twoTime.toString("yyyy-MM-dd HH:mm:ss"))
        );
        Query query = Query.query(Criteria.where("promise").is("pending").andOperator(criteria));
        // 只查询一条
        FlowBillingLogic flowBillingLogic = mongoTemplate.findOne(query, FlowBillingLogic.class);
        if (Assert.isEmpty(flowBillingLogic)) {
            return;
        }
        // 域名
        String domainNames = flowBillingLogic.getDomains();
        log.info("开始补单，用户ID：{}，域名：{}，时间：{}", flowBillingLogic.getUserId(), domainNames, flowBillingLogic.getTime());
        DateTime start = DateUtil.parse(flowBillingLogic.getTime(), "yyyy-MM-dd HH:mm:ss");
        DateTime end = DateUtil.offsetHour(start, 1);
        if (isBilling(flowBillingLogic.getUserId(), start, end)) {
            log.info("用户[{}]在[{}]已经扣费", flowBillingLogic.getUserId(), start);
            flowBillingLogic.setPromise("resolved");
            flowBillingLogic.setUpdateTime(KuocaiDateUtil.getCurrentTime());
            mongoTemplate.save(flowBillingLogic);
        } else {
            log.info("用户[{}]在[{}]未扣费", flowBillingLogic.getUserId(), start);
            List<CdnDomain> cdnDomains = queryByDomainNames(domainNames);
            if (cdnDomains.isEmpty()) {
                log.error("用户[{}]在[{}]未找到域名[{}]", flowBillingLogic.getUserId(), start, domainNames);
                flowBillingLogic.setPromise("cancel");
                flowBillingLogic.setUpdateTime(KuocaiDateUtil.getCurrentTime());
                mongoTemplate.save(flowBillingLogic);
            } else {
                userFlowBilling(flowBillingLogic.getUserId(), cdnDomains, start, end);
            }
        }
    }

    private boolean isBilling(Long userId, DateTime start, DateTime end) {
        QueryWrapper<PurchasedFlowDetail> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).between("create_time", start, end);
        return !purchasedFlowDetailService.queryByWrapper(wrapper).isEmpty();
    }

    public void flowBillingLastHour() {
        // 上一个小时 补单
        DateTime lastHour = DateUtil.offsetHour(getCurrentHour(), -1);
        flowBillingHour(lastHour);
    }

    public void retryPendingBilling() {
        DateTime latestRetryHour = DateUtil.offsetHour(getCurrentHour(), -1);
        DateTime earliestRetryHour = DateUtil.offsetHour(getCurrentHour(), -48);
        Query query = Query.query(new Criteria().orOperator(
                        Criteria.where("promise").is("pending"),
                        Criteria.where("promise").is("rejected"),
                        Criteria.where("promise").is("resolved")
                ))
                .addCriteria(Criteria.where("time")
                        .gte(earliestRetryHour.toString("yyyy-MM-dd HH:mm:ss"))
                        .lt(latestRetryHour.toString("yyyy-MM-dd HH:mm:ss")))
                .with(Sort.by(Sort.Direction.ASC, "time"))
                .limit(100);
        List<FlowBillingLogic> pendingLogics = mongoTemplate.find(query, FlowBillingLogic.class);
        for (FlowBillingLogic billingLogic : pendingLogics) {
            List<CdnDomain> cdnDomains = queryByDomainNames(billingLogic.getDomains());
            if (cdnDomains.isEmpty()) {
                billingLogic.setPromise("cancel");
                billingLogic.setUpdateTime(KuocaiDateUtil.getCurrentTime());
                mongoTemplate.save(billingLogic);
                continue;
            }
            DateTime start = DateUtil.parse(billingLogic.getTime(), "yyyy-MM-dd HH:mm:ss");
            userFlowBilling(billingLogic.getUserId(), cdnDomains, start, DateUtil.offsetHour(start, 1));
        }
    }

    public void flowBillingHour() {
        flowBillingHour(getCurrentHour());
    }

    public void flowBillingHour(DateTime currentHour) {
        log.info("Flow billing hour task start");
        DateTime lastHour = DateUtil.offsetHour(currentHour, -1);
        // 获取所有的域名
        List<CdnDomain> cdnDomains = cdnDomainService.queryAll();
        if (Assert.isEmpty(cdnDomains)) {
            log.info("Flow billing hour task end");
            return;
        }
        Map<Long, List<CdnDomain>> userIdMaps = groupByUserId(cdnDomains);
        ExecutorService executor = Executors.newFixedThreadPool(10);
        ExecutorCompletionService<Void> executorCompletionService = new ExecutorCompletionService<>(executor);
        for (Map.Entry<Long, List<CdnDomain>> userIdEntry : userIdMaps.entrySet()) {
            // userFlowBilling(userIdEntry, lastHour, currentHour);
            executorCompletionService.submit(() -> userFlowBilling(userIdEntry, lastHour, currentHour), null);
        }
        // 关闭线程池的提交功能
        executor.shutdown();
        // 等待线程池中的所有线程执行完毕
        try {
            boolean b = executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            if (b) {
                log.info("线程池中的所有线程执行完毕");
            }
        } catch (InterruptedException e) {
            log.error("线程池中的所有线程执行完成失败", e);
        }
        log.info("Flow billing hour task end");
    }

    private boolean openSourceMeteredBillingEnabled() {
        return true;
    }

    private void flowStatementByMeteredBalance(List<CdnDomain> userCdnDomains, Long totalUse, Long userId, DateTime start, DateTime end) throws BusinessException {
        if (Assert.isEmpty(totalUse) || totalUse <= 0) {
            return;
        }
        if (cdnDomainService.isCanStopDomain(userId)) {
            log.info("用户[{}]存在未结订单或余额不足，所持有的域名将会停用", userId);
            userCdnDomains.forEach(item -> {
                try {
                    ICdnPlatformService iCdnPlatformService = CdnPlatformFactory.getCdnPlatform(item.getRoute());
                    iCdnPlatformService.disable(item);
                    item.setDomainStatus(DomainStatus.CONFIGURING);
                    item.setUpdateTime(new Date());
                    cdnDomainService.save(item);
                } catch (Exception e) {
                    log.error("停用用户域名失败，用户ID：{}，域名：{}", userId, item.getDomainName());
                }
            });
        }
        SysUser sysUser = sysUserService.queryById(userId);
        if (Assert.isEmpty(sysUser)) {
            throw new BusinessException("当前用户不存在：[{}]", userId);
        }
        BigDecimal usedFlowGb = KuocaiBaseUtil.flowUnitConversion(totalUse, "GB");
        BigDecimal flowPrice = sysUser.getFlowPrice();
        BigDecimal amount = flowPrice.multiply(usedFlowGb).setScale(2, RoundingMode.UP);
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            amount = amount.abs();
        }
        TransactionOrder transactionOrder = TransactionOrder.builder()
                .payType(TransactionOrderPayType.BALANCE_PAY)
                .orderType(TransactionOrderType.FLOW_DEDUCTION)
                .orderNum(PayUtils.getOutTradeNo())
                .userId(userId)
                .userName(sysUser.getUserName())
                .createTime(new Date())
                .amount(amount)
                .status(TransactionOrderStatus.WAIT_BUYER_PAY)
                .detail("当前使用流量：" + usedFlowGb + "GB，流量费用: ¥" + amount)
                .title("[" + start + "->" + DateUtil.format(end, "HH:mm:ss") + "] 流量消费结账")
                .payUrl("")
                .build();
        transactionOrder = transactionOrderService.save(transactionOrder);
        log.info("用户[{}]已生成按量流量订单，订单ID：[{}]", userId, transactionOrder.getId());
        if (sysUser.openAutoBalance()) {
            SysUserAccount sysUserAccount = sysUserAccountService.queryByUserId(userId);
            if (Assert.isEmpty(sysUserAccount)) {
                throw new BusinessException("当前用户没有余额账户：[{}]", userId);
            }
            if (sysUserAccount.getAccountBalance().compareTo(amount) >= 0) {
                transactionOrderService.useBalance2PayTransactionOrder(sysUserAccount, transactionOrder);
            }
        }
    }

    /**
     * 补执行指定时间段的计费任务
     * @param targetDate 目标日期，格式：yyyy-MM-dd
     * @param startHour 开始小时（包含）
     * @param endHour 结束小时（不包含）
     */
    public void flowBillingForSpecificHours(String targetDate, int startHour, int endHour) {
        log.info("开始补执行计费任务 - 日期: {}, 时间段: {}:00 - {}:00", targetDate, startHour, endHour);

        try {
            // 解析目标日期
            DateTime baseDate = DateUtil.parse(targetDate, "yyyy-MM-dd");

            // 循环执行每个小时的计费
            for (int hour = startHour; hour < endHour; hour++) {
                DateTime startTime = DateUtil.offsetHour(baseDate, hour);
                DateTime endTime = DateUtil.offsetHour(startTime, 1);

                log.info("补执行计费任务 - 时间段: {} -> {}", startTime.toString("yyyy-MM-dd HH:mm:ss"), endTime.toString("yyyy-MM-dd HH:mm:ss"));

                // 调用现有的计费方法
                flowBillingHour(endTime); // 传入结束时间，因为flowBillingHour会自动减1小时作为开始时间

                log.info("完成计费任务 - 时间段: {} -> {}", startTime.toString("yyyy-MM-dd HH:mm:ss"), endTime.toString("yyyy-MM-dd HH:mm:ss"));
            }

        } catch (Exception e) {
            log.error("补执行计费任务失败 - 日期: {}, 时间段: {}:00 - {}:00, 错误: {}", targetDate, startHour, endHour, e.getMessage(), e);
            throw e;
        }

        log.info("补执行计费任务完成 - 日期: {}, 时间段: {}:00 - {}:00", targetDate, startHour, endHour);
    }

    /**
     * 补执行21-24点的计费任务
     * @param targetDate 目标日期，格式：yyyy-MM-dd
     */
    public void flowBillingFor21To24Hours(String targetDate) {
        flowBillingForSpecificHours(targetDate, 21, 24);
    }

    /**
     * 补执行22-24点的计费任务
     * @param targetDate 目标日期，格式：yyyy-MM-dd
     */
    public void flowBillingFor22To24Hours(String targetDate) {
        flowBillingForSpecificHours(targetDate, 22, 24);
    }

    /**
     * 补执行8月8日21点到8月17日21点的计费任务（一次性扣款）
     * 针对在这个时间段内purchase_flow_detail表中一条数据都没有的用户
     * 采用粗暴方式：一次性获取整个时间段的总流量进行扣款
     */
    public void flowBillingForAug8To18() {
        String startDateStr = "2025-08-08";
        String endDateStr = "2025-08-17";
        int startHour = 21; // 8月8日21点开始
        int endHour = 21;   // 8月17日21点结束

        log.info("=== 开始补执行8月8日21点到8月17日21点的计费任务（一次性扣款模式） ===");

        try {
            // 1. 定义精确的时间范围：8月8日21点到8月17日21点
            DateTime periodStart = DateUtil.parse(startDateStr + " " + String.format("%02d:00:00", startHour), "yyyy-MM-dd HH:mm:ss");
            DateTime periodEnd = DateUtil.parse(endDateStr + " " + String.format("%02d:59:59", endHour), "yyyy-MM-dd HH:mm:ss");

            log.info("一次性扣款时间段: {} -> {}", periodStart.toString("yyyy-MM-dd HH:mm:ss"), periodEnd.toString("yyyy-MM-dd HH:mm:ss"));

            // 2. 获取所有有域名的用户
            List<CdnDomain> allDomains = cdnDomainService.queryAll();
            if (allDomains.isEmpty()) {
                log.warn("系统中没有任何域名，无法执行计费补单");
                return;
            }

            Map<Long, List<CdnDomain>> allUserDomains = groupByUserId(allDomains);
            log.info("系统中有域名的用户总数: {}", allUserDomains.size());

            // 3. 查询在此时间段内有消费记录的用户ID
            Set<Long> usersWithRecords = getUsersWithFlowDetailInPeriod(periodStart.toJdkDate(), periodEnd.toJdkDate());
            log.info("在时间段内有消费记录的用户数量: {}", usersWithRecords.size());
            log.info("有消费记录的用户ID列表: {}", usersWithRecords);

            // 4. 筛选出没有消费记录但有域名的用户
            List<Long> usersWithoutRecords = allUserDomains.keySet().stream()
                    .filter(userId -> userId != null) // 确保用户ID不为null
                    .filter(userId -> !usersWithRecords.contains(userId)) // 没有消费记录
                    .filter(userId -> allUserDomains.get(userId) != null && !allUserDomains.get(userId).isEmpty()) // 确保有域名
                    .collect(Collectors.toList());

            log.info("在时间段内没有消费记录但有域名的用户数量: {}", usersWithoutRecords.size());
            log.info("需要补执行计费的用户ID列表: {}", usersWithoutRecords);

            // 5. 验证筛选结果
            for (Long userId : usersWithoutRecords) {
                List<CdnDomain> userDomains = allUserDomains.get(userId);
                log.info("用户[{}]拥有域名数量: {}, 域名列表: {}", userId, userDomains.size(),
                        userDomains.stream().map(CdnDomain::getDomainName).collect(Collectors.joining(", ")));
            }

            if (usersWithoutRecords.isEmpty()) {
                log.info("没有需要补执行计费的用户");
                return;
            }

            // 2. 对这些用户进行一次性整体计费（粗暴模式）
            log.info("开始对 {} 个用户进行一次性整体计费", usersWithoutRecords.size());

            int successCount = 0;
            int failureCount = 0;
            long totalFlowConsumed = 0L; // 总流量消耗统计
            int usersWithFlow = 0; // 有流量消耗的用户数
            int usersWithoutFlow = 0; // 无流量消耗的用户数

            for (Long userId : usersWithoutRecords) {
                List<CdnDomain> userDomains = allUserDomains.get(userId);
                if (userDomains != null && !userDomains.isEmpty()) {
                    try {
                        // 获取用户流量消耗
                        JSONObject resource = getResource(userId, userDomains, periodStart, periodEnd);
                        Long userFlowConsumed = getUsedFlow(resource);
                        totalFlowConsumed += userFlowConsumed;

                        // 统计有无流量消耗的用户
                        if (userFlowConsumed > 0) {
                            usersWithFlow++;
                        } else {
                            usersWithoutFlow++;
                        }

                        // 一次性获取整个时间段的流量并计费
                        userFlowBilling(userId, userDomains, periodStart, periodEnd);
                        successCount++;
                        log.info("用户[{}]一次性计费完成，消耗流量: {} ({}), 时间段[{} -> {}]", userId,
                                userFlowConsumed, KuocaiBaseUtil.autoReducedFlowUnit(userFlowConsumed),
                                periodStart.toString("yyyy-MM-dd HH:mm:ss"),
                                periodEnd.toString("yyyy-MM-dd HH:mm:ss"));
                    } catch (Exception e) {
                        failureCount++;
                        log.error("用户[{}]一次性计费失败，时间段[{} -> {}]，错误: {}", userId,
                                periodStart.toString("yyyy-MM-dd HH:mm:ss"),
                                periodEnd.toString("yyyy-MM-dd HH:mm:ss"), e.getMessage());
                    }
                }
            }

            log.info("=== 8月8日21点到8月17日21点的一次性计费任务完成 ===");
            log.info("=== 📊 详细统计结果 ===");
            log.info("处理结果：成功 {} 个用户，失败 {} 个用户", successCount, failureCount);
            log.info("流量统计：有流量消耗 {} 个用户，无流量消耗 {} 个用户", usersWithFlow, usersWithoutFlow);
            log.info("总流量消耗: {} bytes ({})", totalFlowConsumed, KuocaiBaseUtil.autoReducedFlowUnit(totalFlowConsumed));
            if (usersWithFlow > 0) {
                long avgFlowPerUser = totalFlowConsumed / usersWithFlow;
                log.info("平均每个有流量用户消耗: {} bytes ({})", avgFlowPerUser, KuocaiBaseUtil.autoReducedFlowUnit(avgFlowPerUser));
            }
            log.info("=== 📊 统计完成 ===");

        } catch (Exception e) {
            log.error("8月8日13点到8月18日20点的计费任务补执行失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 查询在指定时间段内有流量消费记录的用户ID集合
     */
    private Set<Long> getUsersWithFlowDetailInPeriod(Date startDate, Date endDate) {
        try {
            Set<Long> userIds = purchasedFlowDetailService.getUsersWithRecordsInPeriod(startDate, endDate);
            log.debug("查询到在时间段[{} -> {}]内有消费记录的用户: {}", startDate, endDate, userIds);
            return userIds;
        } catch (Exception e) {
            log.error("查询时间段内有消费记录的用户失败: {}", e.getMessage(), e);
            return new HashSet<>();
        }
    }

    /**
     * 模拟扣费 - 获取详细数据但不实际扣费
     * 用于验证数据正确性：用户+域名+流量包+流量包扣费+余额扣费
     */
    public void simulateFlowBillingForAug8To18() {
        String startDateStr = "2025-08-08";
        String endDateStr = "2025-08-17";
        int startHour = 21; // 8月8日21点开始
        int endHour = 21;   // 8月17日21点结束

        log.info("=== 开始模拟扣费 - 8月8日21点到8月17日21点 ===");

        try {
            // 1. 定义精确的时间范围
            DateTime periodStart = DateUtil.parse(startDateStr + " " + String.format("%02d:00:00", startHour), "yyyy-MM-dd HH:mm:ss");
            DateTime periodEnd = DateUtil.parse(endDateStr + " " + String.format("%02d:59:59", endHour), "yyyy-MM-dd HH:mm:ss");

            log.info("模拟扣费时间段: {} -> {}", periodStart.toString("yyyy-MM-dd HH:mm:ss"), periodEnd.toString("yyyy-MM-dd HH:mm:ss"));

            // 2. 获取所有有域名的用户
            List<CdnDomain> allDomains = cdnDomainService.queryAll();
            if (allDomains.isEmpty()) {
                log.warn("系统中没有任何域名，无法执行模拟扣费");
                return;
            }

            Map<Long, List<CdnDomain>> allUserDomains = groupByUserId(allDomains);
            log.info("系统中有域名的用户总数: {}", allUserDomains.size());

            // 3. 查询在此时间段内有消费记录的用户ID
            Set<Long> usersWithRecords = getUsersWithFlowDetailInPeriod(periodStart.toJdkDate(), periodEnd.toJdkDate());
            log.info("在时间段内有消费记录的用户数量: {}", usersWithRecords.size());

            // 4. 筛选出没有消费记录但有域名的用户
            List<Long> usersWithoutRecords = allUserDomains.keySet().stream()
                    .filter(userId -> userId != null)
                    .filter(userId -> !usersWithRecords.contains(userId))
                    .filter(userId -> allUserDomains.get(userId) != null && !allUserDomains.get(userId).isEmpty())
                    .collect(Collectors.toList());

            log.info("需要模拟扣费的用户数量: {}", usersWithoutRecords.size());

            if (usersWithoutRecords.isEmpty()) {
                log.info("没有需要模拟扣费的用户");
                return;
            }

            // 5. 对每个用户进行模拟扣费
            for (Long userId : usersWithoutRecords) {
                List<CdnDomain> userDomains = allUserDomains.get(userId);
                if (userDomains != null && !userDomains.isEmpty()) {
                    try {
                        simulateUserFlowBilling(userId, userDomains, periodStart, periodEnd);
                    } catch (Exception e) {
                        log.error("用户[{}]模拟扣费失败: {}", userId, e.getMessage());
                    }
                }
            }

            log.info("=== 模拟扣费完成 ===");

        } catch (Exception e) {
            log.error("模拟扣费失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 验证用户筛选逻辑 - 仅用于测试和调试
     */
    public void validateUserFilteringLogic() {
        String startDateStr = "2025-08-08";
        String endDateStr = "2025-08-18";
        int startHour = 13;
        int endHour = 20;

        log.info("=== 开始验证用户筛选逻辑 ===");

        // 1. 定义时间范围
        DateTime periodStart = DateUtil.parse(startDateStr + " " + String.format("%02d:00:00", startHour), "yyyy-MM-dd HH:mm:ss");
        DateTime periodEnd = DateUtil.parse(endDateStr + " " + String.format("%02d:59:59", endHour), "yyyy-MM-dd HH:mm:ss");

        log.info("查询时间段: {} -> {}", periodStart.toString("yyyy-MM-dd HH:mm:ss"), periodEnd.toString("yyyy-MM-dd HH:mm:ss"));

        // 2. 获取所有用户域名
        List<CdnDomain> allDomains = cdnDomainService.queryAll();
        Map<Long, List<CdnDomain>> allUserDomains = groupByUserId(allDomains);

        log.info("系统中总域名数: {}", allDomains.size());
        log.info("系统中有域名的用户数: {}", allUserDomains.size());

        // 3. 查询有消费记录的用户
        Set<Long> usersWithRecords = getUsersWithFlowDetailInPeriod(periodStart.toJdkDate(), periodEnd.toJdkDate());

        log.info("在时间段内有消费记录的用户数: {}", usersWithRecords.size());
        log.info("有消费记录的用户ID: {}", usersWithRecords);

        // 4. 筛选没有消费记录的用户
        List<Long> usersWithoutRecords = allUserDomains.keySet().stream()
                .filter(userId -> userId != null)
                .filter(userId -> !usersWithRecords.contains(userId))
                .filter(userId -> allUserDomains.get(userId) != null && !allUserDomains.get(userId).isEmpty())
                .collect(Collectors.toList());

        log.info("没有消费记录但有域名的用户数: {}", usersWithoutRecords.size());
        log.info("没有消费记录的用户ID: {}", usersWithoutRecords);

        // 5. 详细验证每个用户
        log.info("=== 详细验证结果 ===");
        for (Long userId : usersWithoutRecords) {
            List<CdnDomain> userDomains = allUserDomains.get(userId);
            log.info("用户[{}]: 域名数量={}, 域名列表={}",
                    userId,
                    userDomains.size(),
                    userDomains.stream().map(CdnDomain::getDomainName).collect(Collectors.joining(", ")));
        }

        log.info("=== 用户筛选逻辑验证完成 ===");
    }

    /**
     * 模拟用户流量扣费 - 获取详细数据但不实际扣费
     */
    public void simulateUserFlowBilling(Long userId, List<CdnDomain> userCdnDomains, DateTime start, DateTime end) {
        log.info("========== 开始模拟用户[{}]流量扣费 ==========", userId);
        log.info("用户[{}] - 域名信息: {}", userId, userCdnDomains.stream()
                .map(domain -> String.format("%s(ID:%d,状态:%s,路由:%s)",
                        domain.getDomainName(), domain.getId(), domain.getDomainStatus(), domain.getRoute()))
                .collect(Collectors.joining(", ")));

        // 1. 获取流量数据
        JSONObject resource;
        try {
            log.info("用户[{}] - 开始获取流量数据，域名数量: {}", userId, userCdnDomains.size());
            resource = getResource(userId, userCdnDomains, start, end);
            if (resource == null) {
                log.error("用户[{}] - getResource返回null", userId);
                return;
            }
        } catch (BusinessException e) {
            log.error("用户[{}] - 获取流量数据失败: {}", userId, e.getMessage(), e);
            return;
        }

        // 2. 解析流量消耗
        Long usedFlow = getUsedFlow(resource);
        log.info("用户[{}] - 时间段[{} -> {}]消耗流量: {} bytes ({})", userId,
                start.toString("yyyy-MM-dd HH:mm:ss"), end.toString("yyyy-MM-dd HH:mm:ss"),
                usedFlow, KuocaiBaseUtil.autoReducedFlowUnit(usedFlow));

        if (usedFlow == 0L) {
            log.info("用户[{}] - 流量消耗为0，无需扣费", userId);
            return;
        }

        // 3. 获取用户信息
        SysUser sysUser = sysUserService.queryById(userId);
        log.info("用户[{}] - 用户信息: 用户名={}, 流量单价={}元/GB, 自动扣费={}", userId,
                sysUser.getUserName(), sysUser.getFlowPrice(), sysUser.openAutoBalance());

        // 4. 获取用户账户信息
        SysUserAccount sysUserAccount = sysUserAccountService.queryByUserId(userId);
        if (sysUserAccount != null) {
            log.info("用户[{}] - 账户余额: {}元", userId, sysUserAccount.getAccountBalance());
        } else {
            log.warn("用户[{}] - 未找到账户信息", userId);
        }

        // 5. 模拟流量包扣费逻辑
        simulateFlowStatement(userCdnDomains, usedFlow, userId, start, end, sysUser, sysUserAccount);

        log.info("========== 用户[{}]模拟扣费完成 ==========", userId);
    }

    /**
     * 模拟流量包扣费逻辑 - 详细展示扣费过程但不实际扣费
     */
    public void simulateFlowStatement(List<CdnDomain> userCdnDomains, Long totalUse, Long userId,
                                     DateTime start, DateTime end, SysUser sysUser, SysUserAccount sysUserAccount) {
        log.info("---------- 开始模拟用户[{}]流量包扣费逻辑 ----------", userId);

        // 1. 查询用户的已购买流量包
        List<PurchasedFlowVo> purchasedFlowVos = purchasedFlowService.queryUserOnUsedPurchasedFlow(userId, 0);
        log.info("用户[{}] - 查询到的流量包数量: {}", userId, purchasedFlowVos.size());

        if (purchasedFlowVos.isEmpty()) {
            log.info("用户[{}] - 没有可用的流量包", userId);
        } else {
            for (PurchasedFlowVo flowVo : purchasedFlowVos) {
                long remainingFlow = flowVo.getFlowPackageSize() - flowVo.getUsedFlow();
                log.info("用户[{}] - 流量包[ID:{}] 名称:{}, 总量:{}, 已用:{}, 剩余:{}, 过期时间:{}",
                        userId, flowVo.getId(), flowVo.getFlowPackageName(),
                        KuocaiBaseUtil.autoReducedFlowUnit(flowVo.getFlowPackageSize()),
                        KuocaiBaseUtil.autoReducedFlowUnit(flowVo.getUsedFlow()),
                        KuocaiBaseUtil.autoReducedFlowUnit(remainingFlow),
                        flowVo.getDeadline());
            }
        }

        // 2. 计算流量包剩余总量
        long residualFlow = purchasedFlowVos.stream().mapToLong(item -> item.getFlowPackageSize() - item.getUsedFlow()).sum();
        log.info("用户[{}] - 流量包剩余总量: {} ({})", userId, residualFlow, KuocaiBaseUtil.autoReducedFlowUnit(residualFlow));
        log.info("用户[{}] - 本次需要扣费流量: {} ({})", userId, totalUse, KuocaiBaseUtil.autoReducedFlowUnit(totalUse));

        // 3. 模拟流量包扣费分配
        if (!purchasedFlowVos.isEmpty()) {
            Map<String, Map<Long, Object>> eachFlowUseDetails = deductFlow(purchasedFlowVos, totalUse);
            Map<Long, Object> allUsePackages = eachFlowUseDetails.get("all");  // 全部用完的流量包
            Map<Long, Object> partialUsePackages = eachFlowUseDetails.get("noAll");  // 部分使用的流量包

            log.info("用户[{}] - 将会完全用尽的流量包数量: {}", userId, allUsePackages.size());
            for (Map.Entry<Long, Object> entry : allUsePackages.entrySet()) {
                PurchasedFlowVo flowVo = Convert.convert(PurchasedFlowVo.class, entry.getValue());
                long willUse = flowVo.getFlowPackageSize() - flowVo.getUsedFlow();
                log.info("用户[{}] - 流量包[ID:{}] {} 将被完全用尽，消耗: {}",
                        userId, flowVo.getId(), flowVo.getFlowPackageName(),
                        KuocaiBaseUtil.autoReducedFlowUnit(willUse));
            }

            log.info("用户[{}] - 将会部分使用的流量包数量: {}", userId, partialUsePackages.size());
            for (Map.Entry<Long, Object> entry : partialUsePackages.entrySet()) {
                Long packageId = entry.getKey();
                Long useAmount = Convert.toLong(entry.getValue());
                PurchasedFlow purchasedFlow = purchasedFlowService.queryById(packageId);
                log.info("用户[{}] - 流量包[ID:{}] {} 将被部分使用，消耗: {}",
                        userId, packageId, purchasedFlow.getFlowPackageName(),
                        KuocaiBaseUtil.autoReducedFlowUnit(useAmount));
            }
        }

        // 4. 计算是否需要余额扣费
        if (residualFlow < totalUse) {
            long needPayFlow = totalUse - residualFlow;
            log.info("用户[{}] - 流量包不足，需要额外扣费流量: {} ({})", userId, needPayFlow, KuocaiBaseUtil.autoReducedFlowUnit(needPayFlow));

            // 处理负数情况
            if (needPayFlow < 0) {
                log.warn("用户[{}] - 计算出的扣费流量为负数: {}，将取绝对值", userId, needPayFlow);
                needPayFlow = Math.abs(needPayFlow);
            }

            BigDecimal needPayFlowGB = KuocaiBaseUtil.flowUnitConversion(needPayFlow, "GB");
            BigDecimal flowPrice = sysUser.getFlowPrice();
            BigDecimal needPayMoney = flowPrice.multiply(needPayFlowGB).setScale(2, RoundingMode.UP);

            // 处理负数金额
            if (needPayMoney.compareTo(BigDecimal.ZERO) < 0) {
                log.warn("用户[{}] - 计算出的扣费金额为负数: {}，将取绝对值", userId, needPayMoney);
                needPayMoney = needPayMoney.abs();
            }

            log.info("用户[{}] - 需要创建订单: 流量{}GB × {}元/GB = {}元", userId, needPayFlowGB, flowPrice, needPayMoney);

            // 检查自动扣费和余额
            if (sysUser.openAutoBalance()) {
                if (sysUserAccount != null) {
                    BigDecimal accountBalance = sysUserAccount.getAccountBalance();
                    if (accountBalance.compareTo(needPayMoney) >= 0) {
                        log.info("用户[{}] - 余额充足，将自动扣费: 当前余额{}元 >= 需要{}元", userId, accountBalance, needPayMoney);
                    } else {
                        log.warn("用户[{}] - 余额不足，无法自动扣费: 当前余额{}元 < 需要{}元", userId, accountBalance, needPayMoney);
                    }
                } else {
                    log.warn("用户[{}] - 开启了自动扣费但没有账户信息", userId);
                }
            } else {
                log.info("用户[{}] - 未开启自动扣费，订单将保持待支付状态", userId);
            }
        } else {
            log.info("用户[{}] - 流量包充足，无需额外扣费", userId);
        }

        log.info("---------- 用户[{}]流量包扣费逻辑模拟完成 ----------", userId);
    }

    private void legacyUserFlowBilling(Long userId, List<CdnDomain> userCdnDomains, DateTime start, DateTime end) {
        log.info("用户[{}]流量扣费开始, 域名：[{}], 时间：[{}->{}]", userId, toDomainNames(userCdnDomains), start, end);
        JSONObject resource;
        try {
            resource = getResource(userId, userCdnDomains, start, end);
            log.debug("userFlowBilling - 获取到的resource: {}", resource);
        } catch (BusinessException e) {
            log.error("获取用户域名资源消耗失败", e);
            return;
        }
        // 格式化 start yyyy-MM-dd HH:mm:ss
        String startStr = start.toString("yyyy-MM-dd HH:mm:ss");
        Query query = Query.query(Criteria.where("userId").is(userId).and("time").is(startStr));
        FlowBillingLogic flowBillingLogic = mongoTemplate.findOne(query, FlowBillingLogic.class);
        // 获取用户消耗的流量
        Long usedFlow = getUsedFlow(resource);
        if (Assert.isEmpty(flowBillingLogic)) {
            flowBillingLogic = new FlowBillingLogic();
            flowBillingLogic.setUserId(userId);
            flowBillingLogic.setDomains(toDomainNames(userCdnDomains));
            flowBillingLogic.setTime(startStr);
        }
        Long logicSummary = flowBillingLogic.getSummary();
        log.info("用户[{}]流量计费 - 当前流量: {}, 已扣费流量: {}", userId, usedFlow, logicSummary);

        if (logicSummary > 0) {
            if (logicSummary >= usedFlow) {
                log.info("用户[{}]当前流量[{}]小于等于已扣费流量[{}]，无需重复扣费", userId, usedFlow, logicSummary);
                return;
            } else {
                Long newUsedFlow = usedFlow - logicSummary;
                if (newUsedFlow < 0) {
                    log.info("用户[{}]计算后的流量为负数: {} - {} = {}，设置为0", userId, usedFlow, logicSummary, newUsedFlow);
                    usedFlow = 0L;
                } else {
                    usedFlow = newUsedFlow;
                    log.info("用户[{}]扣除已计费流量后的新流量: {}", userId, usedFlow);
                }
            }
        } else {
            flowBillingLogic.setPromise("pending");
        }
        mongoTemplate.save(flowBillingLogic);
        flowBillingLogic.setSummary(usedFlow);
        // 如果用户消耗的流量为0则不记录
        if (!ObjectUtil.equal(usedFlow, 0L)) {
            try {
                flowStatement(userCdnDomains, usedFlow, userId, start, end);
            } catch (Exception e) {
                flowBillingLogic.setPromise("rejected");
                log.error("对用户扣款失败，具体错误信息:[{}]", e.getMessage());
            }
        }
        flowBillingLogic.setPromise("resolved");
        flowBillingLogic.setUpdateTime(KuocaiDateUtil.getCurrentTime());
        // 保存流量扣费记录
        mongoTemplate.save(flowBillingLogic);
        log.info("用户[{}]流量扣费结束, 域名：[{}], 时间：[{}->{}]", userId, toDomainNames(userCdnDomains), start, end);
    }

    public void userFlowBilling(Long userId, List<CdnDomain> userCdnDomains, DateTime start, DateTime end) {
        log.info("User[{}] flow billing started, domains: [{}], period: [{} -> {}]",
                userId, toDomainNames(userCdnDomains), start, end);
        JSONObject resource;
        try {
            resource = getResource(userId, userCdnDomains, start, end);
        } catch (BusinessException e) {
            log.error("Failed to query flow statistics for user[{}]", userId, e);
            return;
        }

        String startStr = start.toString("yyyy-MM-dd HH:mm:ss");
        Query query = Query.query(Criteria.where("userId").is(userId).and("time").is(startStr));
        FlowBillingLogic billingLogic = mongoTemplate.findOne(query, FlowBillingLogic.class);
        if (Assert.isEmpty(billingLogic)) {
            billingLogic = new FlowBillingLogic();
            billingLogic.setUserId(userId);
            billingLogic.setDomains(toDomainNames(userCdnDomains));
            billingLogic.setTime(startStr);
        }

        long observedFlow = extractUsedFlow(resource);
        long billedFlow = ObjectUtil.defaultIfNull(billingLogic.getSummary(), 0L);
        log.info("User[{}] flow billing - observed: {}, billed: {}", userId, observedFlow, billedFlow);

        if (observedFlow <= 0L) {
            billingLogic.setPromise("pending");
            billingLogic.setUpdateTime(KuocaiDateUtil.getCurrentTime());
            mongoTemplate.save(billingLogic);
            log.info("User[{}] statistics are not ready for [{} -> {}], keeping task pending",
                    userId, start, end);
            return;
        }

        if (billedFlow >= observedFlow) {
            billingLogic.setPromise("resolved");
            billingLogic.setUpdateTime(KuocaiDateUtil.getCurrentTime());
            mongoTemplate.save(billingLogic);
            log.info("User[{}] observed flow[{}] has already been billed[{}]",
                    userId, observedFlow, billedFlow);
            return;
        }

        long incrementalFlow = observedFlow - billedFlow;
        billingLogic.setPromise("pending");
        mongoTemplate.save(billingLogic);
        try {
            flowStatement(userCdnDomains, incrementalFlow, userId, start, end);
        } catch (Exception e) {
            billingLogic.setPromise("rejected");
            billingLogic.setUpdateTime(KuocaiDateUtil.getCurrentTime());
            mongoTemplate.save(billingLogic);
            log.error("User[{}] flow billing failed", userId, e);
            return;
        }

        billingLogic.setSummary(observedFlow);
        billingLogic.setPromise("resolved");
        billingLogic.setUpdateTime(KuocaiDateUtil.getCurrentTime());
        mongoTemplate.save(billingLogic);
        log.info("User[{}] flow billing finished, incremental flow: {}", userId, incrementalFlow);
    }

    public void userFlowBilling(Map.Entry<Long, List<CdnDomain>> userIdEntry, DateTime start, DateTime end) {
        userFlowBilling(userIdEntry.getKey(), userIdEntry.getValue(), start, end);
    }

    /**
     * 将用户的流量包扣费
     *
     * @param userCdnDomains 用户的域名
     * @param totalUse       总共使用的流量
     */
    @Transactional(rollbackFor = Exception.class)
    public void flowStatement(List<CdnDomain> userCdnDomains, Long totalUse, Long userId, DateTime start, DateTime end) throws BusinessException {
        if (openSourceMeteredBillingEnabled()) {
            flowStatementByMeteredBalance(userCdnDomains, totalUse, userId, start, end);
            return;
        }
        // 查询当前用户的已购买流量包
        List<PurchasedFlowVo> purchasedFlowVos = purchasedFlowService.queryUserOnUsedPurchasedFlow(userId, 0);
        // List<Long> doMainIds = userCdnDomains.stream().map(CdnDomain::getId).collect(Collectors.toList());
        // 用户流量包残留流量
        long residualFlow = purchasedFlowVos.stream().mapToLong(item -> item.getFlowPackageSize() - item.getUsedFlow()).sum();
        // 用户已购流量包ID
        List<Long> purchasedFlowIds = purchasedFlowVos.stream().map(PurchasedFlowVo::getId).collect(Collectors.toList());
        log.info("用户[{}]可使用的流量包购买记录：[{}]，总计剩余：[{}]", userId, purchasedFlowIds, KuocaiBaseUtil.autoReducedFlowUnit(residualFlow));
        // 判断用户是否有流量包
        Map<String, Map<Long, Object>> eachFlowUseDetails = deductFlow(purchasedFlowVos, totalUse);
        // 全部消耗的
        Map<Long, Object> all = eachFlowUseDetails.get("all");
        // 部分消耗
        Map<Long, Object> noAll = eachFlowUseDetails.get("noAll");
        List<Map<String, Object>> notifyPurchasedFlows = new ArrayList<>();
        // 处理已用尽的流量包
        for (Map.Entry<Long, Object> allEntry : all.entrySet()) {
            // 将流量包使用， 生成一个流量明细
            // Long key = allEntry.getKey();
            PurchasedFlowVo purchasedFlowVo = Convert.convert(PurchasedFlowVo.class, allEntry.getValue());
            // 使用流量生成流量明细
            long onceUsed = purchasedFlowVo.getFlowPackageSize() - purchasedFlowVo.getUsedFlow();
            log.info("用户[{}]已购流量包[{}]，本次消耗流量[{}]", userId, purchasedFlowVo.getId(), KuocaiBaseUtil.autoReducedFlowUnit(onceUsed));
            PurchasedFlowDetail flowDetail = PurchasedFlowDetail.builder().userId(userId).purchasedFlowId(purchasedFlowVo.getId()).consume(onceUsed).build();
            PurchasedFlowDetail detail = purchasedFlowDetailService.save(flowDetail);
            // 对返回结果进行处理， 将流量包使用量修改，状态修改
            PurchasedFlow hasUpdate = PurchasedFlow.builder()
                    .id(purchasedFlowVo.getId())
                    .usedFlow(purchasedFlowVo.getFlowPackageSize())
                    .status(PurchasedFlowConstants.ON_OVER)
                    .updateTime(new Date())
                    .build();
            log.info("用户[{}]已购流量包[{}]，已用尽，已生成使用记录[{}]", userId, purchasedFlowVo.getId(), detail.getId());
            HashMap<String, Object> notifyPurchasedFlow = new HashMap<>(16);
            notifyPurchasedFlow.put("name", purchasedFlowVo.getFlowPackageName());
            notifyPurchasedFlow.put("type", "over");
            notifyPurchasedFlows.add(notifyPurchasedFlow);
            purchasedFlowService.save(hasUpdate);
        }
        // 如果流量足够的时候会有一个使用一般的流量包
        for (Map.Entry<Long, Object> noAllEntry : noAll.entrySet()) {
            Long key = noAllEntry.getKey();
            Long usedSize = Convert.toLong(noAllEntry.getValue());
            PurchasedFlow purchasedFlow = purchasedFlowService.queryById(key);
            purchasedFlow.setUsedFlow(purchasedFlow.getUsedFlow() + usedSize);
            purchasedFlow.setUpdateTime(new Date());
            // calc
            HashMap<String, Object> notifyPurchasedFlow = new HashMap<>(16);
            notifyPurchasedFlow.put("name", purchasedFlow.getFlowPackageName());
            notifyPurchasedFlow.put("type", calcPercentage(purchasedFlow));
            notifyPurchasedFlows.add(notifyPurchasedFlow);
            purchasedFlowService.save(purchasedFlow);
            PurchasedFlowDetail flowDetail = PurchasedFlowDetail.builder().userId(userId).purchasedFlowId(purchasedFlow.getId()).consume(usedSize).build();
            PurchasedFlowDetail detail = purchasedFlowDetailService.save(flowDetail);
            log.info("用户[{}]已购流量包[{}]，本次消耗流量[{}]，已生成使用记录[{}]", userId, purchasedFlow.getId(), KuocaiBaseUtil.autoReducedFlowUnit(usedSize), detail.getId());
        }
        if (residualFlow < totalUse) {
            // 判断当前用户是否还有未结的账单,或者余额不够才停用
            if (cdnDomainService.isCanStopDomain(userId)) {
                log.info("用户[{}]还有未结的账单，或余额不足，所持有的域名将会停用", userId);
                // 调用华为云修改状态
                userCdnDomains.forEach(item -> {
                    try {
                        ICdnPlatformService iCdnPlatformService = CdnPlatformFactory.getCdnPlatform(item.getRoute());
                        iCdnPlatformService.disable(item);
                        item.setDomainStatus(DomainStatus.CONFIGURING);
                        item.setUpdateTime(new Date());
                        cdnDomainService.save(item);
                    } catch (Exception e) {
                        log.error("停用用户域名失败，用户ID：{}，域名：{}", userId, item.getDomainName());
                    }
                });
            }
            // 需要支付剩余流量
            long hasPayFlow = totalUse - residualFlow;
            BigDecimal hasPayFlowGB = flowBytesToBillingGB(hasPayFlow);
            SysUser sysUser = sysUserService.queryById(userId);
            // 元/GB
            BigDecimal flowPrice = sysUser.getFlowPrice();
            // 按字节精确折算，小额流量先结转，累计满 1 元再生成欠费账单。
            BigDecimal hasPayMoney = calculateFlowBillingAmount(flowPrice, hasPayFlow);
            if (hasPayMoney.compareTo(BigDecimal.ZERO) < 0) {
                log.warn("计算出的订单金额为负数({})，将取绝对值进行计费", hasPayMoney);
                hasPayMoney = hasPayMoney.abs();
            }

            log.info("订单支付金额: flowPrice:{}, hasPayFlowGB:{}, hasPayMoney:{}", flowPrice, formatBillingDecimal(hasPayFlowGB), formatBillingDecimal(hasPayMoney));
            carryOrCreateFlowBill(hasPayFlow, hasPayMoney, userId, start, end, sysUser);
        }
        // 如果当前用户还有其他流量包可用则无需通知
        if (purchasedFlowService.hasAdditionalFlow(userId)) {
            return;
        }
        for (Map<String, Object> notifyPurchasedFlow : notifyPurchasedFlows) {
            String name = MapUtil.getStr(notifyPurchasedFlow, "name", "name");
            String type = MapUtil.getStr(notifyPurchasedFlow, "type", "normal");
            switch (type) {
                case "over":
                    try {
                        smsAsync.notifyPacketGiveOut(userId, name);
                    } catch (Exception e) {
                        log.error("流量包已用尽短信或邮箱通知失败：用户id：{},流量包名称：{},异常信息：{}", userId, name, e.getMessage());
                    }
                    break;
                case "beover":
                    try {
                        smsAsync.notifyPacketWillGiveOut(userId, name);
                    } catch (Exception e) {
                        log.error("流量包即将用尽短信或邮箱通知失败：用户id：{},流量包名称：{},异常信息：{}", userId, name, e.getMessage());
                    }
                    break;
            }
        }
    }




    /**
     * 计算当前用户各个流量包扣流量情况
     *
     * @param packages 包
     * @param size     大小
     * @return {@code Map<String, Map<Long, Object>>}
     */
    private void carryOrCreateFlowBill(long paidFlow, BigDecimal currentAmount, Long userId, DateTime start, DateTime end, SysUser sysUser) throws BusinessException {
        if (paidFlow <= 0L || currentAmount == null || currentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        Query query = Query.query(Criteria.where("userId").is(userId));
        FlowBillingCarry carry = mongoTemplate.findOne(query, FlowBillingCarry.class);
        if (Assert.isEmpty(carry)) {
            carry = new FlowBillingCarry();
            carry.setUserId(userId);
            carry.setFirstStartTime(start.toString("yyyy-MM-dd HH:mm:ss"));
        }
        if (Assert.isEmpty(carry.getFirstStartTime())) {
            carry.setFirstStartTime(start.toString("yyyy-MM-dd HH:mm:ss"));
        }

        long totalFlow = ObjectUtil.defaultIfNull(carry.getPendingFlow(), 0L) + paidFlow;
        BigDecimal totalAmount = ObjectUtil.defaultIfNull(carry.getPendingAmount(), BigDecimal.ZERO).add(currentAmount);
        carry.setPendingFlow(totalFlow);
        carry.setPendingAmount(totalAmount);
        carry.setLastEndTime(end.toString("yyyy-MM-dd HH:mm:ss"));
        carry.setUpdateTime(KuocaiDateUtil.getCurrentTime());

        if (totalAmount.compareTo(FLOW_MIN_BILLING_AMOUNT) < 0) {
            mongoTemplate.save(carry);
            log.info("User[{}] flow fee carried, flow: {}, amount: {}", userId, totalFlow, totalAmount);
            return;
        }

        BigDecimal amount = totalAmount.setScale(FLOW_BILLING_AMOUNT_SCALE, RoundingMode.HALF_UP);
        if (amount.compareTo(FLOW_MIN_BILLING_AMOUNT) < 0) {
            mongoTemplate.save(carry);
            log.info("User[{}] flow fee carried after rounding, flow: {}, amount: {}", userId, totalFlow, totalAmount);
            return;
        }

        BigDecimal flowGB = flowBytesToBillingGB(totalFlow);
        TransactionOrder transactionOrder = TransactionOrder.builder()
                .payType(TransactionOrderPayType.BALANCE_PAY)
                .orderType(TransactionOrderType.FLOW_DEDUCTION)
                .orderNum(PayUtils.getOutTradeNo())
                .userId(userId)
                .userName(sysUser.getUserName())
                .createTime(new Date())
                .amount(amount)
                .status(TransactionOrderStatus.WAIT_BUYER_PAY)
                .detail("累计使用流量：" + formatBillingDecimal(flowGB) + "GB，流量费用: ¥" + formatBillingDecimal(amount))
                .title("[" + carry.getFirstStartTime() + "->" + DateUtil.format(end, "yyyy-MM-dd HH:mm:ss") + "] 流量消费累计结账")
                .payUrl("")
                .build();
        transactionOrder = transactionOrderService.save(transactionOrder);
        log.info("User[{}] flow deduction order created, title: {}, detail: {}, orderId: {}",
                userId, transactionOrder.getTitle(), transactionOrder.getDetail(), transactionOrder.getId());

        carry.setPendingFlow(0L);
        carry.setPendingAmount(BigDecimal.ZERO);
        carry.setFirstStartTime(null);
        carry.setLastEndTime(null);
        carry.setUpdateTime(KuocaiDateUtil.getCurrentTime());
        mongoTemplate.save(carry);

        if (sysUser.openAutoBalance()) {
            SysUserAccount sysUserAccount = sysUserAccountService.queryByUserId(userId);
            if (Assert.isEmpty(sysUserAccount)) {
                throw new BusinessException("当前用户没有余额账户：" + userId);
            }
            if (sysUserAccount.getAccountBalance().compareTo(amount) >= 0) {
                transactionOrderService.useBalance2PayTransactionOrder(sysUserAccount, transactionOrder);
            }
        }
    }

    public Map<String, Map<Long, Object>> deductFlow(List<PurchasedFlowVo> packages, long size) {
        // 按照过期时间进行排序
        packages.sort(Comparator.comparing(PurchasedFlowVo::getDeadline));
        Map<Long, Object> allUse = new HashMap<>();
        Map<Long, Object> noAllUse = new HashMap<>();
        for (PurchasedFlowVo purchasedFlow : packages) {
            // 判断流量是否充足
            long residualFlow = purchasedFlow.getFlowPackageSize() - purchasedFlow.getUsedFlow();
            if (residualFlow <= size) {
                size -= residualFlow;
                allUse.put(purchasedFlow.getId(), purchasedFlow);
            } else {
                // 最后一个肯定是没有全部用完的，所以size就是他用的量
                noAllUse.put(purchasedFlow.getId(), size);
                break;
            }
        }
        Map<String, Map<Long, Object>> result = new HashMap<>();
        result.put("all", allUse);
        result.put("noAll", noAllUse);
        return result;
    }

    public String calcPercentage(PurchasedFlow purchasedFlow) {
        double percentage = (double) purchasedFlow.getUsedFlow() / purchasedFlow.getFlowPackageSize() * 100;
        if (percentage >= 100) {
            return "over";
        } else if (percentage >= 95) {
            return "beover";
        } else {
            return "normal";
        }
    }
}
