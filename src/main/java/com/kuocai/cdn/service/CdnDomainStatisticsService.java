package com.kuocai.cdn.service;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kuocai.cdn.constant.KuoCaiConstants;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.entity.WorkOrder;
import com.kuocai.cdn.enumeration.domainmerage.CdnRoute;
import com.kuocai.cdn.enumeration.domainmerage.domain.QueryStatisticsEnum;
import com.kuocai.cdn.enumeration.domainmerage.route.CdnOperationRoute;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.exception.CdnHuaweiException;
import com.kuocai.cdn.service.domain.statistics.ICdnStatisticsPlatformService;
import com.kuocai.cdn.service.factory.CdnStatisticsPlatformFactory;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.JedisUtil;
import com.kuocai.cdn.vo.CertificateVo;
import com.kuocai.cdn.vo.statistics.HttpCodeStatusStatistics;
import com.kuocai.cdn.vo.statistics.ResourceStatistics;
import com.kuocai.cdn.vo.statistics.TopUri;
import com.kuocai.cdn.vo.statistics.VisitsStatistics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.kuocai.cdn.constant.StatisticsType.*;
import static com.kuocai.cdn.util.KuocaiBaseUtil.*;

/**
 * 加速域名统计服务
 *
 * @author XUEW
 * @since 2023-02-26 23:30:24
 */
@Slf4j
@Service
public class CdnDomainStatisticsService {

    @Autowired
    private CdnDomainService cdnDomainService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private WorkOrderService workOrderService;

    @Autowired
    private HttpsCertificateService httpsCertificateService;

    /**
     * 清理所有统计缓存（用于修复峰值计算问题后清理错误缓存）
     */
    public void clearAllStatisticsCache() {
        log.info("开始清理所有统计缓存...");
        try {
            // 清理所有以 "Statistics:" 开头的缓存键
            Set<String> keys = JedisUtil.keys("Statistics:*");
            if (keys != null && !keys.isEmpty()) {
                for (String key : keys) {
                    JedisUtil.delKey(key);
                }
                log.info("成功清理 {} 个统计缓存", keys.size());
            } else {
                log.info("没有找到需要清理的统计缓存");
            }
        } catch (Exception e) {
            log.error("清理统计缓存失败", e);
        }
    }

    /**
     * 刷新缓存
     */
    public void refreshStatisticsCache() {
        List<CdnDomain> cdnDomains = cdnDomainService.queryAll();
        String domainNames = cdnDomains.stream().map(CdnDomain::getDomainName).sorted().collect(Collectors.joining(","));
        DateTime start = getTodayStart();
        DateTime end = getTomorrowStart();
        List<SysUser> admins = sysUserService.queryAllAdmins();
        for (SysUser admin : admins) {
            String key1 = String.format("Statistics:%d:%s:%s:%s->%s", admin.getId(), ALL, domainNames.hashCode(), start.getTime(), end.getTime());
            JedisUtil.delKey(key1);
            try {
                mergeAllPlatForm(cdnDomains, start, end, ALL, admin.getId());
            } catch (Exception e) {
                log.error("刷新全部缓存失败，用户：{}，缓存key：{}", admin.getUserName(), key1);
            }
            String key2 = String.format("Statistics:%d:%s:%s:%s->%s", admin.getId(), RESOURCE, domainNames.hashCode(), start.getTime(), end.getTime());
            JedisUtil.delKey(key2);
            try {
                mergeAllPlatForm(cdnDomains, start, end, RESOURCE, admin.getId());
            } catch (Exception e) {
                log.error("刷新网络资源消耗缓存失败，用户：{}，缓存key：{}", admin.getUserName(), key2);
            }
        }
    }

    public DateTime getTodayStart() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String start = now.format(formatter) + " 00:00:00";
        return DateUtil.parse(start);
    }

    public DateTime getTomorrowStart() {
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String start = tomorrow.format(formatter) + " 00:00:00";
        return DateUtil.parse(start);
    }

    /**
     * 计算金山云时间点数量：按 5 分钟颗粒度，从 start 到 end（不含 end）的点数
     */
    private int calcTimePointsForKingsoft(DateTime start, DateTime end) {
        long minutes = DateUtil.between(start, end, DateUnit.MINUTE);
        if (minutes < 0) {
            return 1;
        }
        // 金山按闭区间计算时间点，24h/5min=288，再+1=289
        int points = (int) (minutes / 5) + 1;
        return Math.max(points, 1);
    }

    /**
     * 查询数量统计集合
     *
     * @param userId 用户ID，不传表示查询全部
     * @return 数量统计集合
     */
    public Map<String, Long> queryCountStatistics(Long userId) throws BusinessException {
        Map<String, Long> result = new HashMap<>();

        // 获取域名总数
        List<CdnDomain> cdnDomains = cdnDomainService.queryByUserId(userId);
        int domainCount = cdnDomains.size();

        // 查询证书总数
        long certificateCount = 0;
        JSONObject certificateInfo = new JSONObject();
        try {
            certificateInfo = httpsCertificateService.queryCertificateInfosByPage(new CertificateVo());
        } catch (Exception e) {
            log.error("查询HTTPS证书信息失败，将使用默认空值继续执行，用户ID：{}", userId, e);
            certificateInfo.put("https", new JSONArray());
        }

        if (Assert.notEmpty(cdnDomains)) {
            List<String> domainIds = cdnDomains.stream().map(CdnDomain::getDomainId).collect(Collectors.toList());
            JSONArray https = (JSONArray) certificateInfo.get("https");
            for (Object http : https) {
                JSONObject httpObject = (JSONObject) http;
                if (domainIds.contains(httpObject.get("domainId").toString())) {
                    certificateCount++;
                }
            }
        }
        // 获取用户总数
        long userCount = sysUserService.countByWrapper(new QueryWrapper<>());

        // 获取工单总数
        QueryWrapper<WorkOrder> workOrderQueryWrapper = new QueryWrapper<>();
        workOrderQueryWrapper.eq("user_id", userId);
        long orderCount = workOrderService.countByWrapper(workOrderQueryWrapper);

        result.put("domainCount", (long) domainCount);
        result.put("certificateCount", certificateCount);
        result.put("userCount", userCount);
        result.put("orderCount", orderCount);
        return result;
    }

    /**
     * 这个是查询统计的通用接口，请使用这个
     * @param cdnDomains
     * @param start
     * @param end
     * @param type
     * @param userId
     * @return
     * @throws BusinessException
     */
    public JSONObject mergeAllPlatForm(List<CdnDomain> cdnDomains, DateTime start, DateTime end, String type, Long userId) throws BusinessException {
        String domainNameAlls = cdnDomains.stream().map(CdnDomain::getDomainName).sorted().collect(Collectors.joining(","));
        String key = String.format("Statistics:v5:%d:%s:%s:%s->%s", userId, type, domainNameAlls.hashCode(), start.getTime(), end.getTime());
        DateTime now = DateUtil.date();
        if (!start.after(now) && end.after(now)) {
            key = key + ":rt:" + DateUtil.format(now, "yyyyMMddHHmm");
        }
        JSONObject cacheData = JedisUtil.getJson(key);
        if (Assert.notEmpty(cacheData)) {
            return cacheData;
        }
        // 设置默认路由，如果没有设置，则使用默认的
        for (CdnDomain cdnDomain : cdnDomains) {
            if (Assert.isEmpty(cdnDomain.getRoute())) {
                cdnDomain.setRoute(CdnOperationRoute.HUAWEI.getRoute());
            }
        }
        // 对不同路线的加速域名处理
        Map<String, List<CdnDomain>> cdnDomainGroup = cdnDomains.stream().collect(Collectors.groupingBy(CdnDomain::getRoute));
        JSONObject result = new JSONObject();
        // 网络资源消耗
        ResourceStatistics resourceResult = new ResourceStatistics();
        // 访问情况
        VisitsStatistics visitsResult = new VisitsStatistics();
        // Http状态码
        HttpCodeStatusStatistics httpCodeStatusResult = new HttpCodeStatusStatistics();

        for (Map.Entry<String, List<CdnDomain>> cdnDomainEntry : cdnDomainGroup.entrySet()) {
            String route = cdnDomainEntry.getKey();
            List<CdnDomain> cdnDomainListForRoute = cdnDomainEntry.getValue()
                    .stream()
                    // 平台级严谨过滤：必须有该平台的 route，且 domainId 非空
                    .filter(d -> Assert.notEmpty(d.getRoute()) && route.equals(d.getRoute()))
                    .filter(d -> Assert.notEmpty(d.getDomainId()))
                    .collect(Collectors.toList());
            if (Assert.isEmpty(cdnDomainListForRoute)) {
                continue;
            }

            Collection<List<CdnDomain>> processingGroups;

            if (CdnRoute.KINGSOFT.getCode().equals(route)) {
                // 如果是金山云，则按“CDN类型”进行二次分组
                processingGroups = cdnDomainListForRoute.stream()
                        .collect(Collectors.groupingBy(CdnDomain::getBusinessType))
                        .values();
            } else {
                // 如果是其他厂商，整个列表视为一个组
                processingGroups = Collections.singletonList(cdnDomainListForRoute);
            }

            for (List<CdnDomain> groupToQuery : processingGroups) {
                Integer queryMax = QueryStatisticsEnum.convert(route).getQueryMax();
                if (CdnRoute.KINGSOFT.getCode().equals(route)) {
                    int timePoints = calcTimePointsForKingsoft(start, end);
                    int maxByPoints = Math.max(1, 10000 / Math.max(1, timePoints));
                    queryMax = Math.min(queryMax, maxByPoints);
                }
                List<String> distinctCdnDomainList = groupToQuery.stream().map(CdnDomain::getDomainName).distinct().collect(Collectors.toList());
                List<List<String>> splitDistinctCdnDomainList = ListUtil.split(distinctCdnDomainList, queryMax);

                for (List<String> items : splitDistinctCdnDomainList) {
                    String domainNames = "";
                    if(route.equals(CdnRoute.QINIU.getCode())){
                        domainNames = items.stream().collect(Collectors.joining(";"));
                    } else {
                        domainNames = items.stream().collect(Collectors.joining(","));
                    }
                    try {
                        JSONObject jsonObject = queryStatisticsNoVirtual(domainNames, route, start, end, type, userId);
                        switch (type) {
                            case RESOURCE:
                                ResourceStatistics resourceStatistics = jsonObject.getObject(RESOURCE, ResourceStatistics.class);
                                resourceResult = mergeResourceData(resourceResult, resourceStatistics);
                                break;
                            case VISITS:
                                VisitsStatistics visitsStatistics = jsonObject.getObject(VISITS, VisitsStatistics.class);
                                visitsResult = mergeVisitsData(visitsResult, visitsStatistics);
                                break;
                            case HTTP_CODE_STATUS:
                                HttpCodeStatusStatistics httpCodeStatusStatistics = jsonObject.getObject(HTTP_CODE_STATUS, HttpCodeStatusStatistics.class);
                                httpCodeStatusResult = mergeHttpCodeStatusData(httpCodeStatusResult, httpCodeStatusStatistics);
                                break;
                            case ALL:
                                ResourceStatistics resourceStatistics1 = jsonObject.getObject(RESOURCE, ResourceStatistics.class);
                                resourceResult = mergeResourceData(resourceResult, resourceStatistics1);
                                VisitsStatistics visitsStatistics1 = jsonObject.getObject(VISITS, VisitsStatistics.class);
                                visitsResult = mergeVisitsData(visitsResult, visitsStatistics1);
                                HttpCodeStatusStatistics httpCodeStatusStatistics1 = jsonObject.getObject(HTTP_CODE_STATUS, HttpCodeStatusStatistics.class);
                                httpCodeStatusResult = mergeHttpCodeStatusData(httpCodeStatusResult, httpCodeStatusStatistics1);
                                break;
                            default:
                                break;
                        }
                    } catch (BusinessException e) {
                        String msg = e.getMessage();
                        if (msg != null && msg.contains("账号下无此域名")) {
                            log.warn("统计忽略无效域名批次：route={}, domains={}, msg={}", route, domainNames, msg);
                            continue;
                        }
                        throw e;
                    }
                }
            }
        }

        switch (type) {
            case RESOURCE:
                result.put(RESOURCE, resourceResult);
                break;
            case VISITS:
                result.put(VISITS, visitsResult);
                break;
            case HTTP_CODE_STATUS:
                result.put(HTTP_CODE_STATUS, httpCodeStatusResult);
                break;
            case ALL:
                result.put(RESOURCE, resourceResult);
                result.put(VISITS, visitsResult);
                result.put(HTTP_CODE_STATUS, httpCodeStatusResult);
                break;
            default:
                break;
        }
        result = calcVirtualData(userId, result, type);
        List<String> labels = getLabels(start, end);
        result.put("labels", labels);
        JedisUtil.setJson(key, result, KuoCaiConstants.smartCacheTime(start, end));
        log.info("Redis中设置缓存，key：{}", key);
        return result;
    }

    public JSONObject queryStatisticsNoVirtual(String domainName, String route, DateTime start, DateTime end, String type, Long userId) throws BusinessException {
        ICdnStatisticsPlatformService platformService = CdnStatisticsPlatformFactory.getCdnPlatform(route);
        JSONObject result = new JSONObject();
        switch (type) {
            case RESOURCE:
                try {
                    result.put(RESOURCE, platformService.queryResourceStatistics(domainName, start, end));
                } catch (CdnHuaweiException e) {
                    log.error("查询域名统计信息，域名：{}, 开始时间：{}，结束时间：{}，查询类型：{}, 用户ID：{}", domainName, start, end, type, userId);
                    throw new BusinessException(e.getMessage()).setCause(e).log();
                }
                break;
            case VISITS:
                try {
                    result.put(VISITS, platformService.queryVisitsStatistics(domainName, start, end));
                } catch (Exception e) {
                    log.error("查询域名统计信息，域名：{}, 开始时间：{}，结束时间：{}，查询类型：{}, 用户ID：{}", domainName, start, end, type, userId);
                    throw new BusinessException(e.getMessage()).setCause(e).log();
                }
                break;
            case HTTP_CODE_STATUS:
                try {
                    result.put(HTTP_CODE_STATUS, platformService.queryHttpCodeStatusStatistics(domainName, start, end));
                } catch (Exception e) {
                    log.error("查询域名统计信息，域名：{}, 开始时间：{}，结束时间：{}，查询类型：{}, 用户ID：{}", domainName, start, end, type, userId);
                    throw new BusinessException(e.getMessage()).setCause(e).log();
                }
                break;
            case ALL:
                try {
                    result.put(RESOURCE, platformService.queryResourceStatistics(domainName, start, end));
                    result.put(VISITS, platformService.queryVisitsStatistics(domainName, start, end));
                    result.put(HTTP_CODE_STATUS, platformService.queryHttpCodeStatusStatistics(domainName, start, end));
                } catch (Exception e) {
                    log.error("查询域名统计信息，域名：{}, 开始时间：{}，结束时间：{}，查询类型：{}, 用户ID：{}", domainName, start, end, type, userId);
                    throw new BusinessException(e.getMessage()).setCause(e).log();
                }
                break;
            default:
                break;
        }
        return result;
    }

    public List<String> getLabels(DateTime start, DateTime end) {
        long between = DateUtil.between(start, end, DateUnit.DAY);
        List<String> labels = new ArrayList<>();
        if (between > 1) {
            for (int i = 0; i < between; i++) {
                labels.add(DateUtil.format(start, "MM-dd"));
                start = DateUtil.offsetDay(start, 1);
            }
        } else {
            long betweenHour = DateUtil.between(start, end, DateUnit.HOUR);
            for (int i = 0; i < betweenHour; i++) {
                labels.add((i) + "-" + (i + 1));
            }
        }
        return labels;
    }

    public JSONObject calcVirtualData(Long userId, JSONObject result, String type) {
        BigDecimal virtualRate = BigDecimal.ONE;
        JSONObject resource = result.getJSONObject(RESOURCE);
        JSONObject visits = result.getJSONObject(VISITS);
        HttpCodeStatusStatistics httpCodeStatus = result.getObject(HTTP_CODE_STATUS, HttpCodeStatusStatistics.class);

        if (Assert.notEmpty(resource)) {
            result.put(RESOURCE, calcResourceData(resource, virtualRate));
        }
        if (Assert.notEmpty(visits)) {
            result.put(VISITS, calcVisitsData(visits, virtualRate));
        }
        if (Assert.notEmpty(httpCodeStatus)) {
            result.put(HTTP_CODE_STATUS, calcHttpCodeStatusData(httpCodeStatus, virtualRate));
        }

        // 此处的 switch 返回有误，应直接返回修改后的 result
        return result;
    }

    private JSONObject calcResourceData(JSONObject data, BigDecimal virtualRate) {
        JSONObject summary = data.getJSONObject("resource_summary");
        long bwSummary = summary.getLongValue("bw");
        long bsBwSummary = summary.getLongValue("bs_bw");
        long fluxSummary = summary.getLongValue("flux");
        long bsFluxSummary = summary.getLongValue("bs_flux");
        summary.put("bw", autoReducedBwUnit(multiplyVirtualRate(virtualRate, bwSummary).longValue()));
        summary.put("bs_bw", autoReducedBwUnit(multiplyVirtualRate(virtualRate, bsBwSummary).longValue()));
        long virtualFlux = multiplyVirtualRate(virtualRate, fluxSummary).longValue();
        long virtualBsFlux = multiplyVirtualRate(virtualRate, bsFluxSummary).longValue();
        long totalFlux = virtualFlux + virtualBsFlux;
        summary.put("access_flux_byte", virtualFlux);
        summary.put("bs_flux_byte", virtualBsFlux);
        summary.put("total_flux_byte", totalFlux);
        summary.put("flux_byte", virtualFlux);
        // 云厂商控制台按 SI（1000 进位）展示 GB/TB；原始字节字段保持不变，
        // 套餐扣量仍直接使用字节，避免展示换算影响客户已有套餐权益。
        summary.put("flux", autoReducedFlowUnitDecimal(virtualFlux));
        summary.put("bs_flux", autoReducedFlowUnitDecimal(virtualBsFlux));

        JSONObject detail = data.getJSONObject("resource_detail");
        List<Long> bwDetail = Assert.notEmpty(detail.getJSONArray("bw")) ? detail.getJSONArray("bw").toJavaList(Long.class) : new ArrayList<>();
        List<Long> bsBwDetail = Assert.notEmpty(detail.getJSONArray("bs_bw")) ? detail.getJSONArray("bs_bw").toJavaList(Long.class) : new ArrayList<>();
        List<Long> fluxDetail = Assert.notEmpty(detail.getJSONArray("flux")) ? detail.getJSONArray("flux").toJavaList(Long.class) : new ArrayList<>();
        List<Long> bsFluxDetail = Assert.notEmpty(detail.getJSONArray("bs_flux")) ? detail.getJSONArray("bs_flux").toJavaList(Long.class) : new ArrayList<>();

        List<BigDecimal> bwData = multiplyVirtualRateList(virtualRate, bwDetail);
        List<BigDecimal> bsBwData = multiplyVirtualRateList(virtualRate, bsBwDetail);
        String bwUnit = getSuitableBwUnit(maxSeriesValue(bwData, bsBwData));
        HashMap<String, Object> bwMap = new HashMap<>();
        bwMap.put("data", convertBwUnit(bwData, bwUnit));
        bwMap.put("unit", bwUnit);
        detail.put("bw", bwMap);
        HashMap<String, Object> bsBwMap = new HashMap<>();
        bsBwMap.put("data", convertBwUnit(bsBwData, bwUnit));
        bsBwMap.put("unit", bwUnit);
        detail.put("bs_bw", bsBwMap);

        List<BigDecimal> fluxData = multiplyVirtualRateList(virtualRate, fluxDetail);
        List<BigDecimal> bsFluxData = multiplyVirtualRateList(virtualRate, bsFluxDetail);
        String fluxUnit = getSuitableFlowUnitDecimal(maxSeriesValue(fluxData, bsFluxData));
        HashMap<String, Object> fluxMap = new HashMap<>();
        fluxMap.put("data", convertFlowUnitDecimal(fluxData, fluxUnit));
        fluxMap.put("unit", fluxUnit);
        detail.put("flux", fluxMap);
        HashMap<String, Object> bsFlowMap = new HashMap<>();
        bsFlowMap.put("data", convertFlowUnitDecimal(bsFluxData, fluxUnit));
        bsFlowMap.put("unit", fluxUnit);
        detail.put("bs_flux", bsFlowMap);

        data.put("resource_summary", summary);
        data.put("resource_detail", detail);
        return data;
    }

    private long maxSeriesValue(List<BigDecimal> base, List<BigDecimal> add) {
        BigDecimal max = BigDecimal.ZERO;
        if (Assert.notEmpty(base)) {
            BigDecimal baseMax = base.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            if (baseMax.compareTo(max) > 0) {
                max = baseMax;
            }
        }
        if (Assert.notEmpty(add)) {
            BigDecimal addMax = add.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            if (addMax.compareTo(max) > 0) {
                max = addMax;
            }
        }
        return max.longValue();
    }

    private JSONObject calcVisitsData(JSONObject data, BigDecimal virtualRate) {
        JSONObject summary = data.getJSONObject("visits_summary");
        long reqNumSummary = summary.getLongValue("req_num");
        long hitFluxSummary = summary.getLongValue("hit_flux");
        long hitNumSummary = summary.getLongValue("hit_num");
        summary.put("req_num", multiplyVirtualRate(virtualRate, reqNumSummary));
        summary.put("hit_flux", autoReducedFlowUnitDecimal(multiplyVirtualRate(virtualRate, hitFluxSummary).longValue()));
        summary.put("hit_num", multiplyVirtualRate(virtualRate, hitNumSummary));

        JSONObject detail = data.getJSONObject("visits_detail");
        List<Long> reqNumDetail = Assert.notEmpty(detail.getJSONArray("req_num")) ? detail.getJSONArray("req_num").toJavaList(Long.class) : new ArrayList<>();
        List<Long> hitFluxDetail = Assert.notEmpty(detail.getJSONArray("hit_flux")) ? detail.getJSONArray("hit_flux").toJavaList(Long.class) : new ArrayList<>();
        List<Long> hitNumDetail = Assert.notEmpty(detail.getJSONArray("hit_num")) ? detail.getJSONArray("hit_num").toJavaList(Long.class) : new ArrayList<>();
        detail.put("req_num", multiplyVirtualRateList(virtualRate, reqNumDetail));
        detail.put("hit_flux", convertFlowUnitDecimal(multiplyVirtualRateList(virtualRate, hitFluxDetail)));
        detail.put("hit_num", multiplyVirtualRateList(virtualRate, hitNumDetail));

        data.put("visits_summary", summary);
        data.put("visits_detail", detail);
        return data;
    }

    private JSONObject calcHttpCodeStatusData(HttpCodeStatusStatistics data, BigDecimal virtualRate) {
        List<Long> statusSummary = data.getStatus_summary();
        List<Long> bsStatusSummary = data.getBs_status_summary();
        List<BigDecimal> statusSummaryNum = multiplyVirtualRateList(virtualRate, statusSummary);
        List<BigDecimal> bsStatusSummaryNum = multiplyVirtualRateList(virtualRate, bsStatusSummary);

        List<Object[]> statusDetail = data.getStatus_detail();
        List<Object[]> bsStatusDetail = data.getBs_status_detail();
        List<List<BigDecimal>> newStatusDetail = statusDetail.stream().map(objects -> Arrays.stream(objects).map(n -> multiplyVirtualRate(virtualRate, n)).collect(Collectors.toList())).collect(Collectors.toList());
        List<List<BigDecimal>> newBsStatusDetail = bsStatusDetail.stream().map(objects -> Arrays.stream(objects).map(n -> multiplyVirtualRate(virtualRate, n)).collect(Collectors.toList())).collect(Collectors.toList());

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status_summary", statusSummaryNum);
        jsonObject.put("bs_status_summary", bsStatusSummaryNum);
        jsonObject.put("status_detail", newStatusDetail);
        jsonObject.put("bs_status_detail", newBsStatusDetail);
        return jsonObject;
    }

    private JSONObject calcTopURIData(JSONObject data, BigDecimal virtualRate) {
        JSONArray reqNumResult = data.getJSONArray("reqNumResult");
        JSONArray fluxResult = data.getJSONArray("fluxResult");
        for (int i = 0; i < reqNumResult.size(); i++) {
            JSONObject jsonObject = reqNumResult.getJSONObject(i);
            Object value = jsonObject.get("value");
            jsonObject.put("value", multiplyVirtualRate(virtualRate, value) + "次");
        }
        for (int i = 0; i < fluxResult.size(); i++) {
            JSONObject jsonObject = fluxResult.getJSONObject(i);
            Object value = jsonObject.get("value");
            jsonObject.put("value", autoReducedFlowUnitDecimal(multiplyVirtualRate(virtualRate, value).longValue()));
        }
        return data;
    }

    private List<BigDecimal> multiplyVirtualRateList(BigDecimal virtualRate, List<Long> list) {
        return list.stream().map(n -> virtualRate.multiply(new BigDecimal(String.valueOf(n))).setScale(0, RoundingMode.HALF_UP)).collect(Collectors.toList());
    }

    private BigDecimal multiplyVirtualRate(BigDecimal virtualRate, Object obj) {
        return virtualRate.multiply(new BigDecimal(String.valueOf(obj))).setScale(0, RoundingMode.HALF_UP);
    }

    private List<Long> mergeLongSeries(List<Long> base, List<Long> add) {
        List<Long> a = base == null ? new ArrayList<>() : base;
        List<Long> b = add == null ? new ArrayList<>() : add;
        int max = Math.max(a.size(), b.size());
        List<Long> out = new ArrayList<>(max);
        for (int i = 0; i < max; i++) {
            long av = i < a.size() ? a.get(i) : 0L;
            long bv = i < b.size() ? b.get(i) : 0L;
            out.add(av + bv);
        }
        return out;
    }

    /**
     * 合并峰值数据序列（取最大值）
     * 用于带宽等峰值类型的数据合并
     */
    private List<Long> mergePeakSeries(List<Long> base, List<Long> add) {
        List<Long> a = base == null ? new ArrayList<>() : base;
        List<Long> b = add == null ? new ArrayList<>() : add;
        int max = Math.max(a.size(), b.size());
        List<Long> out = new ArrayList<>(max);
        for (int i = 0; i < max; i++) {
            long av = i < a.size() ? a.get(i) : 0L;
            long bv = i < b.size() ? b.get(i) : 0L;
            // 【修复】峰值数据取最大值，不是相加
            out.add(Math.max(av, bv));
        }
        return out;
    }

    public ResourceStatistics mergeResourceData(ResourceStatistics result, ResourceStatistics hasAdd) {
        if (Assert.isEmpty(result.getResource_summary())) {
            result.setResource_summary(hasAdd.getResource_summary());
            log.debug("首次设置资源统计数据 - 带宽: {}, 回源带宽: {}, 流量: {}, 回源流量: {}",
                    hasAdd.getResource_summary().getBw(), hasAdd.getResource_summary().getBs_bw(),
                    hasAdd.getResource_summary().getFlux(), hasAdd.getResource_summary().getBs_flux());
        } else {
            long oldBw = result.getResource_summary().getBw();
            long oldBsBw = result.getResource_summary().getBs_bw();
            long newBw = hasAdd.getResource_summary().getBw();
            long newBsBw = hasAdd.getResource_summary().getBs_bw();

            // 【修复】带宽峰值应该取最大值，不是相加
            result.getResource_summary().setBw(Math.max(oldBw, newBw));
            result.getResource_summary().setBs_bw(Math.max(oldBsBw, newBsBw));
            // 流量是累计值，应该相加
            result.getResource_summary().setFlux(result.getResource_summary().getFlux() + hasAdd.getResource_summary().getFlux());
            result.getResource_summary().setBs_flux(result.getResource_summary().getBs_flux() + hasAdd.getResource_summary().getBs_flux());

            log.debug("合并资源统计数据 - 带宽: {} + {} = {} (取最大值), 回源带宽: {} + {} = {} (取最大值)",
                    oldBw, newBw, result.getResource_summary().getBw(),
                    oldBsBw, newBsBw, result.getResource_summary().getBs_bw());
        }
        if (Assert.isEmpty(result.getResource_detail())) {
            result.setResource_detail(hasAdd.getResource_detail());
        } else {
            // 【修复】带宽详细数据使用峰值合并（取最大值）
            result.getResource_detail().setBw(mergePeakSeries(result.getResource_detail().getBw(), hasAdd.getResource_detail().getBw()));
            result.getResource_detail().setBs_bw(mergePeakSeries(result.getResource_detail().getBs_bw(), hasAdd.getResource_detail().getBs_bw()));
            // 流量详细数据使用累计合并（相加）
            result.getResource_detail().setFlux(mergeLongSeries(result.getResource_detail().getFlux(), hasAdd.getResource_detail().getFlux()));
            result.getResource_detail().setBs_flux(mergeLongSeries(result.getResource_detail().getBs_flux(), hasAdd.getResource_detail().getBs_flux()));
        }
        return result;
    }

    public VisitsStatistics mergeVisitsData(VisitsStatistics result, VisitsStatistics hasAdd) {
        if (Assert.isEmpty(result.getVisits_summary())) {
            result.setVisits_summary(hasAdd.getVisits_summary());
        } else {
            result.getVisits_summary().setHit_flux(result.getVisits_summary().getHit_flux() + hasAdd.getVisits_summary().getHit_flux());
            result.getVisits_summary().setHit_num(result.getVisits_summary().getHit_num() + hasAdd.getVisits_summary().getHit_num());
            result.getVisits_summary().setReq_num(result.getVisits_summary().getReq_num() + hasAdd.getVisits_summary().getReq_num());
        }
        if (Assert.isEmpty(result.getVisits_detail())) {
            result.setVisits_detail(hasAdd.getVisits_detail());
        } else {
            result.getVisits_detail().setHit_flux(mergeLongSeries(result.getVisits_detail().getHit_flux(), hasAdd.getVisits_detail().getHit_flux()));
            result.getVisits_detail().setHit_num(mergeLongSeries(result.getVisits_detail().getHit_num(), hasAdd.getVisits_detail().getHit_num()));
            result.getVisits_detail().setReq_num(mergeLongSeries(result.getVisits_detail().getReq_num(), hasAdd.getVisits_detail().getReq_num()));
        }
        return result;
    }

    public HttpCodeStatusStatistics mergeHttpCodeStatusData(HttpCodeStatusStatistics result, HttpCodeStatusStatistics hasAdd) {
        if (Assert.isEmpty(result.getStatus_summary())) {
            result.setStatus_summary(hasAdd.getStatus_summary());
        } else {
            List<Long> statusSummary = new ArrayList<>();
            List<Long> resultList = result.getStatus_summary();
            List<Long> hasAddList = hasAdd.getStatus_summary();
            int maxSize = Math.max(resultList.size(), hasAddList.size());

            for (int i = 0; i < maxSize; i++) {
                long resultValue = i < resultList.size() ? resultList.get(i) : 0L;
                long hasAddValue = i < hasAddList.size() ? hasAddList.get(i) : 0L;
                statusSummary.add(resultValue + hasAddValue);
            }
            result.setStatus_summary(statusSummary);
        }

        if (Assert.isEmpty(result.getBs_status_summary())) {
            result.setBs_status_summary(hasAdd.getBs_status_summary());
        } else {
            List<Long> bsStatusSummary = new ArrayList<>();
            List<Long> resultList = result.getBs_status_summary();
            List<Long> hasAddList = hasAdd.getBs_status_summary();
            int maxSize = Math.max(resultList.size(), hasAddList.size());

            for (int i = 0; i < maxSize; i++) {
                long resultValue = i < resultList.size() ? resultList.get(i) : 0L;
                long hasAddValue = i < hasAddList.size() ? hasAddList.get(i) : 0L;
                bsStatusSummary.add(resultValue + hasAddValue);
            }
            result.setBs_status_summary(bsStatusSummary);
        }

        if (Assert.isEmpty(result.getStatus_detail())) {
            result.setStatus_detail(hasAdd.getStatus_detail());
        } else {
            List<Object[]> statusDetails = result.getStatus_detail();
            List<Object[]> hasAddStatusDetails = hasAdd.getStatus_detail();
            int maxSize = Math.max(statusDetails.size(), hasAddStatusDetails.size());

            for (int i = 0; i < maxSize; i++) {
                Object[] statusDetail = i < statusDetails.size() ? statusDetails.get(i) : new Object[0];
                Object[] hasAddStatusDetail = i < hasAddStatusDetails.size() ? hasAddStatusDetails.get(i) : new Object[0];

                int maxArraySize = Math.max(statusDetail.length, hasAddStatusDetail.length);
                Object[] mergedArray = new Object[maxArraySize];

                for (int j = 0; j < maxArraySize; j++) {
                    long resultValue = j < statusDetail.length ? Long.valueOf(statusDetail[j].toString()) : 0L;
                    long hasAddValue = j < hasAddStatusDetail.length ? Long.valueOf(hasAddStatusDetail[j].toString()) : 0L;
                    mergedArray[j] = resultValue + hasAddValue;
                }

                if (i < statusDetails.size()) {
                    statusDetails.set(i, mergedArray);
                } else {
                    statusDetails.add(mergedArray);
                }
            }
            result.setStatus_detail(statusDetails);
        }

        if (Assert.isEmpty(result.getBs_status_detail())) {
            result.setBs_status_detail(hasAdd.getBs_status_detail());
        } else {
            List<Object[]> bsStatusDetails = result.getBs_status_detail();
            List<Object[]> hasAddBsStatusDetails = hasAdd.getBs_status_detail();
            int maxSize = Math.max(bsStatusDetails.size(), hasAddBsStatusDetails.size());

            for (int i = 0; i < maxSize; i++) {
                Object[] bsStatusDetail = i < bsStatusDetails.size() ? bsStatusDetails.get(i) : new Object[0];
                Object[] hasAddBsStatusDetail = i < hasAddBsStatusDetails.size() ? hasAddBsStatusDetails.get(i) : new Object[0];

                int maxArraySize = Math.max(bsStatusDetail.length, hasAddBsStatusDetail.length);
                Object[] mergedArray = new Object[maxArraySize];

                for (int j = 0; j < maxArraySize; j++) {
                    long resultValue = j < bsStatusDetail.length ? Long.valueOf(bsStatusDetail[j].toString()) : 0L;
                    long hasAddValue = j < hasAddBsStatusDetail.length ? Long.valueOf(hasAddBsStatusDetail[j].toString()) : 0L;
                    mergedArray[j] = resultValue + hasAddValue;
                }

                if (i < bsStatusDetails.size()) {
                    bsStatusDetails.set(i, mergedArray);
                } else {
                    bsStatusDetails.add(mergedArray);
                }
            }
            result.setBs_status_detail(bsStatusDetails);
        }
        return result;
    }

    public TopUri mergeTopUriData(TopUri result, TopUri hasAdd) {
        if (Assert.isEmpty(result.getFluxResult())) {
            result.setFluxResult(hasAdd.getFluxResult());
        } else {
            Set<String> existingUrls = new HashSet<>();
            List<TopUri.DataNum> mergedList = new ArrayList<>(result.getFluxResult());

            for (TopUri.DataNum dataNum : result.getFluxResult()) {
                existingUrls.add(dataNum.getUrl());
            }
            for (TopUri.DataNum dataNum : hasAdd.getFluxResult()) {
                if (dataNum.getUrl() != null && !existingUrls.contains(dataNum.getUrl())) {
                    mergedList.add(dataNum);
                    existingUrls.add(dataNum.getUrl());
                } else if (dataNum.getUrl() != null && existingUrls.contains(dataNum.getUrl())) {
                    TopUri.DataNum existingDataNum = mergedList.stream()
                            .filter(dn -> dn.getUrl().equals(dataNum.getUrl()))
                            .findFirst()
                            .orElse(null);
                    existingDataNum.setValue(existingDataNum.getValue() + dataNum.getValue());
                }
            }
            List<TopUri.DataNum> sortedList = mergedList.stream()
                    .sorted(Comparator.comparingLong(TopUri.DataNum::getValue).reversed())
                    .limit(100)
                    .collect(Collectors.toList());

            result.setFluxResult(sortedList);
        }
        if (Assert.isEmpty(result.getReqNumResult())) {
            result.setReqNumResult(hasAdd.getReqNumResult());
        } else {
            Set<String> existingUrls = new HashSet<>();
            List<TopUri.DataNum> mergedList = new ArrayList<>(result.getReqNumResult());

            for (TopUri.DataNum dataNum : result.getReqNumResult()) {
                existingUrls.add(dataNum.getUrl());
            }
            for (TopUri.DataNum dataNum : hasAdd.getReqNumResult()) {
                if (dataNum.getUrl() != null && !existingUrls.contains(dataNum.getUrl())) {
                    mergedList.add(dataNum);
                    existingUrls.add(dataNum.getUrl());
                } else if (dataNum.getUrl() != null && existingUrls.contains(dataNum.getUrl())) {
                    TopUri.DataNum existingDataNum = mergedList.stream()
                            .filter(dn -> dn.getUrl().equals(dataNum.getUrl()))
                            .findFirst()
                            .orElse(null);
                    existingDataNum.setValue(existingDataNum.getValue() + dataNum.getValue());
                }
            }
            List<TopUri.DataNum> sortedList = mergedList.stream()
                    .sorted(Comparator.comparingLong(TopUri.DataNum::getValue).reversed())
                    .limit(100)
                    .collect(Collectors.toList());

            result.setReqNumResult(sortedList);
        }
        return result;
    }
}
