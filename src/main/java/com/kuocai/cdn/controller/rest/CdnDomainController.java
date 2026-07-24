package com.kuocai.cdn.controller.rest;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONArray;
import com.kuocai.cdn.annotation.RateLimiter;
import com.kuocai.cdn.annotation.SysLog;
import com.kuocai.cdn.api.DomainConfig;
import com.kuocai.cdn.api.DomainVerifyRecordInfo;
import com.kuocai.cdn.api.huawei.cdn.constant.DomainStatus;
import com.kuocai.cdn.api.tencent.dns.TencentApi;
import com.kuocai.cdn.api.tencent.dns.dto.DeleteRecordDTO;
import com.kuocai.cdn.api.tencent.dns.properties.TencentDns;
import com.kuocai.cdn.api.tencent.edgeone.TencentEdgeOneClient;
import com.kuocai.cdn.constant.CdnBusinessTypeMap;
import com.kuocai.cdn.constant.CdnOriginTypeMap;
import com.kuocai.cdn.constant.CdnServiceAreaMap;
import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.dto.datatable.DataTableQuery;
import com.kuocai.cdn.dto.resp.RespResult;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.enumeration.domainmerage.CdnRoute;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.CdnDomainService;
import com.kuocai.cdn.service.CdnAreaRouteService;
import com.kuocai.cdn.service.CdnServiceAreaPolicyService;
import com.kuocai.cdn.service.EdgeOneDomainQuotaService;
import com.kuocai.cdn.service.FlowDonateService;
import com.kuocai.cdn.service.domain.operation.CdnetworksDomainServiceImpl;
import com.kuocai.cdn.service.domain.operation.ICdnPlatformService;
import com.kuocai.cdn.service.domain.operation.MultiCdnDomainServiceImpl;
import com.kuocai.cdn.service.domain.operation.optional.ICdnDomainVerifyService;
import com.kuocai.cdn.service.factory.CdnPlatformFactory;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.EdgeOneFailureReasonFormatter;
import com.kuocai.cdn.util.JedisUtil;
import com.kuocai.cdn.util.ThreadMdcUtils;
import com.kuocai.cdn.vo.CdnDomainVo;
import com.kuocai.cdn.vo.AreaRouteTargetVo;
import com.kuocai.cdn.vo.ResolvedAreaRouteVo;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.core.MongoTemplate;
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
    private final EdgeOneDomainQuotaService edgeOneDomainQuotaService;
    private final CdnServiceAreaPolicyService cdnServiceAreaPolicyService;
    private final CdnAreaRouteService cdnAreaRouteService;
    private final MultiCdnDomainServiceImpl multiCdnDomainService;
    private final Executor executorService;

    @Autowired
    CdnDomainController(CdnDomainService service,
                        EdgeOneDomainQuotaService edgeOneDomainQuotaService,
                        CdnServiceAreaPolicyService cdnServiceAreaPolicyService,
                        CdnAreaRouteService cdnAreaRouteService,
                        MultiCdnDomainServiceImpl multiCdnDomainService,
                        @Qualifier("cdnDomainExecutor") Executor executorService) {
        this.service = service;
        this.edgeOneDomainQuotaService = edgeOneDomainQuotaService;
        this.cdnServiceAreaPolicyService = cdnServiceAreaPolicyService;
        this.cdnAreaRouteService = cdnAreaRouteService;
        this.multiCdnDomainService = multiCdnDomainService;
        this.executorService = executorService;
    }

    /**
     * Compatibility constructor for integrations compiled against the pre-open-source signature.
     */
    CdnDomainController(CdnDomainService service, FlowDonateService ignoredFlowDonateService,
                        EdgeOneDomainQuotaService edgeOneDomainQuotaService, MongoTemplate ignoredMongoTemplate,
                        Executor executorService) {
        this(service, edgeOneDomainQuotaService, null, null, null, executorService);
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
        try {
            if (isConfigurableStatus(cdnDomain.getDomainStatus())) {
                if (Assert.notEmpty(cdnDomain.getFailureReason())) {
                    service.clearFailureReason(cdnDomain.getId());
                }
                return RespResult.success("配置已就绪");
            }
            DomainConfig domainConfig = loadDomainConfigForReadyCheck(cdnDomain);
            String upstreamStatus = domainConfig == null || domainConfig.getDomainBasicInfo() == null
                    ? null : domainConfig.getDomainBasicInfo().getDomainStatus();
            if (isConfigurableStatus(upstreamStatus)) {
                cdnDomain.setDomainStatus(upstreamStatus);
                cdnDomain.setFailureReason(null);
                service.save(cdnDomain);
                service.clearFailureReason(cdnDomain.getId());
                return RespResult.success("配置已就绪");
            }
            if (DomainStatus.CONFIGURE_FAILED.equals(upstreamStatus)) {
                cdnDomain.setDomainStatus(upstreamStatus);
                if (CdnRoute.TENCENT_EDGEONE.getCode().equals(cdnDomain.getRoute())
                        && Assert.isEmpty(cdnDomain.getFailureReason())) {
                    cdnDomain.setFailureReason(
                            EdgeOneFailureReasonFormatter.defaultReason(cdnDomain.getServiceArea()));
                }
                service.save(cdnDomain);
                return RespResult.fail(Assert.notEmpty(cdnDomain.getFailureReason())
                        ? cdnDomain.getFailureReason()
                        : "上游存在配置失败的域名，请联系管理员处理");
            }
            return RespResult.fail("域名正在配置中，请稍后刷新后再配置");
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

    private DomainConfig loadDomainConfigForReadyCheck(CdnDomain cdnDomain) throws BusinessException {
        String domainRoute = cdnDomain.getRoute();
        ICdnPlatformService cdnPlatformService = CdnPlatformFactory.getCdnPlatform(domainRoute);
        if ("cdnetworks".equals(domainRoute)) {
            return ((CdnetworksDomainServiceImpl) cdnPlatformService)
                    .getDomainBasicConfig(cdnDomain.getDomainName());
        }
        return cdnPlatformService.getDomainConfig(cdnDomain.getDomainName());
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
        ResolvedAreaRouteVo routePlan;
        try {
            routePlan = cdnAreaRouteService.resolve(loginUserId, route, domainName, serviceArea);
        } catch (BusinessException e) {
            return RespResult.fail(e.getMessage());
        }
        AreaRouteTargetVo primaryTarget = routePlan.getPrimaryTarget();
        String effectiveRoute = routePlan.isMultiCdn()
                ? CdnRoute.MULTI_CDN.getCode() : primaryTarget.getRoute();
        boolean edgeOneRoute = CdnRoute.TENCENT_EDGEONE.getCode().equals(effectiveRoute);
        boolean containsEdgeOne = routePlan.getTargets().stream()
                .anyMatch(target -> CdnRoute.TENCENT_EDGEONE.getCode().equals(target.getRoute()));
        boolean multiCdnRoute = CdnRoute.isMultiCdn(effectiveRoute);
        boolean edgeOneResume = false;
        boolean selfHostedRoute = CdnRoute.isSelfHosted(effectiveRoute);
        boolean selfHostedResume = false;
        boolean multiCdnResume = false;
        CdnDomain existingDomain = cdnDomainService.queryByDomainName(domainName);
        if (Assert.notEmpty(existingDomain)) {
            boolean ownedEdgeOneDomain = edgeOneRoute
                    && "tencent_edgeone".equals(existingDomain.getRoute())
                    && ObjectUtil.equal(loginUserId, existingDomain.getUserId());
            boolean ownedSelfHostedDomain = selfHostedRoute
                    && CdnRoute.isSelfHosted(existingDomain.getRoute())
                    && ObjectUtil.equal(loginUserId, existingDomain.getUserId());
            boolean ownedMultiCdnDomain = multiCdnRoute
                    && CdnRoute.isMultiCdn(existingDomain.getRoute())
                    && ObjectUtil.equal(loginUserId, existingDomain.getUserId());
            if (!ownedEdgeOneDomain && !ownedSelfHostedDomain && !ownedMultiCdnDomain) {
                return RespResult.fail("加速域名已创建，不可重复添加");
            }
            if (!"configure_failed".equals(existingDomain.getDomainStatus())) {
                return RespResult.success("该域名已进入创建流程，请在域名列表查看状态", existingDomain);
            }
            edgeOneResume = ownedEdgeOneDomain;
            selfHostedResume = ownedSelfHostedDomain;
            multiCdnResume = ownedMultiCdnDomain;
        }
        if ("user".equals(loginUserRoleCode) && !edgeOneRoute && !selfHostedResume && !multiCdnResume) {
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
        try {
            ICdnPlatformService iCdnPlatformService = CdnPlatformFactory.getCdnPlatform(effectiveRoute);
            CdnDomain domain;
            if (multiCdnRoute) {
                domain = multiCdnDomainService.create(routePlan, loginUserId, domainName, businessType,
                        serviceArea, originType, originAddr, originProtocol, httpPort, httpsPort,
                        originHost, originWeight);
            } else {
                domain = iCdnPlatformService.create(loginUserId, domainName, businessType, serviceArea, originType, originAddr,
                        originProtocol, httpPort, httpsPort, originHost, originWeight);
            }
            domain = service.save(domain);
            if (multiCdnRoute) {
                multiCdnDomainService.persistBindings(domain);
            }
            if (containsEdgeOne) {
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
                if (containsEdgeOne) {
                    try {
                        verifyDomainName = TencentEdgeOneClient.getRootDomain(domainName);
                    } catch (Exception ignored) {
                    }
                }
                data.put("domainName", verifyDomainName);
                data.put("serviceArea", serviceArea);
                try {
                    JSONObject verifyInfo = buildVerifyRecordPayload(routePlan, domainName, serviceArea);
                    if (verifyInfo != null) {
                        data.put("verifyInfo", verifyInfo);
                        if (Assert.notEmpty(verifyInfo.getString("domainName"))) {
                            data.put("domainName", verifyInfo.getString("domainName"));
                        }
                    }
                } catch (Exception verifyException) {
                    data.put("verifyError", verifyException.getMessage());
                    log.error("获取域名归属权验证信息失败：域名：{}，错误原因：{}", domainName, verifyException.getMessage());
                }
                return RespResult.fail(e.getMessage(), data);
            }
            if (!containsEdgeOne) {
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

    @RateLimiter
    @PostMapping("retrySelfHostedConfig")
    @SysLog(module = "站点管理", describe = "重试自建 CDN 域名配置")
    public RespResult retrySelfHostedConfig(@RequestParam("id") Long id) {
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
        if (!CdnRoute.isSelfHosted(cdnDomain.getRoute())) {
            return RespResult.fail("仅自建 CDN 域名支持此重试操作");
        }
        if (!"configure_failed".equals(cdnDomain.getDomainStatus())) {
            return RespResult.fail("当前域名状态无需重试配置");
        }
        try {
            CdnDomain updated = CdnPlatformFactory.getCdnPlatform(cdnDomain.getRoute()).configDNS(cdnDomain);
            return RespResult.success("自建 CDN 域名配置成功", updated);
        } catch (Exception e) {
            log.error("重试自建 CDN 域名[{}]配置失败：{}", cdnDomain.getDomainName(), e.getMessage(), e);
            return RespResult.fail("自建 CDN 域名配置失败：" + e.getMessage());
        }
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
            if (CdnRoute.isMultiCdn(cdnDomain.getRoute())) {
                if (!ObjectUtil.equal(cdnDomain.getServiceArea(), serviceArea)) {
                    return RespResult.fail("多 CDN 线路组创建后不能直接切换加速区域，请删除域名后按新区域重新创建");
                }
            }
            if (!CdnRoute.isMultiCdn(cdnDomain.getRoute())) {
                cdnServiceAreaPolicyService.requireAllowed(cdnDomain.getRoute(), serviceArea);
            }
        } catch (BusinessException e) {
            return RespResult.fail(e.getMessage());
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
            if (!CdnRoute.isSelfHosted(cdnDomain.getRoute())) {
                service.updateConfiguring(id);
            }
            return RespResult.success("停用成功，配置过程大约5分钟", cdnDomain);
        } catch (Exception e) {
            if (!CdnRoute.isSelfHosted(cdnDomain.getRoute())) {
                service.updateConfiguring(id);
            }
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
            if (!CdnRoute.isSelfHosted(cdnDomain.getRoute())) {
                service.updateConfiguring(id);
            }
            return RespResult.success("启用成功，配置过程大约5分钟", cdnDomain);
        } catch (Exception e) {
            if (!CdnRoute.isSelfHosted(cdnDomain.getRoute())) {
                service.updateConfiguring(id);
            }
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
        if (CdnRoute.isMultiCdn(cdnDomain.getRoute())) {
            multiCdnDomainService.forceCleanup(cdnDomain);
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
        if (Assert.isEmpty(domainName) || Assert.isEmpty(area)) {
            return RespResult.paramEmpty();
        }
        try {
            ResolvedAreaRouteVo plan = cdnAreaRouteService.resolve(
                    loginUserId, route, domainName, area);
            JSONObject result = buildVerifyRecordPayload(plan, domainName, area);
            if (result == null) {
                return RespResult.fail("当前线路组不需要域名归属权验证");
            }
            return RespResult.success("success", result);
        } catch (Exception e) {
            return RespResult.fail(e.getMessage());
        }
    }

    private JSONObject buildVerifyRecordPayload(ResolvedAreaRouteVo plan, String domainName, String area)
            throws BusinessException {
        String strippedDomain = domainName.startsWith("www.") ? domainName.substring(4) : domainName;
        JSONArray records = new JSONArray();
        for (AreaRouteTargetVo target : plan.getTargets()) {
            ICdnPlatformService platform = CdnPlatformFactory.getCdnPlatform(target.getRoute());
            if (!(platform instanceof ICdnDomainVerifyService)) {
                continue;
            }
            DomainVerifyRecordInfo info = ((ICdnDomainVerifyService) platform)
                    .createVerifyRecord(strippedDomain, area);
            if (info == null) {
                continue;
            }
            JSONObject item = JSONObject.parseObject(JSONObject.toJSONString(info));
            item.put("targetKey", target.getTargetKey());
            item.put("targetName", target.getRouteName());
            records.add(item);
        }
        if (records.isEmpty()) {
            return null;
        }
        JSONObject result = new JSONObject(records.getJSONObject(0));
        result.put("verifyRecords", records);
        return result;
    }

    @RateLimiter
    @PostMapping("verifyDomainRecord")
    @SysLog(module = "站点管理", describe = "验证域名解析")
    public RespResult verifyDomainRecord(String domainName, String verifyType, String area) {
        if (Assert.isEmpty(domainName) || Assert.isEmpty(verifyType) || Assert.isEmpty(area)) {
            return RespResult.paramEmpty();
        }
        try {
            String strippedDomain = domainName.startsWith("www.") ? domainName.substring(4) : domainName;
            ResolvedAreaRouteVo plan = cdnAreaRouteService.resolve(
                    loginUserId, route, strippedDomain, area);
            List<String> failures = new java.util.ArrayList<>();
            int verifiedTargets = 0;
            for (AreaRouteTargetVo target : plan.getTargets()) {
                ICdnPlatformService platform = CdnPlatformFactory.getCdnPlatform(target.getRoute());
                if (!(platform instanceof ICdnDomainVerifyService)) {
                    continue;
                }
                verifiedTargets++;
                try {
                    ((ICdnDomainVerifyService) platform).verifyDomainRecord(strippedDomain, verifyType);
                } catch (Exception e) {
                    failures.add(target.getRouteName() + "：" + e.getMessage());
                }
            }
            if (verifiedTargets == 0) {
                return RespResult.success("当前线路组无需验证，正在继续创建");
            }
            if (!failures.isEmpty()) {
                return RespResult.fail("部分线路验证失败：" + String.join("；", failures));
            }
            return RespResult.success("域名验证成功！");
        } catch (Exception e) {
            return RespResult.fail(e.getMessage());
        }
    }

    private void deleteDnsRecord(CdnDomain cdnDomain) {
        if (cdnDomain == null || cdnDomain.getTencentDnsId() == null) {
            return;
        }
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
