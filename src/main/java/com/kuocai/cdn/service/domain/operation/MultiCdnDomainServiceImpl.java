package com.kuocai.cdn.service.domain.operation;

import com.alibaba.fastjson.JSON;
import com.kuocai.cdn.api.DomainBasicInfo;
import com.kuocai.cdn.api.DomainConfig;
import com.kuocai.cdn.api.huawei.cdn.constant.DomainStatus;
import com.kuocai.cdn.api.tencent.dns.CreateRecordResponse;
import com.kuocai.cdn.api.tencent.dns.TencentApi;
import com.kuocai.cdn.api.tencent.dns.dto.CreateRecordDTO;
import com.kuocai.cdn.api.tencent.dns.dto.DeleteRecordDTO;
import com.kuocai.cdn.api.tencent.dns.properties.TencentDns;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.entity.CdnDomainRouteBinding;
import com.kuocai.cdn.entity.SelfHostedDomainConfig;
import com.kuocai.cdn.entity.SelfHostedNodeGroup;
import com.kuocai.cdn.enumeration.domainmerage.CdnRoute;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.CdnDomainRouteBindingService;
import com.kuocai.cdn.service.CdnDomainService;
import com.kuocai.cdn.service.SelfHostedCdnService;
import com.kuocai.cdn.service.factory.CdnPlatformFactory;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.DomainUtil;
import com.kuocai.cdn.vo.AreaRouteTargetVo;
import com.kuocai.cdn.vo.CdnDomainSourcesVo;
import com.kuocai.cdn.vo.DomainHttpsSettingVo;
import com.kuocai.cdn.vo.DomainOriginSettingVo;
import com.kuocai.cdn.vo.EdgeOneSecurityPolicyVo;
import com.kuocai.cdn.vo.IgnoreQueryStringDTO;
import com.kuocai.cdn.vo.ResolvedAreaRouteVo;
import com.kuocai.cdn.vo.SettingAccessVo;
import com.kuocai.cdn.vo.SettingCacheVo;
import com.kuocai.cdn.vo.SettingHigherVo;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class MultiCdnDomainServiceImpl implements ICdnPlatformService {

    private final CdnDomainService cdnDomainService;
    private final CdnDomainRouteBindingService bindingService;
    private final SelfHostedCdnService selfHostedCdnService;

    public MultiCdnDomainServiceImpl(CdnDomainService cdnDomainService,
                                     CdnDomainRouteBindingService bindingService,
                                     SelfHostedCdnService selfHostedCdnService) {
        this.cdnDomainService = cdnDomainService;
        this.bindingService = bindingService;
        this.selfHostedCdnService = selfHostedCdnService;
    }

    public CdnDomain create(ResolvedAreaRouteVo routePlan, Long userId, String domainName,
                            String businessType, String serviceArea, String originType, String originAddr,
                            String originProtocol, Integer httpPort, Integer httpsPort,
                            String originHost, Integer originWeight) throws BusinessException {
        if (routePlan == null || Assert.isEmpty(routePlan.getTargets())) {
            throw new BusinessException("多 CDN 线路组未配置可用厂商");
        }
        List<CreatedTarget> createdTargets = new ArrayList<>();
        try {
            for (AreaRouteTargetVo target : routePlan.getTargets()) {
                ICdnPlatformService platform = CdnPlatformFactory.getCdnPlatform(target.getRoute());
                CdnDomain child;
                try {
                    if (platform instanceof SelfHostedDomainServiceImpl) {
                        child = ((SelfHostedDomainServiceImpl) platform).createForRoute(
                                target.getRoute(), userId, domainName, businessType, serviceArea,
                                originType, originAddr, originProtocol, httpPort, httpsPort,
                                originHost, originWeight);
                    } else {
                        child = platform.create(userId, domainName, businessType, serviceArea, originType, originAddr,
                                originProtocol, httpPort, httpsPort, originHost, originWeight);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new BusinessException("创建多 CDN 域名被中断");
                }
                if (child == null) {
                    throw new BusinessException(target.getRouteName() + "未返回域名信息");
                }
                child.setRoute(target.getRoute());
                CreatedTarget createdTarget = new CreatedTarget(target, child, null);
                createdTargets.add(createdTarget);
                String upstreamCname = platform instanceof SelfHostedDomainServiceImpl
                        ? ((SelfHostedDomainServiceImpl) platform).prepareForMultiCdn(child)
                        : extractUpstreamCname(child, target.getRoute());
                if (Assert.isEmpty(upstreamCname)) {
                    throw new BusinessException(target.getRouteName() + "尚未返回上游 CNAME");
                }
                createdTarget.upstreamCname = upstreamCname;
            }
            return consolidate(createdTargets, userId, domainName, businessType, serviceArea);
        } catch (Exception e) {
            rollbackCreatedTargets(createdTargets);
            if (e instanceof BusinessException) {
                throw (BusinessException) e;
            }
            throw new BusinessException(e.getMessage());
        }
    }

    @Override
    public CdnDomain create(Long userId, String domainName, String businessType, String serviceArea,
                            String originType, String ipOrDomain) throws BusinessException {
        throw new BusinessException("多 CDN 域名必须通过区域线路组创建");
    }

    private CdnDomain consolidate(List<CreatedTarget> createdTargets, Long userId, String domainName,
                                  String businessType, String serviceArea) {
        CreatedTarget primary = choosePrimary(createdTargets);
        CdnDomain parent = primary.child;
        parent.setUserId(userId);
        parent.setDomainName(domainName);
        parent.setBusinessType(businessType);
        parent.setServiceArea(serviceArea);
        parent.setRoute(CdnRoute.MULTI_CDN.getCode());
        parent.setCname(null);
        parent.setTencentDnsId(null);
        parent.setUpdateTime(new Date());

        List<CdnDomainRouteBinding> bindings = new ArrayList<>();
        for (CreatedTarget item : createdTargets) {
            boolean isPrimary = item == primary;
            Long localDomainId = item.child.getId();
            if (!isPrimary && localDomainId != null) {
                cdnDomainService.deleteById(localDomainId);
                localDomainId = null;
            }
            mergeVendorCname(parent, item.child);
            bindings.add(CdnDomainRouteBinding.builder()
                    .route(item.target.getRoute())
                    .targetKey(item.target.getTargetKey())
                    .upstreamDomainId(item.child.getDomainId())
                    .upstreamCname(item.upstreamCname)
                    .domainSnapshotJson(JSON.toJSONString(item.child))
                    .localDomainId(localDomainId)
                    .primaryBinding(isPrimary ? 1 : 0)
                    .status(CdnDomainRouteBindingService.STATUS_ACTIVE)
                    .build());
        }
        parent.setRouteBindings(bindings);
        return parent;
    }

    private CreatedTarget choosePrimary(List<CreatedTarget> targets) {
        for (CreatedTarget target : targets) {
            if (CdnRoute.isSelfHosted(target.target.getRoute()) && target.child.getId() != null) {
                return target;
            }
        }
        for (CreatedTarget target : targets) {
            if (target.child.getId() != null) {
                return target;
            }
        }
        return targets.get(0);
    }

    public void persistBindings(CdnDomain parent) {
        bindingService.persistBindings(parent);
    }

    public void forceCleanup(CdnDomain domain) {
        if (domain == null || domain.getId() == null) {
            return;
        }
        List<CdnDomainRouteBinding> bindings = bindingService.listActiveByDomainId(domain.getId());
        deleteDnsRecords(bindings);
        bindingService.deleteByDomainId(domain.getId());
        domain.setTencentDnsId(null);
    }

    @Override
    public CdnDomain configDNS(CdnDomain domain) throws TencentCloudSDKException, BusinessException {
        List<CdnDomainRouteBinding> bindings = requireBindings(domain);
        String subDomain = DomainUtil.convertSubDomain(domain.getDomainName());
        List<CdnDomainRouteBinding> configured = new ArrayList<>();
        try {
            for (CdnDomainRouteBinding binding : bindings) {
                CreateRecordDTO request = new CreateRecordDTO();
                request.setDomain(TencentDns.LOCAL_DOMAIN_NAME)
                        .setSubDomain(subDomain)
                        .setValue(binding.getUpstreamCname());
                CreateRecordResponse response = TencentApi.createRecord(request);
                if (response == null || response.getRecordId() == null) {
                    throw new BusinessException("多 CDN 调度记录创建失败：" + binding.getTargetKey());
                }
                binding.setDnsRecordId(response.getRecordId());
                binding.setUpdateTime(new Date());
                bindingService.save(binding);
                configured.add(binding);
            }
        } catch (Exception e) {
            deleteDnsRecords(configured);
            if (e instanceof TencentCloudSDKException) {
                throw (TencentCloudSDKException) e;
            }
            if (e instanceof BusinessException) {
                throw (BusinessException) e;
            }
            throw new BusinessException(e.getMessage());
        }
        domain.setCname(subDomain + "." + TencentDns.LOCAL_DOMAIN_NAME);
        domain.setTencentDnsId(configured.get(0).getDnsRecordId());
        domain.setDomainStatus("configuring");
        domain.setUpdateTime(new Date());
        return cdnDomainService.save(domain);
    }

    @Override
    public void save(CdnDomain domain, String businessType, String serviceArea) throws BusinessException {
        fanOut(domain, "基础配置", (platform, child) -> platform.save(child, businessType, serviceArea));
    }

    @Override
    public void disable(CdnDomain domain) throws BusinessException {
        fanOut(domain, "停用域名", ICdnPlatformService::disable);
    }

    @Override
    public void enable(CdnDomain domain) throws BusinessException {
        fanOut(domain, "启用域名", ICdnPlatformService::enable);
    }

    @Override
    public void delete(CdnDomain domain) throws BusinessException {
        List<CdnDomainRouteBinding> bindings = requireBindings(domain);
        fanOut(domain, "删除域名", ICdnPlatformService::delete);
        deleteDnsRecords(bindings);
        domain.setTencentDnsId(null);
        bindingService.deleteByDomainId(domain.getId());
    }

    @Override
    public void ipv6(CdnDomain domain, Integer status) throws BusinessException {
        fanOut(domain, "IPv6 配置", (platform, child) -> platform.ipv6(child, status));
    }

    @Override
    public void saveSourceStationConfig(CdnDomain domain, CdnDomainSourcesVo config) throws BusinessException {
        fanOut(domain, "源站配置", (platform, child) -> platform.saveSourceStationConfig(child, config));
    }

    @Override
    public void change(CdnDomain domain) throws BusinessException {
        fanOut(domain, "主备源切换", ICdnPlatformService::change);
    }

    @Override
    public void saveOriginProtocol(CdnDomain domain, DomainOriginSettingVo config) throws BusinessException {
        fanOut(domain, "回源协议", (platform, child) -> platform.saveOriginProtocol(child, config));
    }

    @Override
    public void saveOriginRequestUrlRewrite(CdnDomain domain, DomainOriginSettingVo config) throws BusinessException {
        fanOut(domain, "回源 URL 改写", (platform, child) -> platform.saveOriginRequestUrlRewrite(child, config));
    }

    @Override
    public void saveAdvancedReturnSource(CdnDomain domain, DomainOriginSettingVo config) throws BusinessException {
        fanOut(domain, "高级回源", (platform, child) -> platform.saveAdvancedReturnSource(child, config));
    }

    @Override
    public void saveRangeSwitch(CdnDomain domain, DomainOriginSettingVo config) throws BusinessException {
        fanOut(domain, "Range 回源", (platform, child) -> platform.saveRangeSwitch(child, config));
    }

    @Override
    public void saveRangeVerifyETag(CdnDomain domain, DomainOriginSettingVo config) throws BusinessException {
        fanOut(domain, "ETag 校验", (platform, child) -> platform.saveRangeVerifyETag(child, config));
    }

    @Override
    public void saveOriginHost(CdnDomain domain, DomainOriginSettingVo config) throws BusinessException {
        fanOut(domain, "回源 HOST", (platform, child) -> platform.saveOriginHost(child, config));
    }

    @Override
    public void saveRangeTimeOut(CdnDomain domain, DomainOriginSettingVo config) throws BusinessException {
        fanOut(domain, "回源超时", (platform, child) -> platform.saveRangeTimeOut(child, config));
    }

    @Override
    public void saveOriginFollowRedirect(CdnDomain domain, DomainOriginSettingVo config) throws BusinessException {
        fanOut(domain, "回源跟随重定向", (platform, child) -> platform.saveOriginFollowRedirect(child, config));
    }

    @Override
    public void saveOriginRequestHeader(CdnDomain domain, DomainOriginSettingVo config) throws BusinessException {
        fanOut(domain, "回源请求头", (platform, child) -> platform.saveOriginRequestHeader(child, config));
    }

    @Override
    public void httpsConfiguration(CdnDomain domain, DomainHttpsSettingVo config) throws BusinessException {
        fanOut(domain, "HTTPS 配置", (platform, child) -> platform.httpsConfiguration(child, config));
    }

    @Override
    public void httpsConfigurationOther(CdnDomain domain, DomainHttpsSettingVo config) throws BusinessException {
        fanOut(domain, "HTTPS 扩展配置", (platform, child) -> platform.httpsConfigurationOther(child, config));
    }

    @Override
    public void forcedToJump(CdnDomain domain, DomainHttpsSettingVo config, String redirectCode) throws BusinessException {
        fanOut(domain, "HTTPS 强制跳转", (platform, child) -> platform.forcedToJump(child, config, redirectCode));
    }

    @Override
    public void saveCacheRules(CdnDomain domain, SettingCacheVo config) throws BusinessException {
        fanOut(domain, "缓存规则", (platform, child) -> platform.saveCacheRules(child, config));
    }

    @Override
    public void saveIgnoreQueryString(CdnDomain domain, IgnoreQueryStringDTO config) throws BusinessException {
        fanOut(domain, "过滤参数", (platform, child) -> platform.saveIgnoreQueryString(child, config));
    }

    @Override
    public void saveCacheFollowOriginStatusSwitch(CdnDomain domain, SettingCacheVo config) throws BusinessException {
        fanOut(domain, "遵循源站缓存", (platform, child) -> platform.saveCacheFollowOriginStatusSwitch(child, config));
    }

    @Override
    public void saveErrorCodeCache(CdnDomain domain, SettingCacheVo config) throws BusinessException {
        fanOut(domain, "状态码缓存", (platform, child) -> platform.saveErrorCodeCache(child, config));
    }

    @Override
    public void saveHotlinkPrevention(CdnDomain domain, SettingAccessVo config) throws BusinessException {
        fanOut(domain, "防盗链", (platform, child) -> platform.saveHotlinkPrevention(child, config));
    }

    @Override
    public void saveIpBlackWhiteList(CdnDomain domain, SettingAccessVo config) throws BusinessException {
        fanOut(domain, "IP 黑白名单", (platform, child) -> platform.saveIpBlackWhiteList(child, config));
    }

    @Override
    public void saveUserAgentFilter(CdnDomain domain, SettingAccessVo config) throws BusinessException {
        fanOut(domain, "User-Agent 黑白名单", (platform, child) -> platform.saveUserAgentFilter(child, config));
    }

    @Override
    public void saveUrlAuth(CdnDomain domain, SettingAccessVo config) throws BusinessException {
        fanOut(domain, "URL 鉴权", (platform, child) -> platform.saveUrlAuth(child, config));
    }

    @Override
    public EdgeOneSecurityPolicyVo getEdgeOneSecurityPolicy(CdnDomain domain) throws BusinessException {
        return withFirstRoute(domain, CdnRoute.TENCENT_EDGEONE.getCode(),
                (platform, child) -> platform.getEdgeOneSecurityPolicy(child));
    }

    @Override
    public void saveEdgeOneSecurityPolicy(CdnDomain domain, EdgeOneSecurityPolicyVo config) throws BusinessException {
        fanOutRoute(domain, CdnRoute.TENCENT_EDGEONE.getCode(), "EdgeOne 安全策略",
                (platform, child) -> platform.saveEdgeOneSecurityPolicy(child, config));
    }

    @Override
    public void saveHttpHeader(CdnDomain domain, SettingHigherVo config) throws BusinessException {
        fanOut(domain, "HTTP Header", (platform, child) -> platform.saveHttpHeader(child, config));
    }

    @Override
    public void saveCustomErrorPageConfiguration(CdnDomain domain, SettingHigherVo config) throws BusinessException {
        fanOut(domain, "自定义错误页", (platform, child) -> platform.saveCustomErrorPageConfiguration(child, config));
    }

    @Override
    public void saveCompress(CdnDomain domain, SettingHigherVo config) throws BusinessException {
        fanOut(domain, "智能压缩", (platform, child) -> platform.saveCompress(child, config));
    }

    @Override
    public DomainConfig getDomainConfig(String domainName) throws BusinessException {
        CdnDomain domain = cdnDomainService.queryByDomainName(domainName);
        if (domain == null || !CdnRoute.isMultiCdn(domain.getRoute())) {
            throw new BusinessException("多 CDN 域名不存在");
        }
        List<DomainConfig> configs = new ArrayList<>();
        List<String> statuses = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        for (CdnDomainRouteBinding binding : requireBindings(domain)) {
            CdnDomain child = bindingService.toChildDomain(domain, binding);
            try {
                DomainConfig config = invokeResult(binding, child, (platform, targetDomain) ->
                        platform instanceof SelfHostedDomainServiceImpl
                                ? ((SelfHostedDomainServiceImpl) platform).getDomainConfig(targetDomain)
                                : platform.getDomainConfig(targetDomain.getDomainName()));
                if (config == null || config.getDomainBasicInfo() == null) {
                    failures.add(binding.getTargetKey() + "：未返回域名基础信息");
                    continue;
                }
                DomainBasicInfo basicInfo = config.getDomainBasicInfo();
                configs.add(config);
                statuses.add(basicInfo.getDomainStatus());
                child.setDomainStatus(basicInfo.getDomainStatus());
                if (Assert.notEmpty(basicInfo.getBusinessType())) {
                    child.setBusinessType(basicInfo.getBusinessType());
                }
                if (Assert.notEmpty(basicInfo.getServiceArea())) {
                    child.setServiceArea(basicInfo.getServiceArea());
                }
                binding.setDomainSnapshotJson(JSON.toJSONString(child));
                binding.setUpdateTime(new Date());
                bindingService.save(binding);
            } catch (Exception e) {
                failures.add(binding.getTargetKey() + "：" + safeMessage(e));
            }
        }
        if (configs.isEmpty()) {
            throw new BusinessException("多 CDN 全部上游状态查询失败：" + String.join("；", failures));
        }
        if (!failures.isEmpty()) {
            throw new BusinessException("多 CDN 部分上游状态查询失败：" + String.join("；", failures));
        }
        DomainConfig primaryConfig = configs.get(0);
        DomainBasicInfo primaryBasicInfo = primaryConfig.getDomainBasicInfo();
        primaryBasicInfo.setDomainName(domain.getDomainName());
        primaryBasicInfo.setBusinessType(domain.getBusinessType());
        primaryBasicInfo.setServiceArea(domain.getServiceArea());
        primaryBasicInfo.setCname(domain.getCname());
        primaryBasicInfo.setDomainStatus(aggregateDomainStatus(statuses));
        return primaryConfig;
    }

    String aggregateDomainStatus(List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return DomainStatus.CONFIGURING;
        }
        boolean allOnline = true;
        boolean allOffline = true;
        for (String status : statuses) {
            if (DomainStatus.CONFIGURE_FAILED.equals(status) || DomainStatus.CHECK_FAILED.equals(status)) {
                return DomainStatus.CONFIGURE_FAILED;
            }
            allOnline &= DomainStatus.ONLINE.equals(status);
            allOffline &= DomainStatus.OFFLINE.equals(status);
        }
        if (allOnline) {
            return DomainStatus.ONLINE;
        }
        if (allOffline) {
            return DomainStatus.OFFLINE;
        }
        return DomainStatus.CONFIGURING;
    }

    private void fanOut(CdnDomain domain, String actionName, BindingAction action) throws BusinessException {
        fanOutRoute(domain, null, actionName, action);
    }

    private void fanOutRoute(CdnDomain domain, String requiredRoute, String actionName,
                             BindingAction action) throws BusinessException {
        List<String> failures = new ArrayList<>();
        int matched = 0;
        for (CdnDomainRouteBinding binding : requireBindings(domain)) {
            if (requiredRoute != null && !requiredRoute.equals(binding.getRoute())) {
                continue;
            }
            matched++;
            CdnDomain child = bindingService.toChildDomain(domain, binding);
            try {
                invoke(binding, child, action);
                binding.setDomainSnapshotJson(JSON.toJSONString(child));
                binding.setUpdateTime(new Date());
                bindingService.save(binding);
            } catch (Exception e) {
                failures.add(binding.getTargetKey() + "：" + safeMessage(e));
            }
        }
        // 自建 CDN 的配置以父域名 ID 为键，调用时可能更新同一行；统一恢复父记录的 multi_cdn 路由。
        cdnDomainService.save(domain);
        if (matched == 0) {
            throw new BusinessException("当前多 CDN 线路组不包含可执行" + actionName + "的厂商");
        }
        if (!failures.isEmpty()) {
            throw new BusinessException(actionName + "部分上游失败：" + String.join("；", failures));
        }
    }

    private <T> T withFirstRoute(CdnDomain domain, String route, BindingResult<T> action)
            throws BusinessException {
        for (CdnDomainRouteBinding binding : requireBindings(domain)) {
            if (route.equals(binding.getRoute())) {
                return invokeResult(binding, bindingService.toChildDomain(domain, binding), action);
            }
        }
        throw new BusinessException("当前多 CDN 线路组不包含腾讯云 EdgeOne");
    }

    private void invoke(CdnDomainRouteBinding binding, CdnDomain child, BindingAction action)
            throws BusinessException {
        ICdnPlatformService platform = CdnPlatformFactory.getCdnPlatform(binding.getRoute());
        try {
            action.apply(platform, child);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(safeMessage(e));
        }
    }

    private <T> T invokeResult(CdnDomainRouteBinding binding, CdnDomain child, BindingResult<T> action)
            throws BusinessException {
        ICdnPlatformService platform = CdnPlatformFactory.getCdnPlatform(binding.getRoute());
        return action.apply(platform, child);
    }

    private List<CdnDomainRouteBinding> requireBindings(CdnDomain domain) throws BusinessException {
        List<CdnDomainRouteBinding> bindings = bindingService.listActiveByDomainId(domain.getId());
        if (bindings.isEmpty()) {
            throw new BusinessException("多 CDN 域名缺少上游绑定记录，请联系管理员");
        }
        return bindings;
    }

    private void rollbackCreatedTargets(List<CreatedTarget> targets) {
        for (int i = targets.size() - 1; i >= 0; i--) {
            CreatedTarget target = targets.get(i);
            try {
                ICdnPlatformService platform = CdnPlatformFactory.getCdnPlatform(target.target.getRoute());
                platform.delete(target.child);
            } catch (Exception e) {
                log.error("回滚多 CDN 上游域名失败，域名={}，目标={}，原因={}",
                        target.child.getDomainName(), target.target.getTargetKey(), e.getMessage());
            }
            if (target.child.getId() != null) {
                cdnDomainService.deleteById(target.child.getId());
            }
        }
    }

    private void deleteDnsRecords(List<CdnDomainRouteBinding> bindings) {
        for (CdnDomainRouteBinding binding : bindings) {
            if (binding.getDnsRecordId() == null) {
                continue;
            }
            try {
                DeleteRecordDTO request = new DeleteRecordDTO();
                request.setDomain(TencentDns.LOCAL_DOMAIN_NAME);
                request.setRecordId(binding.getDnsRecordId());
                TencentApi.deleteRecord(request);
                binding.setDnsRecordId(null);
                bindingService.save(binding);
            } catch (Exception e) {
                log.warn("删除多 CDN 调度记录失败，记录ID={}，原因={}",
                        binding.getDnsRecordId(), e.getMessage());
            }
        }
    }

    private String extractUpstreamCname(CdnDomain domain, String route) throws BusinessException {
        if (CdnRoute.HUAWEI.getCode().equals(route)) return domain.getCnameHuawei();
        if (CdnRoute.VOLCENGINE.getCode().equals(route)) return domain.getCnameVolcengine();
        if (CdnRoute.YIFAN.getCode().equals(route) || CdnRoute.BAISHAN.getCode().equals(route)) return domain.getCnameYifan();
        if (CdnRoute.TENCENT.getCode().equals(route) || CdnRoute.TENCENT_EDGEONE.getCode().equals(route)) return domain.getCnameTencent();
        if (CdnRoute.CDNETWORKS.getCode().equals(route)) return domain.getCnameCdnetworks();
        if (CdnRoute.ALIYUN.getCode().equals(route)) return domain.getCnameAliyun();
        if (CdnRoute.BAIDU.getCode().equals(route)) return domain.getCnameBaidu();
        if (CdnRoute.KINGSOFT.getCode().equals(route)) return domain.getCnameKingsoft();
        if (CdnRoute.isSelfHosted(route)) return selfHostedUpstreamCname(domain);
        return domain.getCname();
    }

    private String selfHostedUpstreamCname(CdnDomain domain) throws BusinessException {
        SelfHostedDomainConfig config = selfHostedCdnService.getDomainConfig(domain.getId());
        for (SelfHostedNodeGroup group : selfHostedCdnService.listGroups()) {
            if (group.getId().equals(config.getNodeGroupId())) {
                return selfHostedCdnService.groupCname(group);
            }
        }
        throw new BusinessException("自建 CDN 节点组不存在");
    }

    private void mergeVendorCname(CdnDomain parent, CdnDomain child) {
        if (Assert.notEmpty(child.getCnameHuawei())) parent.setCnameHuawei(child.getCnameHuawei());
        if (Assert.notEmpty(child.getCnameVolcengine())) parent.setCnameVolcengine(child.getCnameVolcengine());
        if (Assert.notEmpty(child.getCnameYifan())) parent.setCnameYifan(child.getCnameYifan());
        if (Assert.notEmpty(child.getCnameTencent())) parent.setCnameTencent(child.getCnameTencent());
        if (Assert.notEmpty(child.getCnameCdnetworks())) parent.setCnameCdnetworks(child.getCnameCdnetworks());
        if (Assert.notEmpty(child.getCnameAliyun())) parent.setCnameAliyun(child.getCnameAliyun());
        if (Assert.notEmpty(child.getCnameBaidu())) parent.setCnameBaidu(child.getCnameBaidu());
        if (Assert.notEmpty(child.getCnameWangsu())) parent.setCnameWangsu(child.getCnameWangsu());
        if (Assert.notEmpty(child.getCnameKingsoft())) parent.setCnameKingsoft(child.getCnameKingsoft());
    }

    private String safeMessage(Exception e) {
        return Assert.isEmpty(e.getMessage()) ? e.getClass().getSimpleName() : e.getMessage();
    }

    private static final class CreatedTarget {
        private final AreaRouteTargetVo target;
        private final CdnDomain child;
        private String upstreamCname;

        private CreatedTarget(AreaRouteTargetVo target, CdnDomain child, String upstreamCname) {
            this.target = target;
            this.child = child;
            this.upstreamCname = upstreamCname;
        }
    }

    @FunctionalInterface
    private interface BindingAction {
        void apply(ICdnPlatformService platform, CdnDomain child) throws Exception;
    }

    @FunctionalInterface
    private interface BindingResult<T> {
        T apply(ICdnPlatformService platform, CdnDomain child) throws BusinessException;
    }
}
