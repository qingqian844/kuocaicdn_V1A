package com.kuocai.cdn.service.domain.operation;

import com.alibaba.fastjson.JSON;
import com.kuocai.cdn.api.*;
import com.kuocai.cdn.api.huawei.cdn.dto.HttpPutBodyDTO;
import com.kuocai.cdn.api.tencent.dns.CreateRecordResponse;
import com.kuocai.cdn.api.tencent.dns.TencentApi;
import com.kuocai.cdn.api.tencent.dns.dto.CreateRecordDTO;
import com.kuocai.cdn.api.tencent.dns.properties.TencentDns;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.entity.CdnDomainSources;
import com.kuocai.cdn.entity.SelfHostedDomainConfig;
import com.kuocai.cdn.entity.SelfHostedNodeGroup;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.enumeration.domainmerage.CdnRoute;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.SelfHostedCdnService;
import com.kuocai.cdn.service.SysUserService;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.DomainUtil;
import com.kuocai.cdn.vo.*;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
public class SelfHostedDomainServiceImpl extends BaseService<CdnDomain> implements ICdnPlatformService {
    private static final long DOMAIN_APPLY_TIMEOUT_MS = 120_000L;

    private final SelfHostedCdnService selfHostedCdnService;
    private final SysUserService sysUserService;

    public SelfHostedDomainServiceImpl(SelfHostedCdnService selfHostedCdnService,
                                       SysUserService sysUserService) {
        this.selfHostedCdnService = selfHostedCdnService;
        this.sysUserService = sysUserService;
    }

    @Override
    public CdnDomain create(Long userId, String domainName, String businessType, String serviceArea,
                            String originType, String ipOrDomain) throws BusinessException {
        return create(userId, domainName, businessType, serviceArea, originType, ipOrDomain,
                "http", 80, 443, domainName, 100);
    }

    @Override
    public CdnDomain create(Long userId, String domainName, String businessType, String serviceArea,
                            String originType, String ipOrDomain, String originProtocol, Integer httpPort,
                            Integer httpsPort, String originHost, Integer originWeight) throws BusinessException {
        if (Assert.isEmpty(ipOrDomain)) {
            throw new BusinessException("源站地址不能为空");
        }
        SysUser owner = sysUserService.queryById(userId);
        String userRoute = owner == null ? null : owner.getRoute();
        String targetRoute = CdnRoute.isSelfHosted(userRoute)
                ? userRoute : CdnRoute.selfHostedRouteForServiceArea(serviceArea);
        String fixedServiceArea = CdnRoute.selfHostedServiceArea(targetRoute);
        if (fixedServiceArea != null) {
            serviceArea = fixedServiceArea;
        }
        CdnDomain failedDomain = queryByObj(CdnDomain.builder().domainName(domainName).build()).stream()
                .filter(item -> CdnRoute.isSelfHosted(item.getRoute()))
                .filter(item -> userId.equals(item.getUserId()) && "configure_failed".equals(item.getDomainStatus()))
                .findFirst().orElse(null);
        if (failedDomain != null) {
            SelfHostedDomainConfig config = selfHostedCdnService.getDomainConfig(failedDomain.getId());
            config.setOriginType(originType);
            config.setOriginAddress(ipOrDomain);
            config.setOriginProtocol(normalizeOriginProtocol(originProtocol));
            config.setHttpPort(httpPort == null ? 80 : httpPort);
            config.setHttpsPort(httpsPort == null ? 443 : httpsPort);
            config.setOriginHost(Assert.isEmpty(originHost) ? domainName : originHost.trim());
            config.setStatus("enabled");
            selfHostedCdnService.updateDomainConfig(config);
            failedDomain.setBusinessType(businessType);
            String existingServiceArea = CdnRoute.selfHostedServiceArea(failedDomain.getRoute());
            failedDomain.setServiceArea(existingServiceArea == null ? serviceArea : existingServiceArea);
            failedDomain.setDomainStatus("configuring");
            failedDomain.setUpdateTime(new Date());
            return save(failedDomain);
        }
        CdnDomain domain = CdnDomain.builder()
                .userId(userId).domainId("self-" + System.currentTimeMillis()).domainName(domainName)
                .businessType(businessType).serviceArea(serviceArea).domainStatus("configuring")
                .route(targetRoute).createTime(new Date()).updateTime(new Date()).build();
        domain = save(domain);
        try {
            selfHostedCdnService.createDomainConfig(domain, originType, ipOrDomain, originProtocol,
                    httpPort, httpsPort, originHost);
            return domain;
        } catch (BusinessException e) {
            deleteById(domain.getId());
            throw e;
        }
    }

    @Override
    public CdnDomain configDNS(CdnDomain cdnDomain) throws TencentCloudSDKException, BusinessException {
        try {
            SelfHostedDomainConfig config = selfHostedCdnService.getDomainConfig(cdnDomain.getId());
            SelfHostedNodeGroup group = null;
            for (SelfHostedNodeGroup candidate : selfHostedCdnService.listGroups()) {
                if (candidate.getId().equals(config.getNodeGroupId())) {
                    group = candidate;
                    break;
                }
            }
            if (group == null) {
                throw new BusinessException("自建 CDN 节点组不存在");
            }
            selfHostedCdnService.requireActiveNode(group.getId());
            selfHostedCdnService.syncGroupDns(group.getId());
            if (!selfHostedCdnService.waitForDomainConfigurationApplied(
                    cdnDomain.getId(), DOMAIN_APPLY_TIMEOUT_MS)) {
                throw new BusinessException("节点配置下发超时，请检查节点状态和最近错误后重试");
            }
            if (cdnDomain.getTencentDnsId() == null || Assert.isEmpty(cdnDomain.getCname())) {
                CreateRecordDTO dto = buildCustomerCnameRecord(cdnDomain.getDomainName(),
                        TencentDns.LOCAL_DOMAIN_NAME, selfHostedCdnService.groupCname(group));
                CreateRecordResponse response = TencentApi.createRecord(dto);
                if (response == null || response.getRecordId() == null) {
                    throw new BusinessException("自建 CDN CNAME 创建失败");
                }
                cdnDomain.setTencentDnsId(response.getRecordId());
                cdnDomain.setCname(dto.getSubDomain() + "." + TencentDns.LOCAL_DOMAIN_NAME);
            }
            cdnDomain.setDomainStatus("online");
            cdnDomain.setUpdateTime(new Date());
            return save(cdnDomain);
        } catch (TencentCloudSDKException | BusinessException e) {
            markConfigureFailed(cdnDomain);
            throw e;
        } catch (RuntimeException e) {
            markConfigureFailed(cdnDomain);
            throw e;
        }
    }

    @Override
    public void save(CdnDomain cdnDomain, String businessType, String serviceArea) {
        cdnDomain.setBusinessType(businessType);
        cdnDomain.setServiceArea(serviceArea);
        cdnDomain.setUpdateTime(new Date());
        save(cdnDomain);
    }

    @Override
    public void disable(CdnDomain cdnDomain) throws BusinessException {
        cdnDomain.setDomainStatus("offline");
        cdnDomain.setUpdateTime(new Date());
        save(cdnDomain);
        SelfHostedDomainConfig config = selfHostedCdnService.getDomainConfig(cdnDomain.getId());
        config.setStatus("disabled");
        selfHostedCdnService.updateDomainConfig(config);
    }

    @Override
    public void enable(CdnDomain cdnDomain) throws BusinessException {
        cdnDomain.setDomainStatus("online");
        cdnDomain.setUpdateTime(new Date());
        save(cdnDomain);
        SelfHostedDomainConfig config = selfHostedCdnService.getDomainConfig(cdnDomain.getId());
        config.setStatus("enabled");
        selfHostedCdnService.updateDomainConfig(config);
    }

    @Override public void delete(CdnDomain domain) { selfHostedCdnService.deleteDomainConfig(domain.getId()); }
    @Override public void ipv6(CdnDomain d, Integer s) throws BusinessException { unsupported(" IPv6 调度"); }

    @Override
    public void saveSourceStationConfig(CdnDomain cdnDomain, CdnDomainSourcesVo sourceVo) throws BusinessException {
        if (sourceVo == null || sourceVo.getMain() == null) {
            throw new BusinessException("主源站不能为空");
        }
        CdnDomainSources source = sourceVo.getMain();
        SelfHostedDomainConfig config = selfHostedCdnService.getDomainConfig(cdnDomain.getId());
        config.setOriginType(source.getOriginType());
        config.setOriginAddress(source.getIpOrDomain());
        config.setHttpPort(source.getHttpPort() == null ? 80 : source.getHttpPort());
        config.setHttpsPort(source.getHttpsPort() == null ? 443 : source.getHttpsPort());
        config.setOriginHost(Assert.isEmpty(source.getHostName()) ? cdnDomain.getDomainName() : source.getHostName());
        if (!Assert.isEmpty(sourceVo.getOriginProtocol())) {
            config.setOriginProtocol(sourceVo.getOriginProtocol());
        }
        selfHostedCdnService.updateDomainConfig(config);
    }

    @Override public void change(CdnDomain cdnDomain) { }

    @Override
    public void saveOriginProtocol(CdnDomain cdnDomain, DomainOriginSettingVo setting) throws BusinessException {
        SelfHostedDomainConfig config = selfHostedCdnService.getDomainConfig(cdnDomain.getId());
        config.setOriginProtocol(setting.getOriginProtocol());
        if (setting.getHttpPort() != null) config.setHttpPort(setting.getHttpPort());
        if (setting.getHttpsPort() != null) config.setHttpsPort(setting.getHttpsPort());
        selfHostedCdnService.updateDomainConfig(config);
    }

    @Override public void saveOriginRequestUrlRewrite(CdnDomain d, DomainOriginSettingVo v) throws BusinessException { unsupported("回源 URL 改写"); }
    @Override public void saveAdvancedReturnSource(CdnDomain d, DomainOriginSettingVo v) throws BusinessException { unsupported("高级回源"); }
    @Override public void saveRangeSwitch(CdnDomain d, DomainOriginSettingVo v) throws BusinessException { unsupported("Range 回源开关"); }
    @Override public void saveRangeVerifyETag(CdnDomain d, DomainOriginSettingVo v) throws BusinessException { unsupported("ETag 校验"); }
    @Override public void saveOriginHost(CdnDomain d, DomainOriginSettingVo v) throws BusinessException { unsupported("独立回源 Host 设置"); }
    @Override public void saveRangeTimeOut(CdnDomain d, DomainOriginSettingVo v) throws BusinessException { unsupported("回源超时设置"); }
    @Override public void saveOriginRequestHeader(CdnDomain d, DomainOriginSettingVo v) throws BusinessException { unsupported("回源请求头"); }

    @Override
    public void httpsConfiguration(CdnDomain cdnDomain, DomainHttpsSettingVo setting) throws BusinessException {
        HttpPutBodyDTO https = setting == null ? null : setting.getHttps();
        boolean enabled = https != null && "on".equals(https.getHttps_status());
        SelfHostedDomainConfig config = selfHostedCdnService.getDomainConfig(cdnDomain.getId());
        String redirect = setting != null && setting.getForceRedirect() != null
                ? setting.getForceRedirect().getStatus() : config.getForceRedirect();
        selfHostedCdnService.saveCertificate(config, enabled,
                https == null ? null : https.getCertificate_value(),
                https == null ? null : https.getPrivate_key(), redirect);
    }

    @Override public void httpsConfigurationOther(CdnDomain d, DomainHttpsSettingVo v) { }
    @Override public void forcedToJump(CdnDomain d, DomainHttpsSettingVo v, String c) throws BusinessException {
        SelfHostedDomainConfig config = selfHostedCdnService.getDomainConfig(d.getId());
        config.setForceRedirect(v != null && v.getForceRedirect() != null ? v.getForceRedirect().getStatus() : "off");
        selfHostedCdnService.updateDomainConfig(config);
    }

    @Override public void saveCacheRules(CdnDomain d, SettingCacheVo v) throws BusinessException {
        SelfHostedDomainConfig config = selfHostedCdnService.getDomainConfig(d.getId());
        config.setCacheConfigJson(JSON.toJSONString(v));
        selfHostedCdnService.updateDomainConfig(config);
    }
    @Override public void saveIgnoreQueryString(CdnDomain d, IgnoreQueryStringDTO v) throws BusinessException { unsupported("缓存参数过滤"); }
    @Override public void saveCacheFollowOriginStatusSwitch(CdnDomain d, SettingCacheVo v) throws BusinessException { saveCacheRules(d, v); }
    @Override public void saveErrorCodeCache(CdnDomain d, SettingCacheVo v) throws BusinessException { saveCacheRules(d, v); }
    @Override public void saveHotlinkPrevention(CdnDomain d, SettingAccessVo v) throws BusinessException { unsupported("防盗链"); }
    @Override public void saveIpBlackWhiteList(CdnDomain d, SettingAccessVo v) throws BusinessException { unsupported("IP 黑白名单"); }
    @Override public void saveUserAgentFilter(CdnDomain d, SettingAccessVo v) throws BusinessException { unsupported("User-Agent 黑白名单"); }
    @Override public void saveUrlAuth(CdnDomain d, SettingAccessVo v) throws BusinessException { unsupported("URL 鉴权"); }
    @Override public void saveHttpHeader(CdnDomain d, SettingHigherVo v) throws BusinessException { unsupported("HTTP 响应头"); }
    @Override public void saveCustomErrorPageConfiguration(CdnDomain d, SettingHigherVo v) throws BusinessException { unsupported("自定义错误页"); }
    @Override public void saveCompress(CdnDomain d, SettingHigherVo v) throws BusinessException { unsupported("智能压缩配置"); }

    @Override
    public DomainConfig getDomainConfig(String domainName) throws BusinessException {
        SelfHostedDomainConfig config = selfHostedCdnService.getDomainConfigByName(domainName);
        CdnDomain domain = queryByObj(CdnDomain.builder().domainName(domainName).build()).stream()
                .filter(item -> CdnRoute.isSelfHosted(item.getRoute()))
                .findFirst().orElseThrow(() -> new BusinessException("自建 CDN 域名不存在"));
        DomainBasicInfo.SourceStationPrimaryInfo primary = DomainBasicInfo.SourceStationPrimaryInfo.builder()
                .sourceStationType(config.getOriginType()).ipOrDomain(config.getOriginAddress())
                .httpPort(String.valueOf(config.getHttpPort())).httpsPort(String.valueOf(config.getHttpsPort()))
                .sourceHost(config.getOriginHost()).build();
        DomainBasicInfo.SourceStationStandbyInfo standby = DomainBasicInfo.SourceStationStandbyInfo.builder()
                .sourceStationType("ipaddr").ipOrDomain("").httpPort("80").httpsPort("443").sourceHost("").build();
        String domainStatus = domain.getDomainStatus();
        if ("configuring".equals(domainStatus) && Assert.notEmpty(domain.getCname())
                && selfHostedCdnService.isDomainConfigurationApplied(domain.getId())) {
            domainStatus = "online";
        }
        DomainBasicInfo basic = DomainBasicInfo.builder().domainName(domainName).domainStatus(domainStatus)
                .httpsStatus(config.getHttpsEnabled() != null && config.getHttpsEnabled() == 1 ? "1" : "0")
                .cname(domain.getCname()).businessType(domain.getBusinessType()).serviceArea(domain.getServiceArea())
                .isIpv6("0").createTime(domain.getCreateTime()).updateTime(domain.getUpdateTime())
                .sourceStationPrimaryInfo(primary).sourceStationStandbyInfo(standby).build();
        DomainBackSourceInfo back = DomainBackSourceInfo.builder().origin_protocol(config.getOriginProtocol())
                .port("https".equals(config.getOriginProtocol()) ? config.getHttpsPort() : config.getHttpPort())
                .origin_receive_timeout("30").origin_range_status("on").slice_etag_status("off")
                .upstream_follow_redirect_status("off").upstream_follow_redirect_max_times(0)
                .origin_request_url_rewrite(Collections.emptyList()).flexible_origin(Collections.emptyList())
                .origin_request_header(Collections.emptyList()).build();
        DomainHttpsInfo https = DomainHttpsInfo.builder()
                .https(DomainHttpsInfo.HttpGetBody.builder().https_status(config.getHttpsEnabled() != null && config.getHttpsEnabled() == 1 ? "on" : "off")
                        .certificate_name("self-hosted").certificate_value("").certificate_source(1)
                        .http2_status("on").tls_version("TLSv1.2,TLSv1.3").ocsp_stapling_status("off").build())
                .force_redirect(DomainHttpsInfo.ForceRedirect.builder().status(config.getForceRedirect())
                        .type("https").redirect_code("301").redirectType("https").redirectCode(301).build()).build();
        List<DomainCacheInfo.CacheRule> cacheRules = Collections.emptyList();
        List<DomainCacheInfo.ErrorCodeCache> errorRules = Collections.emptyList();
        try {
            com.alibaba.fastjson.JSONObject cacheJson = JSON.parseObject(config.getCacheConfigJson());
            if (cacheJson != null && cacheJson.get("cacheRules") != null) {
                cacheRules = JSON.parseArray(JSON.toJSONString(cacheJson.get("cacheRules")), DomainCacheInfo.CacheRule.class);
            }
            if (cacheJson != null && cacheJson.get("errorCodeCache") != null) {
                errorRules = JSON.parseArray(JSON.toJSONString(cacheJson.get("errorCodeCache")), DomainCacheInfo.ErrorCodeCache.class);
            }
        } catch (Exception ignored) {
            // Keep the management page available even if a historical cache JSON is malformed.
        }
        return DomainConfig.builder().domainBasicInfo(basic).domainBackSourceInfo(back).domainHttpsInfo(https)
                .domainCacheInfo(DomainCacheInfo.builder().cache_rules(cacheRules).error_code_cache(errorRules).build())
                .domainVisitInfo(DomainVisitInfo.builder()
                        .referer(DomainVisitInfo.Referer.builder().type("off").include_empty(true).build())
                        .ip_filter(DomainVisitInfo.IpFilter.builder().type("off").value("").build())
                        .user_agent_filter(DomainVisitInfo.UserAgentFilter.builder().type("off").ua_list(Collections.emptyList()).build())
                        .url_auth(DomainVisitInfo.UrlAuth.builder().status("off").build()).build())
                .domainAdvancedInfo(DomainAdvancedInfo.builder().http_response_header(Collections.emptyList())
                        .error_code_redirect_rules(Collections.emptyList()).error_pages(Collections.emptyList())
                        .compress(DomainAdvancedInfo.Compress.builder().status("on").type("gzip").file_type(".js,.html,.css,.json").build()).build())
                .build();
    }

    private void unsupported(String feature) throws BusinessException {
        throw new BusinessException("自建 CDN 首版暂不支持" + feature);
    }

    static CreateRecordDTO buildCustomerCnameRecord(String domainName, String localDomainName,
                                                      String groupCname) {
        return new CreateRecordDTO().setDomain(localDomainName)
                .setSubDomain(DomainUtil.convertSubDomain(domainName))
                .setRecordType("CNAME").setRecordLine("默认").setValue(groupCname)
                .setTTL(SelfHostedCdnService.DNS_RECORD_TTL_SECONDS);
    }

    private String normalizeOriginProtocol(String protocol) {
        if ("https".equalsIgnoreCase(protocol) || "follow".equalsIgnoreCase(protocol)) {
            return protocol.toLowerCase();
        }
        return "http";
    }

    private void markConfigureFailed(CdnDomain domain) {
        try {
            domain.setDomainStatus("configure_failed");
            domain.setUpdateTime(new Date());
            save(domain);
        } catch (Exception ignored) {
            // Preserve the original DNS/configuration exception for the caller.
        }
    }
}
