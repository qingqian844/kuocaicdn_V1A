package com.kuocai.cdn.service.domain.operation;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.api.*;
import com.kuocai.cdn.api.huawei.cdn.dto.*;
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
import java.util.ArrayList;

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

    @Override
    public void ipv6(CdnDomain domain, Integer status) throws BusinessException {
        SelfHostedDomainConfig config = selfHostedCdnService.getDomainConfig(domain.getId());
        config.setIpv6Enabled(status != null && status == 1 ? 1 : 0);
        selfHostedCdnService.updateDomainConfig(config);
    }

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
            config.setOriginProtocol(normalizeOriginProtocol(sourceVo.getOriginProtocol()));
        }
        JSONObject origin = parseObject(config.getOriginConfigJson());
        if (sourceVo.getBack() == null || Assert.isEmpty(sourceVo.getBack().getIpOrDomain())) {
            origin.remove("standby");
        } else {
            origin.put("standby", sourceVo.getBack());
        }
        config.setOriginConfigJson(origin.toJSONString());
        selfHostedCdnService.updateDomainConfig(config);
    }

    @Override
    public void change(CdnDomain cdnDomain) throws BusinessException {
        SelfHostedDomainConfig config = selfHostedCdnService.getDomainConfig(cdnDomain.getId());
        JSONObject origin = parseObject(config.getOriginConfigJson());
        CdnDomainSources standby = toBean(origin.get("standby"), CdnDomainSources.class);
        if (standby == null || Assert.isEmpty(standby.getIpOrDomain())) {
            throw new BusinessException("未配置备用源站，无法切换");
        }
        CdnDomainSources oldMain = CdnDomainSources.builder()
                .originType(config.getOriginType()).ipOrDomain(config.getOriginAddress())
                .hostName(config.getOriginHost()).httpPort(config.getHttpPort())
                .httpsPort(config.getHttpsPort()).activeStandby(0).build();
        config.setOriginType(standby.getOriginType());
        config.setOriginAddress(standby.getIpOrDomain());
        config.setOriginHost(Assert.isEmpty(standby.getHostName()) ? cdnDomain.getDomainName() : standby.getHostName());
        config.setHttpPort(standby.getHttpPort() == null ? 80 : standby.getHttpPort());
        config.setHttpsPort(standby.getHttpsPort() == null ? 443 : standby.getHttpsPort());
        origin.put("standby", oldMain);
        config.setOriginConfigJson(origin.toJSONString());
        selfHostedCdnService.updateDomainConfig(config);
    }

    @Override
    public void saveOriginProtocol(CdnDomain cdnDomain, DomainOriginSettingVo setting) throws BusinessException {
        SelfHostedDomainConfig config = selfHostedCdnService.getDomainConfig(cdnDomain.getId());
        config.setOriginProtocol(normalizeOriginProtocol(setting.getOriginProtocol()));
        if (setting.getHttpPort() != null) config.setHttpPort(setting.getHttpPort());
        if (setting.getHttpsPort() != null) config.setHttpsPort(setting.getHttpsPort());
        selfHostedCdnService.updateDomainConfig(config);
    }

    @Override public void saveOriginRequestUrlRewrite(CdnDomain d, DomainOriginSettingVo v) throws BusinessException {
        saveOriginValue(d, "originRequestUrlRewrite", v == null ? null : v.getOriginRequestUrlRewriteDTOS());
    }

    @Override public void saveAdvancedReturnSource(CdnDomain d, DomainOriginSettingVo v) throws BusinessException {
        saveOriginValue(d, "flexibleOrigins", v == null ? null : v.getFlexibleOrigins());
    }

    @Override public void saveRangeSwitch(CdnDomain d, DomainOriginSettingVo v) throws BusinessException {
        saveOriginValue(d, "rangeStatus", normalizeSwitch(v == null ? null : v.getStatus()));
    }

    @Override public void saveRangeVerifyETag(CdnDomain d, DomainOriginSettingVo v) throws BusinessException {
        saveOriginValue(d, "etagStatus", normalizeSwitch(v == null ? null : v.getStatus()));
    }

    @Override public void saveOriginHost(CdnDomain d, DomainOriginSettingVo v) throws BusinessException {
        SelfHostedDomainConfig config = selfHostedCdnService.getDomainConfig(d.getId());
        if (Assert.isEmpty(config.getOriginHost())) {
            config.setOriginHost(d.getDomainName());
        }
        selfHostedCdnService.updateDomainConfig(config);
    }

    @Override public void saveRangeTimeOut(CdnDomain d, DomainOriginSettingVo v) throws BusinessException {
        int timeout = v == null || v.getOriginReceiveTimeOut() == null ? 30 : v.getOriginReceiveTimeOut();
        if (timeout < 1 || timeout > 300) {
            throw new BusinessException("回源超时时间必须在 1-300 秒之间");
        }
        saveOriginValue(d, "originReceiveTimeout", timeout);
    }

    @Override public void saveOriginFollowRedirect(CdnDomain d, DomainOriginSettingVo v) throws BusinessException {
        SelfHostedDomainConfig config = selfHostedCdnService.getDomainConfig(d.getId());
        JSONObject origin = parseObject(config.getOriginConfigJson());
        origin.put("followRedirectStatus", normalizeSwitch(v == null ? null : v.getStatus()));
        int maxTimes = v == null || v.getMaxTimes() == null ? 1 : v.getMaxTimes();
        origin.put("followRedirectMaxTimes", Math.max(1, Math.min(maxTimes, 5)));
        config.setOriginConfigJson(origin.toJSONString());
        selfHostedCdnService.updateDomainConfig(config);
    }

    @Override public void saveOriginRequestHeader(CdnDomain d, DomainOriginSettingVo v) throws BusinessException {
        saveOriginValue(d, "originRequestHeader", v == null ? null : v.getOriginRequestHeader());
    }

    @Override
    public void httpsConfiguration(CdnDomain cdnDomain, DomainHttpsSettingVo setting) throws BusinessException {
        HttpPutBodyDTO https = setting == null ? null : setting.getHttps();
        boolean enabled = https != null && "on".equals(https.getHttps_status());
        SelfHostedDomainConfig config = selfHostedCdnService.getDomainConfig(cdnDomain.getId());
        mergeHttpsMetadata(config, https);
        String redirect = setting != null && setting.getForceRedirect() != null
                ? setting.getForceRedirect().getStatus() : config.getForceRedirect();
        selfHostedCdnService.saveCertificate(config, enabled,
                https == null ? null : https.getCertificate_value(),
                https == null ? null : https.getPrivate_key(), redirect);
    }

    @Override public void httpsConfigurationOther(CdnDomain d, DomainHttpsSettingVo v) throws BusinessException {
        SelfHostedDomainConfig config = selfHostedCdnService.getDomainConfig(d.getId());
        mergeHttpsMetadata(config, v == null ? null : v.getHttps());
        selfHostedCdnService.updateDomainConfig(config);
    }
    @Override public void forcedToJump(CdnDomain d, DomainHttpsSettingVo v, String c) throws BusinessException {
        SelfHostedDomainConfig config = selfHostedCdnService.getDomainConfig(d.getId());
        config.setForceRedirect(v != null && v.getForceRedirect() != null ? v.getForceRedirect().getStatus() : "off");
        JSONObject https = parseObject(config.getHttpsConfigJson());
        https.put("redirectCode", Assert.isEmpty(c) ? "301" : c);
        config.setHttpsConfigJson(https.toJSONString());
        selfHostedCdnService.updateDomainConfig(config);
    }

    @Override public void saveCacheRules(CdnDomain d, SettingCacheVo v) throws BusinessException {
        SelfHostedDomainConfig config = selfHostedCdnService.getDomainConfig(d.getId());
        JSONObject cache = parseObject(config.getCacheConfigJson());
        if (v != null && v.getCacheRules() != null) cache.put("cacheRules", v.getCacheRules());
        if (v != null && v.getErrorCodeCache() != null) cache.put("errorCodeCache", v.getErrorCodeCache());
        if (v != null && v.getCacheFollowOriginStatus() != null) cache.put("cacheFollowOriginStatus", v.getCacheFollowOriginStatus());
        config.setCacheConfigJson(cache.toJSONString());
        selfHostedCdnService.updateDomainConfig(config);
    }

    @Override public void saveIgnoreQueryString(CdnDomain d, IgnoreQueryStringDTO v) throws BusinessException {
        SelfHostedDomainConfig config = selfHostedCdnService.getDomainConfig(d.getId());
        JSONObject cache = parseObject(config.getCacheConfigJson());
        cache.put("ignoreQueryString", v == null ? new IgnoreQueryStringDTO() : v);
        config.setCacheConfigJson(cache.toJSONString());
        selfHostedCdnService.updateDomainConfig(config);
    }

    @Override public void saveCacheFollowOriginStatusSwitch(CdnDomain d, SettingCacheVo v) throws BusinessException {
        SelfHostedDomainConfig config = selfHostedCdnService.getDomainConfig(d.getId());
        JSONObject cache = parseObject(config.getCacheConfigJson());
        cache.put("cacheFollowOriginStatus", v == null ? "off" : normalizeSwitch(v.getCacheFollowOriginStatus()));
        config.setCacheConfigJson(cache.toJSONString());
        selfHostedCdnService.updateDomainConfig(config);
    }

    @Override public void saveErrorCodeCache(CdnDomain d, SettingCacheVo v) throws BusinessException {
        SelfHostedDomainConfig config = selfHostedCdnService.getDomainConfig(d.getId());
        JSONObject cache = parseObject(config.getCacheConfigJson());
        cache.put("errorCodeCache", v == null || v.getErrorCodeCache() == null ? new JSONArray() : v.getErrorCodeCache());
        config.setCacheConfigJson(cache.toJSONString());
        selfHostedCdnService.updateDomainConfig(config);
    }

    @Override public void saveHotlinkPrevention(CdnDomain d, SettingAccessVo v) throws BusinessException {
        saveAccessValue(d, "referer", v == null ? null : v.getReferer());
    }

    @Override public void saveIpBlackWhiteList(CdnDomain d, SettingAccessVo v) throws BusinessException {
        SelfHostedDomainConfig config = selfHostedCdnService.getDomainConfig(d.getId());
        JSONObject access = selfHostedCdnService.readAccessConfig(config);
        access.put("ipType", v == null || v.getType() == null ? 0 : v.getType());
        access.put("ips", v == null || v.getIps() == null ? new JSONArray() : v.getIps());
        selfHostedCdnService.writeAccessConfig(config, access);
    }

    @Override public void saveUserAgentFilter(CdnDomain d, SettingAccessVo v) throws BusinessException {
        saveAccessValue(d, "userAgent", v == null ? null : v.getUserAgentBlackAndWhiteListDTO());
    }

    @Override public void saveUrlAuth(CdnDomain d, SettingAccessVo v) throws BusinessException {
        saveAccessValue(d, "urlAuth", v == null ? null : v.getUrlAuth());
    }

    @Override public void saveHttpHeader(CdnDomain d, SettingHigherVo v) throws BusinessException {
        saveAdvancedValue(d, "httpResponseHeaders", v == null ? null : v.getHttpResponseHeaders());
    }

    @Override public void saveCustomErrorPageConfiguration(CdnDomain d, SettingHigherVo v) throws BusinessException {
        SelfHostedDomainConfig config = selfHostedCdnService.getDomainConfig(d.getId());
        JSONObject advanced = parseObject(config.getAdvancedConfigJson());
        advanced.put("errorCodeRedirectRules", v == null || v.getErrorCodeRedirectRules() == null
                ? new JSONArray() : v.getErrorCodeRedirectRules());
        advanced.put("errorPages", v == null || v.getErrorPages() == null ? new JSONArray() : v.getErrorPages());
        config.setAdvancedConfigJson(advanced.toJSONString());
        selfHostedCdnService.updateDomainConfig(config);
    }

    @Override public void saveCompress(CdnDomain d, SettingHigherVo v) throws BusinessException {
        saveAdvancedValue(d, "compress", v == null ? null : v.getCompress());
    }

    @Override
    public DomainConfig getDomainConfig(String domainName) throws BusinessException {
        SelfHostedDomainConfig config = selfHostedCdnService.getDomainConfigByName(domainName);
        JSONObject originJson = parseObject(config.getOriginConfigJson());
        JSONObject cacheJson = parseObject(config.getCacheConfigJson());
        JSONObject accessJson = selfHostedCdnService.readAccessConfig(config);
        JSONObject advancedJson = parseObject(config.getAdvancedConfigJson());
        JSONObject httpsJson = parseObject(config.getHttpsConfigJson());
        CdnDomain domain = queryByObj(CdnDomain.builder().domainName(domainName).build()).stream()
                .filter(item -> CdnRoute.isSelfHosted(item.getRoute()))
                .findFirst().orElseThrow(() -> new BusinessException("自建 CDN 域名不存在"));
        DomainBasicInfo.SourceStationPrimaryInfo primary = DomainBasicInfo.SourceStationPrimaryInfo.builder()
                .sourceStationType(config.getOriginType()).ipOrDomain(config.getOriginAddress())
                .httpPort(String.valueOf(config.getHttpPort())).httpsPort(String.valueOf(config.getHttpsPort()))
                .sourceHost(config.getOriginHost()).build();
        CdnDomainSources standbySource = toBean(originJson.get("standby"), CdnDomainSources.class);
        DomainBasicInfo.SourceStationStandbyInfo standby = DomainBasicInfo.SourceStationStandbyInfo.builder()
                .sourceStationType(standbySource == null ? "ipaddr" : standbySource.getOriginType())
                .ipOrDomain(standbySource == null ? "" : standbySource.getIpOrDomain())
                .httpPort(String.valueOf(standbySource == null || standbySource.getHttpPort() == null ? 80 : standbySource.getHttpPort()))
                .httpsPort(String.valueOf(standbySource == null || standbySource.getHttpsPort() == null ? 443 : standbySource.getHttpsPort()))
                .sourceHost(standbySource == null ? "" : standbySource.getHostName()).build();
        String domainStatus = domain.getDomainStatus();
        if ("configuring".equals(domainStatus) && Assert.notEmpty(domain.getCname())
                && selfHostedCdnService.isDomainConfigurationApplied(domain.getId())) {
            domainStatus = "online";
        }
        DomainBasicInfo basic = DomainBasicInfo.builder().domainName(domainName).domainStatus(domainStatus)
                .httpsStatus(config.getHttpsEnabled() != null && config.getHttpsEnabled() == 1 ? "1" : "0")
                .cname(domain.getCname()).businessType(domain.getBusinessType()).serviceArea(domain.getServiceArea())
                .isIpv6(config.getIpv6Enabled() != null && config.getIpv6Enabled() == 1 ? "1" : "0")
                .createTime(domain.getCreateTime()).updateTime(domain.getUpdateTime())
                .sourceStationPrimaryInfo(primary).sourceStationStandbyInfo(standby).build();
        DomainBackSourceInfo back = DomainBackSourceInfo.builder().origin_protocol(config.getOriginProtocol())
                .port("https".equals(config.getOriginProtocol()) ? config.getHttpsPort() : config.getHttpPort())
                .origin_receive_timeout(String.valueOf(intValue(originJson, "originReceiveTimeout", 30)))
                .origin_range_status(stringValue(originJson, "rangeStatus", "on"))
                .slice_etag_status(stringValue(originJson, "etagStatus", "off"))
                .upstream_follow_redirect_status(stringValue(originJson, "followRedirectStatus", "off"))
                .upstream_follow_redirect_max_times(intValue(originJson, "followRedirectMaxTimes", 1))
                .origin_request_url_rewrite(convertList(originJson.get("originRequestUrlRewrite"), DomainBackSourceInfo.BackSourceUrlChange.class))
                .flexible_origin(convertList(originJson.get("flexibleOrigins"), DomainBackSourceInfo.BackSourceAdvancedInfo.class))
                .origin_request_header(convertList(originJson.get("originRequestHeader"), DomainBackSourceInfo.BackSourceRequestInfo.class))
                .build();
        DomainHttpsInfo https = DomainHttpsInfo.builder()
                .https(DomainHttpsInfo.HttpGetBody.builder().https_status(config.getHttpsEnabled() != null && config.getHttpsEnabled() == 1 ? "on" : "off")
                        .certificate_name(stringValue(httpsJson, "certificateName", "self-hosted"))
                        .certificate_value("").certificate_source(0)
                        .http2_status(stringValue(httpsJson, "http2Status", "on"))
                        .tls_version(stringValue(httpsJson, "tlsVersion", "TLSv1.2,TLSv1.3"))
                        .ocsp_stapling_status(stringValue(httpsJson, "ocspStaplingStatus", "off")).build())
                .force_redirect(DomainHttpsInfo.ForceRedirect.builder().status(config.getForceRedirect())
                        .type("https").redirect_code(stringValue(httpsJson, "redirectCode", "301"))
                        .redirectType("https").redirectCode(intValue(httpsJson, "redirectCode", 301)).build()).build();

        RefererDTO refererConfig = toBean(accessJson.get("referer"), RefererDTO.class);
        int refererType = refererConfig == null || refererConfig.getReferer_type() == null ? 0 : refererConfig.getReferer_type();
        String refererValue = refererConfig == null ? "" : refererConfig.getReferer_list();
        if (refererConfig != null && Assert.isEmpty(refererValue) && refererConfig.getReferers() != null) {
            refererValue = String.join(",", refererConfig.getReferers());
        }
        int ipType = intValue(accessJson, "ipType", 0);
        List<String> ips = convertList(accessJson.get("ips"), String.class);
        UserAgentBlackAndWhiteListDTO userAgent = toBean(accessJson.get("userAgent"), UserAgentBlackAndWhiteListDTO.class);
        int uaType = userAgent == null || userAgent.getType() == null ? 0 : userAgent.getType();
        List<String> uaList = userAgent == null || userAgent.getUa_list() == null
                ? Collections.emptyList() : userAgent.getUa_list();
        UrlAuthDTO urlAuth = toBean(accessJson.get("urlAuth"), UrlAuthDTO.class);

        DomainVisitInfo visit = DomainVisitInfo.builder()
                .referer(DomainVisitInfo.Referer.builder().type(filterType(refererType)).referer_type(refererType)
                        .value(refererValue).include_empty(refererConfig == null || Boolean.TRUE.equals(refererConfig.getInclude_empty())).build())
                .ip_filter(DomainVisitInfo.IpFilter.builder().type(filterType(ipType)).value(String.join(",", ips)).build())
                .user_agent_filter(DomainVisitInfo.UserAgentFilter.builder().type(filterType(uaType))
                        .value(String.join(",", uaList)).ua_list(uaList).build())
                .url_auth(urlAuth == null
                        ? DomainVisitInfo.UrlAuth.builder().status("off").build()
                        : toBean(urlAuth, DomainVisitInfo.UrlAuth.class))
                .build();

        CompressDTO compressConfig = toBean(advancedJson.get("compress"), CompressDTO.class);
        DomainAdvancedInfo advanced = DomainAdvancedInfo.builder()
                .http_response_header(convertList(advancedJson.get("httpResponseHeaders"), DomainAdvancedInfo.HttpResponseHeader.class))
                .error_code_redirect_rules(convertList(advancedJson.get("errorCodeRedirectRules"), DomainAdvancedInfo.ErrorCodeRedirectRules.class))
                .error_pages(convertList(advancedJson.get("errorPages"), DomainAdvancedInfo.ErrorPage.class))
                .compress(DomainAdvancedInfo.Compress.builder()
                        .status(compressConfig == null ? "on" : normalizeSwitch(compressConfig.getStatus()))
                        .type(compressConfig == null || Assert.isEmpty(compressConfig.getType()) ? "gzip" : compressConfig.getType())
                        .file_type(".js,.html,.css,.xml,.json,.shtml,.htm").build())
                .build();

        IgnoreQueryStringDTO ignoreQueryString = toBean(cacheJson.get("ignoreQueryString"), IgnoreQueryStringDTO.class);
        if (ignoreQueryString == null) {
            ignoreQueryString = new IgnoreQueryStringDTO();
            ignoreQueryString.setEnable("off");
            ignoreQueryString.setType("block");
            ignoreQueryString.setHashKeyArgs("");
        }
        return DomainConfig.builder().domainBasicInfo(basic).domainBackSourceInfo(back).domainHttpsInfo(https)
                .domainCacheInfo(DomainCacheInfo.builder()
                        .cache_rules(convertList(cacheJson.get("cacheRules"), DomainCacheInfo.CacheRule.class))
                        .error_code_cache(convertList(cacheJson.get("errorCodeCache"), DomainCacheInfo.ErrorCodeCache.class))
                        .ignore_query_string(ignoreQueryString).build())
                .domainVisitInfo(visit)
                .domainAdvancedInfo(advanced)
                .build();
    }

    private void saveOriginValue(CdnDomain domain, String key, Object value) throws BusinessException {
        SelfHostedDomainConfig config = selfHostedCdnService.getDomainConfig(domain.getId());
        JSONObject origin = parseObject(config.getOriginConfigJson());
        origin.put(key, value == null ? new JSONArray() : value);
        config.setOriginConfigJson(origin.toJSONString());
        selfHostedCdnService.updateDomainConfig(config);
    }

    private void saveAccessValue(CdnDomain domain, String key, Object value) throws BusinessException {
        SelfHostedDomainConfig config = selfHostedCdnService.getDomainConfig(domain.getId());
        JSONObject access = selfHostedCdnService.readAccessConfig(config);
        access.put(key, value == null ? new JSONObject() : value);
        selfHostedCdnService.writeAccessConfig(config, access);
    }

    private void saveAdvancedValue(CdnDomain domain, String key, Object value) throws BusinessException {
        SelfHostedDomainConfig config = selfHostedCdnService.getDomainConfig(domain.getId());
        JSONObject advanced = parseObject(config.getAdvancedConfigJson());
        advanced.put(key, value == null ? new JSONObject() : value);
        config.setAdvancedConfigJson(advanced.toJSONString());
        selfHostedCdnService.updateDomainConfig(config);
    }

    private void mergeHttpsMetadata(SelfHostedDomainConfig config, HttpPutBodyDTO https) {
        if (https == null) return;
        JSONObject metadata = parseObject(config.getHttpsConfigJson());
        if (!Assert.isEmpty(https.getCertificate_name())) metadata.put("certificateName", https.getCertificate_name());
        if (!Assert.isEmpty(https.getHttp2_status())) metadata.put("http2Status", normalizeSwitch(https.getHttp2_status()));
        if (!Assert.isEmpty(https.getTls_version())) metadata.put("tlsVersion", https.getTls_version());
        if (!Assert.isEmpty(https.getOcsp_stapling_status())) metadata.put("ocspStaplingStatus", normalizeSwitch(https.getOcsp_stapling_status()));
        config.setHttpsConfigJson(metadata.toJSONString());
    }

    private JSONObject parseObject(String json) {
        if (Assert.isEmpty(json)) return new JSONObject();
        try {
            JSONObject value = JSON.parseObject(json);
            return value == null ? new JSONObject() : value;
        } catch (Exception ignored) {
            return new JSONObject();
        }
    }

    private <T> T toBean(Object value, Class<T> type) {
        if (value == null) return null;
        try {
            return JSON.parseObject(JSON.toJSONString(value), type);
        } catch (Exception ignored) {
            return null;
        }
    }

    private <T> List<T> convertList(Object value, Class<T> type) {
        if (value == null) return Collections.emptyList();
        try {
            List<T> result = JSON.parseArray(JSON.toJSONString(value), type);
            return result == null ? Collections.emptyList() : result;
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    private String normalizeSwitch(String value) {
        return "on".equalsIgnoreCase(value) ? "on" : "off";
    }

    private String filterType(int type) {
        if (type == 1) return "black";
        if (type == 2) return "white";
        return "off";
    }

    private String stringValue(JSONObject value, String key, String defaultValue) {
        String result = value == null ? null : value.getString(key);
        return Assert.isEmpty(result) ? defaultValue : result;
    }

    private int intValue(JSONObject value, String key, int defaultValue) {
        try {
            Integer result = value == null ? null : value.getInteger(key);
            return result == null ? defaultValue : result;
        } catch (Exception ignored) {
            return defaultValue;
        }
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
