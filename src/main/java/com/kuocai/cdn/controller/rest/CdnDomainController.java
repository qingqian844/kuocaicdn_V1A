package com.kuocai.cdn.controller.rest;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.annotation.RateLimiter;
import com.kuocai.cdn.annotation.SysLog;
import com.kuocai.cdn.api.DomainVerifyRecordInfo;
import com.kuocai.cdn.api.huawei.cdn.constant.DomainStatus;
import com.kuocai.cdn.api.tencent.dns.TencentApi;
import com.kuocai.cdn.api.tencent.dns.dto.DeleteRecordDTO;
import com.kuocai.cdn.api.tencent.dns.properties.TencentDns;
import com.kuocai.cdn.api.tencent.edgeone.TencentEdgeOneClient;
import com.kuocai.cdn.common.mongo.entity.InviteReward;
import com.kuocai.cdn.config.SystemConfig;
import com.kuocai.cdn.constant.CdnBusinessTypeMap;
import com.kuocai.cdn.constant.CdnOriginTypeMap;
import com.kuocai.cdn.constant.CdnServiceAreaMap;
import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.dto.datatable.DataTableQuery;
import com.kuocai.cdn.dto.resp.RespResult;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.enumeration.domainmerage.route.CdnOperationRoute;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.CdnDomainService;
import com.kuocai.cdn.service.EdgeOneDomainQuotaService;
import com.kuocai.cdn.service.FlowDonateService;
import com.kuocai.cdn.service.domain.operation.CdnetworksDomainServiceImpl;
import com.kuocai.cdn.service.domain.operation.ICdnPlatformService;
import com.kuocai.cdn.service.domain.operation.optional.ICdnDomainVerifyService;
import com.kuocai.cdn.service.factory.CdnPlatformFactory;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.JedisUtil;
import com.kuocai.cdn.util.ThreadMdcUtils;
import com.kuocai.cdn.vo.CdnDomainVo;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * 加速域名(CdnDomain)控制器
 *
 * @author XUEW
 * @since 2023-02-26 23:30:24
 */
@Slf4j
@RestController
@RequestMapping(value = "CdnDomain")
@Scope(value = "session")
public class CdnDomainController extends BaseController {

    private final CdnDomainService service;
    private final FlowDonateService flowDonateService;
    private final EdgeOneDomainQuotaService edgeOneDomainQuotaService;
    private final MongoTemplate mongoTemplate;
    private final Executor executorService;

    CdnDomainController(CdnDomainService service,
                        FlowDonateService flowDonateService,
                        EdgeOneDomainQuotaService edgeOneDomainQuotaService,
                        MongoTemplate mongoTemplate,
                        @Qualifier("cdnDomainExecutor") Executor executorService) {
        this.service = service;
        this.flowDonateService = flowDonateService;
        this.edgeOneDomainQuotaService = edgeOneDomainQuotaService;
        this.mongoTemplate = mongoTemplate;
        this.executorService = executorService;
    }

    /**
     * Datatables查询接口
     *
     * @param query 查询条件
     * @return 查询结果
     */
    @RateLimiter
    @PostMapping("queryForDatatables")
    public RespResult queryForDatatables(@RequestBody DataTableQuery query) {
        JSONObject datatables;
        if (isAdmin()) {
            datatables = service.queryForDatatables(null, query);
        } else {
            datatables = service.queryForDatatables(loginUserId, query);
        }
        return RespResult.success("查询成功", datatables);
    }

    /**
     * 创建加速域名
     *
     * @param domainName   加速域名
     * @param businessType 业务类型
     * @param serviceArea  服务区域
     * @param originType   源站类型
     * @param originAddr   源站
     * @return 响应
     */
    @RateLimiter
    @GetMapping("configReady")
    public RespResult configReady(@RequestParam("id") Long id) {
        if (Assert.isEmpty(id)) {
            return RespResult.paramEmpty("domainId");
        }
        CdnDomain cdnDomain = service.queryById(id);
        if (Assert.isEmpty(cdnDomain)) {
            return RespResult.notFound("domainId");
        }
        RespResult accessResult = checkDomainAccess(cdnDomain);
        if (accessResult != null) {
            return accessResult;
        }
        if (!isConfigurableStatus(cdnDomain.getDomainStatus())) {
            return RespResult.fail("域名正在配置中，请稍后刷新后再配置");
        }
        try {
            if (isConfigurableStatus(cdnDomain.getDomainStatus())) {
                return RespResult.success("配置已就绪");
            }
            loadDomainConfigForReadyCheck(cdnDomain);
            return RespResult.success("配置已就绪");
        } catch (BusinessException e) {
            log.info("域名[{}]上游配置尚未准备完成：{}", cdnDomain.getDomainName(), e.getMessage());
            return RespResult.fail("上游配置还在同步中，请稍后再进入配置");
        } catch (Exception e) {
            log.info("域名[{}]配置就绪检查失败：{}", cdnDomain.getDomainName(), e.getMessage());
            return RespResult.fail("上游配置还在同步中，请稍后再进入配置");
        }
    }

    @RateLimiter
    @GetMapping("createStatus")
    public RespResult createStatus(@RequestParam("domainName") String domainName) {
        if (Assert.isEmpty(domainName)) {
            return RespResult.paramEmpty("domainName");
        }
        CdnDomain cdnDomain = service.queryByDomainName(domainName.trim().toLowerCase());
        if (cdnDomain == null) {
            return RespResult.fail("暂未检测到本地创建记录，请继续等待或重新提交");
        }
        RespResult accessResult = checkDomainAccess(cdnDomain);
        if (accessResult != null) {
            return accessResult;
        }
        return RespResult.success("域名已进入创建流程", cdnDomain);
    }

    private boolean isConfigurableStatus(String domainStatus) {
        return "online".equals(domainStatus) || "offline".equals(domainStatus);
    }

    private void loadDomainConfigForReadyCheck(CdnDomain cdnDomain) throws BusinessException {
        String domainRoute = cdnDomain.getRoute();
        ICdnPlatformService cdnPlatformService = CdnPlatformFactory.getCdnPlatform(domainRoute);
        if ("cdnetworks".equals(domainRoute)) {
            ((CdnetworksDomainServiceImpl) cdnPlatformService).getDomainBasicConfig(cdnDomain.getDomainName());
        } else {
            cdnPlatformService.getDomainConfig(cdnDomain.getDomainName());
        }
    }

    @RateLimiter
    @GetMapping("create")
    public RespResult createByGet() {
        return RespResult.fail("创建域名请求已失效，请刷新页面后重新提交");
    }

    @RateLimiter
    @PostMapping("create")
    @SysLog(module = "站点管理", describe = "创建加速域名")
    public RespResult create(String domainName, String businessType, String serviceArea, String originType, String originAddr,
                             String originProtocol, Integer httpPort, Integer httpsPort, String originHost, Integer originWeight, String verifyCode) {
        String key = "SubmitVerifyCode:" + verifyCode;
        if (Assert.isEmpty(JedisUtil.getStr(key))) {
            return RespResult.fail("当前页面已失效，请刷新页面后再试");
        }
        // 校验用户是否欠款
        if (ObjectUtil.notEqual("admin", loginUserRoleCode)) {
            if (service.isCanStopDomain(loginUserId)) {
                return RespResult.fail("当前用户已欠费或存在尚未支付的流量账单");
            }
        }
        // 参数校验
        if (Assert.isEmpty(domainName) || Assert.isEmpty(businessType) || Assert.isEmpty(serviceArea) || Assert.isEmpty(originType)) {
            return RespResult.paramEmpty();
        }
        domainName = domainName.trim().toLowerCase();
        // 判断域名是否合法
        if (domainName.length() - domainName.replaceAll("\\.", "").length() > 9) {
            return RespResult.fail("子域名级数超出限制，最多支持 10 级域名");
        }
        if (Assert.isEmpty(CdnBusinessTypeMap.huawei.get(businessType))) {
            return RespResult.fail("暂不支持的业务类型");
        }
        if (Assert.isEmpty(CdnServiceAreaMap.huawei.get(serviceArea))) {
            return RespResult.fail("暂不支持的服务区域");
        }
        if (Assert.isEmpty(CdnOriginTypeMap.huawei.get(originType))) {
            return RespResult.fail("暂不支持的源站类型");
        }
        RespResult portCheck = validateOriginPort(httpPort, httpsPort, originWeight);
        if (portCheck != null) {
            return portCheck;
        }
        boolean edgeOneRoute = isTencentEdgeOneRoute();
        boolean edgeOneResume = false;
        CdnDomain existingDomain = cdnDomainService.queryByDomainName(domainName);
        if (Assert.notEmpty(existingDomain)) {
            boolean ownedEdgeOneDomain = edgeOneRoute
                    && "tencent_edgeone".equals(existingDomain.getRoute())
                    && ObjectUtil.equal(loginUserId, existingDomain.getUserId());
            if (!ownedEdgeOneDomain) {
                return RespResult.fail("加速域名已创建，不可重复添加");
            }
            if (!"configure_failed".equals(existingDomain.getDomainStatus())) {
                return RespResult.success("该域名已进入创建流程，请在域名列表查看状态", existingDomain);
            }
            edgeOneResume = true;
        }
        if (edgeOneRoute) {
            SysUser sysUser = sysUserService.queryById(loginUserId);
            if ("outside_mainland_china".equals(serviceArea) && (sysUser.getEnableOverseas() == null || sysUser.getEnableOverseas() != 1)) {
                return RespResult.fail("当前账号未开启EdgeOne境外加速区，请联系管理员开启");
            }
            if ("global".equals(serviceArea) && (sysUser.getEnableGlobal() == null || sysUser.getEnableGlobal() != 1)) {
                return RespResult.fail("当前账号未开启EdgeOne全球加速区，请联系管理员开启");
            }
        }
        if ("user".equals(loginUserRoleCode) && !edgeOneRoute) {
            // 数量检查
            int userDomainCount = service.queryUserDomainCount(loginUserId);
            SysUser sysUser = sysUserService.queryById(loginUserId);
            if (userDomainCount >= sysUser.getMaxDomainCount()) {
                return RespResult.fail("当前账户已达到最大创建数量，如需扩容请联系管理员~");
            }
        }
//        // 如果是海外 ??
//        if ("outside_mainland_china".equals(serviceArea)) {
//            route = CdnOperationRoute.ALIYUN.getRoute();
//            if (1814967425386180609L == loginUserId) {
//                route = CdnOperationRoute.BAISHAN.getRoute();
//            }
//            if (1690944670056411137L == loginUserId) {
//                route = CdnOperationRoute.BAISHAN.getRoute();
//            } /*
//            if (1655041628341899265L == loginUserId) {
//                route = CdnOperationRoute.ALIYUN.getRoute();
//            } */
//        }
        if ("user".equals(loginUserRoleCode) && edgeOneRoute && !edgeOneResume) {
            try {
                edgeOneDomainQuotaService.checkCreateQuota(loginUserId, domainName);
            } catch (BusinessException e) {
                if ("EDGEONE_DOMAIN_QUOTA_REQUIRED".equals(e.getMessage())) {
                    JSONObject data = new JSONObject();
                    data.put("action", "EDGEONE_QUOTA_REQUIRED");
                    data.put("summary", edgeOneDomainQuotaService.summary(loginUserId));
                    return RespResult.fail("EdgeOne根域名额度不足，请先购买额度后再添加", data);
                }
                return RespResult.fail(e.getMessage());
            }
        }
        try {
            ICdnPlatformService iCdnPlatformService = CdnPlatformFactory.getCdnPlatform(route);
            CdnDomain domain = iCdnPlatformService.create(loginUserId, domainName, businessType, serviceArea, originType, originAddr,
                    originProtocol, httpPort, httpsPort, originHost, originWeight);
            domain = service.save(domain);
            if ("user".equals(loginUserRoleCode) && "tencent_edgeone".equals(route)) {
                edgeOneDomainQuotaService.recordRootDomain(loginUserId, domainName, domain.getId());
            }
            JedisUtil.delKey(key);
            CdnDomain finalDomain = domain;
            executorService.execute(ThreadMdcUtils.wrapAsync(() -> {
                try {
                    // 这块业务必须单独拎出来，否则会造成数据保存异常
                    iCdnPlatformService.configDNS(finalDomain);
                } catch (Exception e) {
                    log.error("配置域名（{}）失败：{}", finalDomain.getDomainName(), e.getMessage());
                }
                // query one where userId = loginUserId
                Query query = Query.query(Criteria.where("userId").is(loginUserId));
                InviteReward one = mongoTemplate.findOne(query, InviteReward.class);
                if (Assert.notEmpty(one) && !one.isInviteUserReceived()) {
                     flowDonateService.sendFlowGift("推荐注册奖励", one.getInviteUserId(), SystemConfig.websiteBaseConfig.getInviteRewardGb(), 365);
                    one.setInviteUserReceived();
                    mongoTemplate.save(one);
                }
            }, MDC.getCopyOfContextMap()));
            return RespResult.success("创建成功，配置过程大约5分钟", domain);
        } catch (Exception e) {
            log.error("创建域名失败：域名：{}，错误原因：{}", domainName, e.getMessage());
            log.error("创建域名异常", e);
            if (isDomainVerifyRequired(e.getMessage())) {
                JSONObject data = new JSONObject();
                data.put("needVerify", true);
                data.put("originalDomainName", domainName);
                String verifyDomainName = domainName;
                if ("tencent_edgeone".equals(route)) {
                    try {
                        verifyDomainName = TencentEdgeOneClient.getRootDomain(domainName);
                    } catch (Exception ignored) {
                    }
                }
                data.put("domainName", verifyDomainName);
                data.put("serviceArea", serviceArea);
                try {
                    ICdnPlatformService verifyPlatformService = CdnPlatformFactory.getCdnPlatform(route);
                    if (verifyPlatformService instanceof ICdnDomainVerifyService) {
                        String strippedDomain = verifyDomainName.startsWith("www.") ? verifyDomainName.substring(4) : verifyDomainName;
                        DomainVerifyRecordInfo info = ((ICdnDomainVerifyService) verifyPlatformService).createVerifyRecord(strippedDomain, serviceArea);
                        data.put("verifyInfo", info);
                    }
                } catch (Exception verifyException) {
                    data.put("verifyError", verifyException.getMessage());
                    log.error("获取域名归属权验证信息失败：域名：{}，错误原因：{}", domainName, verifyException.getMessage());
                }
                return RespResult.fail(e.getMessage(), data);
            }
            if (!edgeOneRoute) {
                JedisUtil.delKey(key);
            }
            return RespResult.fail(e.getMessage());
        }
    }

    private RespResult validateOriginPort(Integer httpPort, Integer httpsPort, Integer originWeight) {
        if (httpPort != null && (httpPort < 1 || httpPort > 65535)) {
            return RespResult.fail("HTTP端口必须在1-65535范围内");
        }
        if (httpsPort != null && (httpsPort < 1 || httpsPort > 65535)) {
            return RespResult.fail("HTTPS端口必须在1-65535范围内");
        }
        if (originWeight != null && (originWeight < 1 || originWeight > 100)) {
            return RespResult.fail("权重必须在1-100范围内");
        }
        return null;
    }

    private boolean isTencentEdgeOneRoute() {
        if ("tencent_edgeone".equals(route)) {
            return true;
        }
        if (Assert.notEmpty(loginUser) && "tencent_edgeone".equals(loginUser.getRoute())) {
            route = loginUser.getRoute();
            return true;
        }
        if (Assert.notEmpty(loginUserId)) {
            SysUser sysUser = sysUserService.queryById(loginUserId);
            if (Assert.notEmpty(sysUser) && "tencent_edgeone".equals(sysUser.getRoute())) {
                route = sysUser.getRoute();
                return true;
            }
        }
        return false;
    }

    private boolean isDomainVerifyRequired(String message) {
        if (Assert.isEmpty(message)) {
            return false;
        }
        String lower = message.toLowerCase();
        if (message.contains("CdnDomainRecordNotVerified")
                || message.contains("CdnTxtRecordValueNotMatch")
                || lower.contains("domainrecordnotverified")
                || lower.contains("txtrecordvaluenotmatch")) {
            return true;
        }
        if (message.contains("无权限")
                || lower.contains("unauthorized")
                || lower.contains("not authorized")
                || lower.contains("createaccelerationdomain")) {
            return false;
        }
        return message.contains("\u9a8c\u8bc1")
                || message.contains("\u5f52\u5c5e")
                || lower.contains("verify")
                || lower.contains("owner")
                || lower.contains("auth");
    }

    /**
     * 保存加速域名信息
     *
     * @param domainVo 郁闷信息
     * @return 响应
     */
    @RateLimiter
    @PostMapping("save")
    @SysLog(module = "站点管理", describe = "更新加速域名")
    public RespResult save(CdnDomainVo domainVo) {
        Long id = domainVo.getId();
        String businessType = domainVo.getBusinessType();
        String serviceArea = domainVo.getServiceArea();
        if (Assert.isEmpty(id) || Assert.isEmpty(businessType) || Assert.isEmpty(serviceArea)) {
            return RespResult.fail("参数异常");
        }
        CdnDomain cdnDomain = service.queryById(id);
        if (Assert.isEmpty(cdnDomain)) {
            return RespResult.fail("没有对应的加速域名");
        }
        RespResult accessResult = checkDomainAccess(cdnDomain);
        if (accessResult != null) {
            return accessResult;
        }
        try {
            ICdnPlatformService iCdnPlatformService = CdnPlatformFactory.getCdnPlatform(cdnDomain.getRoute());
            iCdnPlatformService.save(cdnDomain, businessType, serviceArea);
            cdnDomain.setBusinessType(businessType);
            cdnDomain.setServiceArea(serviceArea);
            // 修改本地加速域名状态为配置中
            cdnDomain.setDomainStatus(DomainStatus.CONFIGURING);
            // 修改本地
            service.save(cdnDomain);
        } catch (BusinessException e) {
            service.updateConfiguring(id);
            return RespResult.fail(e.getMessage());
        }
        return RespResult.success("配置正在部署中，大约需要5分钟的时间完成部署，请稍后。");
    }


    /**
     * 是否开启域名的ipv6加速
     *
     * @param id     域名id
     * @param status 状态
     * @return 响应
     */
    @RateLimiter
    @PostMapping("ipv6")
    @SysLog(module = "站点管理", describe = "更新加速域名的IPV6信息")
    public RespResult ipv6(Long id, Integer status) {
        if (Assert.isEmpty(id) || Assert.isEmpty(status)) {
            return RespResult.fail("参数错误");
        }
        CdnDomain cdnDomain = service.queryById(id);
        if (Assert.isEmpty(cdnDomain)) {
            return RespResult.fail("没有对应的加速域名");
        }
        RespResult accessResult = checkDomainAccess(cdnDomain);
        if (accessResult != null) {
            return accessResult;
        }
        try {
            ICdnPlatformService iCdnPlatformService = CdnPlatformFactory.getCdnPlatform(cdnDomain.getRoute());
            iCdnPlatformService.ipv6(cdnDomain, status);
        } catch (Exception e) {
            return RespResult.fail(e.getMessage());
        }
        // 修改本地加速域名状态为配置中
        service.updateConfiguring(id);
        return RespResult.success("配置正在部署中，大约需要5分钟的时间完成部署，请稍后。");
    }

    /**
     * 停用加速域名
     *
     * @param id 加速域名ID
     * @return 响应
     */
    @RateLimiter
    @PostMapping("disable")
    @SysLog(module = "站点管理", describe = "停用加速域名")
    public RespResult disable(Long id) {
        if (Assert.isEmpty(id)) {
            return RespResult.paramEmpty("域名ID");
        }
        CdnDomain cdnDomain = service.queryById(id);
        if (Assert.isEmpty(cdnDomain)) {
            return RespResult.notFound("域名ID");
        }
        RespResult accessResult = checkDomainAccess(cdnDomain);
        if (accessResult != null) {
            return accessResult;
        }
        try {
            ICdnPlatformService iCdnPlatformService = CdnPlatformFactory.getCdnPlatform(cdnDomain.getRoute());
            iCdnPlatformService.disable(cdnDomain);
            service.updateConfiguring(id);
            return RespResult.success("停用成功，配置过程大约5分钟", cdnDomain);
        } catch (Exception e) {
            service.updateConfiguring(id);
            return RespResult.fail(e.getMessage());
        }
    }

    /**
     * 启用加速域名
     *
     * @param id 加速域名ID
     * @return 响应
     */
    @RateLimiter
    @PostMapping("enable")
    @SysLog(module = "站点管理", describe = "启用加速域名")
    public RespResult enable(Long id) {
        // 校验用户是否欠款
        if (ObjectUtil.notEqual("admin", loginUserRoleCode)) {
            if (service.isCanStopDomain(loginUserId)) {
                return RespResult.fail("当前用户还有账单未结清，或余额不足");
            }
        }
        if (Assert.isEmpty(id)) {
            return RespResult.paramEmpty("域名ID");
        }
        CdnDomain cdnDomain = service.queryById(id);
        if (Assert.isEmpty(cdnDomain)) {
            return RespResult.notFound("域名ID");
        }
        RespResult accessResult = checkDomainAccess(cdnDomain);
        if (accessResult != null) {
            return accessResult;
        }
        try {
            ICdnPlatformService iCdnPlatformService = CdnPlatformFactory.getCdnPlatform(cdnDomain.getRoute());
            iCdnPlatformService.enable(cdnDomain);
            service.updateConfiguring(id);
            return RespResult.success("启用成功，配置过程大约5分钟", cdnDomain);
        } catch (Exception e) {
            service.updateConfiguring(id);
            return RespResult.fail(e.getMessage());
        }
    }

    /**
     * 删除加速域名
     *
     * @param id 加速域名ID
     * @return 响应
     */
    @RateLimiter
    @PostMapping("delete")
    @SysLog(module = "站点管理", describe = "删除加速域名")
    public RespResult delete(Long id) {
        if (Assert.isEmpty(id)) {
            return RespResult.paramEmpty("域名ID");
        }
        CdnDomain cdnDomain = service.queryById(id);
        if (Assert.isEmpty(cdnDomain)) {
            return RespResult.notFound("域名ID");
        }
        RespResult accessResult = checkDomainAccess(cdnDomain);
        if (accessResult != null) {
            return accessResult;
        }
        try {
            ICdnPlatformService iCdnPlatformService = CdnPlatformFactory.getCdnPlatform(cdnDomain.getRoute());
            iCdnPlatformService.delete(cdnDomain);
            // 删除域名信息
            service.deleteById(cdnDomain.getId());
            log.info("删除加速域名成功，域名信息：{}", cdnDomain);
            // 根据domainId删除源站信息
            deleteDnsRecord(cdnDomain);
            // TODO 根据domainId删除回源信息
            // TODO 根据domainId删除统计信息
            return RespResult.success("删除成功");
        } catch (Exception e) {
            return RespResult.fail(e.getMessage());
        }
    }

    /**
     * 强制删除加速域名
     *
     * @param id 加速域名ID
     * @return 响应
     */
    @RateLimiter
    @PostMapping("forceDelete")
    @SysLog(module = "站点管理", describe = "强制删除加速域名")
    public RespResult forceDelete(Long id) {
        CdnDomain cdnDomain = service.queryById(id);
        RespResult accessResult = checkDomainAccess(cdnDomain);
        if (accessResult != null) {
            return accessResult;
        }
        if (service.deleteById(id) > 0) {
            deleteDnsRecord(cdnDomain);
            return RespResult.success("强制删除成功");
        }
        return RespResult.fail("删除失败");
    }

    @RateLimiter
    @PostMapping("createVerifyRecord")
    @SysLog(module = "站点管理", describe = "创建域名解析验证记录")
    public RespResult createVerifyRecord(String domainName, String area) {
        List<String> routes = Arrays.asList("tencent", "tencent_edgeone", "aliyun", "baidu", "kingsoft", "volcengine");
        if (!routes.contains(route)) {
            return RespResult.fail("请稍后再试");
        }
        if ("outside_mainland_china".equals(area) && !"tencent_edgeone".equals(route) && !"tencent".equals(route)) {
            route = "aliyun";
        }
        if (Assert.isEmpty(domainName)) {
            return RespResult.paramEmpty();
        }
        try {
            ICdnPlatformService iCdnPlatformService = CdnPlatformFactory.getCdnPlatform(route);
            if (iCdnPlatformService instanceof ICdnDomainVerifyService) {
                String strippedDomain = domainName.startsWith("www.") ? domainName.substring(4) : domainName;
                DomainVerifyRecordInfo info = ((ICdnDomainVerifyService) iCdnPlatformService).createVerifyRecord(strippedDomain, area);
                return RespResult.success("success", info);
            }
            return RespResult.fail("非法请求，请稍后再试");
        } catch (Exception e) {
            return RespResult.fail(e.getMessage());
        }
    }

    @RateLimiter
    @PostMapping("verifyDomainRecord")
    @SysLog(module = "站点管理", describe = "验证域名解析")
    public RespResult verifyDomainRecord(String domainName, String verifyType, String area) {
        List<String> routes = Arrays.asList("tencent", "tencent_edgeone", "aliyun", "baidu", "kingsoft", "volcengine");
        if (!routes.contains(route)) {
            return RespResult.fail("请稍后再试");
        }
        if ("outside_mainland_china".equals(area) && !"tencent_edgeone".equals(route) && !"tencent".equals(route)) {
            route = "aliyun";
        }
        if (Assert.isEmpty(domainName) || Assert.isEmpty(verifyType)) {
            return RespResult.paramEmpty();
        }
        try {
            ICdnPlatformService iCdnPlatformService = CdnPlatformFactory.getCdnPlatform(route);
            if (iCdnPlatformService instanceof ICdnDomainVerifyService) {
                String strippedDomain = domainName.startsWith("www.") ? domainName.substring(4) : domainName;
                ((ICdnDomainVerifyService) iCdnPlatformService).verifyDomainRecord(strippedDomain, verifyType);
                return RespResult.success("域名验证成功！");
            }
            return RespResult.fail("非法请求，请稍后再试");
        } catch (Exception e) {
            return RespResult.fail(e.getMessage());
        }
    }

    private void deleteDnsRecord(CdnDomain cdnDomain) {
        executorService.execute(ThreadMdcUtils.wrapAsync(() -> {
            DeleteRecordDTO deleteRecordDTO = new DeleteRecordDTO();
            Long tencentNdsId = cdnDomain.getTencentDnsId();
            deleteRecordDTO.setRecordId(tencentNdsId);
            deleteRecordDTO.setDomain(TencentDns.LOCAL_DOMAIN_NAME);
            try {
                TencentApi.deleteRecord(deleteRecordDTO);
                log.info("删除域名解析记录成功，域名解析记录ID：{}，域名：{}", tencentNdsId, cdnDomain.getDomainName());
            } catch (Exception e) {
                log.error("删除域名解析记录失败：{}", e.getMessage());
            }
        }, MDC.getCopyOfContextMap()));
    }
}
