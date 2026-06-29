package com.kuocai.cdn.service.domain.operation;

import cn.hutool.core.util.RandomUtil;
import com.kuocai.cdn.api.*;
import com.kuocai.cdn.api.cdnetworks.cdn.CdnetworksClient;
import com.kuocai.cdn.api.cdnetworks.cdn.dto.*;
import com.kuocai.cdn.api.cdnetworks.cdn.vo.*;
import com.kuocai.cdn.api.huawei.cdn.dto.*;
import com.kuocai.cdn.api.tencent.dns.CreateRecordResponse;
import com.kuocai.cdn.api.tencent.dns.TencentApi;
import com.kuocai.cdn.api.tencent.dns.dto.CreateRecordDTO;
import com.kuocai.cdn.api.tencent.dns.properties.TencentDns;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.entity.CdnDomainSources;
import com.kuocai.cdn.enumeration.domainmerage.domain.*;
import com.kuocai.cdn.enumeration.domainmerage.route.CdnOperationRoute;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.exception.CdnetworksException;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.service.domain.operation.optional.ICdnCdnetworksPlatformService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.DomainUtil;
import com.kuocai.cdn.util.KuocaiDateUtil;
import com.kuocai.cdn.util.ThreadMdcUtils;
import com.kuocai.cdn.vo.*;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class CdnetworksDomainServiceImpl extends BaseService<CdnDomain> implements ICdnPlatformService, ICdnCdnetworksPlatformService {

    private final Executor executorService;

    CdnetworksDomainServiceImpl(@Qualifier("cdnDomainExecutor") Executor executorService) {
        this.executorService = executorService;
    }

    private <T> CompletableFuture<T> executeAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(ThreadMdcUtils.wrapAsync(supplier, MDC.getCopyOfContextMap()), executorService);
    }

    @Override
    public CdnDomain create(Long userId, String domainName, String businessType, String serviceArea, String originType, String ipOrDomain) throws BusinessException, InterruptedException {
        try {
            // ## 创建域名 纯海外 不分加速地区 业务类型 回源类型
            AddCdnDomainDTO addCdnDomainDTO = new AddCdnDomainDTO();
            addCdnDomainDTO.setDomainName(domainName);
            // ## 回源配置
            AddCdnDomainDTO.AddCdnDomainRequestOriginConfig originConfig = new AddCdnDomainDTO.AddCdnDomainRequestOriginConfig();
            // 分号隔开
            originConfig.setOriginIps(ipOrDomain);
            originConfig.setDefaultOriginHostHeader(domainName);
            addCdnDomainDTO.setOriginConfig(originConfig);
            addCdnDomainDTO.setComment(String.valueOf(userId));
            AddCdnDomainVO addCdnDomainVO;
            try {
                addCdnDomainVO = CdnetworksClient.AddCdnDomain(addCdnDomainDTO);
                log.info("创建加速域名 {}, 成功：{}", domainName, addCdnDomainVO);
            } catch (CdnetworksException e) {
                log.error("创建加速域名 {}, 失败 {}", domainName, e.getMessage());
                throw new BusinessException(e.getMessage());
            }
            CdnDomain cdnDomain = CdnDomain.builder()
                    .userId(userId)
                    .domainName(domainName)
                    .businessType(businessType)
                    .serviceArea(serviceArea)
                    .domainId(addCdnDomainVO.getDomainId())
                    .cnameCdnetworks(addCdnDomainVO.getCname())
                    .domainStatus(DomainStatus.CONFIGURING.getParam())
                    .route(CdnOperationRoute.CDNETWORKS.getRoute())
                    .build();
            return save(cdnDomain);
        } catch (Exception e) {
            log.error("创建域名 {} 流程失败：{}", domainName, e.getMessage());
            throw new BusinessException(e.getMessage());
        }
    }

    @Override
    public CdnDomain configDNS(CdnDomain cdnDomain) throws TencentCloudSDKException, BusinessException {
        String domainName = cdnDomain.getDomainName();
//        String cname = cdnDomain.getCnameCdnetworks();
        CreateRecordDTO createRecordDTO = new CreateRecordDTO();
        createRecordDTO.setDomain(TencentDns.LOCAL_DOMAIN_NAME).setSubDomain(DomainUtil.convertSubDomain(domainName)).setValue(cdnDomain.getCnameCdnetworks());
        CreateRecordResponse createRecordResponse = TencentApi.createRecord(createRecordDTO);
        if (Assert.isEmpty(createRecordResponse.getRecordId())) {
            log.error("dns解析失败，域名：{}", cdnDomain.getDomainName());
            throw new BusinessException("dns解析失败");
        }
        cdnDomain.setCname(createRecordDTO.getSubDomain() + "." + TencentDns.LOCAL_DOMAIN_NAME);
        cdnDomain.setTencentDnsId(createRecordResponse.getRecordId());
        cdnDomain = save(cdnDomain);
        return cdnDomain;
    }

    @Override
    public void save(CdnDomain cdnDomain, String businessType, String serviceArea) throws BusinessException {

    }

    @Override
    public void disable(CdnDomain cdnDomain) throws BusinessException {
        DisableDomainDTO disableDomainDTO = new DisableDomainDTO(cdnDomain.getDomainName());
        try {
            CdnetworksClient.DisableDomain(disableDomainDTO);
            log.info("域名 {} 下线成功", cdnDomain.getDomainName());
        } catch (CdnetworksException e) {
            log.error("域名 {} 下线失败：{}", cdnDomain.getDomainName(), e.getMessage());
            throw new BusinessException(e.getMessage());
        }
    }

    @Override
    public void enable(CdnDomain cdnDomain) throws BusinessException {
        EnableDomainDTO enableDomainDTO = new EnableDomainDTO(cdnDomain.getDomainName());
        try {
            CdnetworksClient.EnableDomain(enableDomainDTO);
            log.info("域名 {} 上线成功", cdnDomain.getDomainName());
        } catch (CdnetworksException e) {
            log.error("域名 {} 上线失败：{}", cdnDomain.getDomainName(), e.getMessage());
            throw new BusinessException(e.getMessage());
        }
    }

    @Override
    public void delete(CdnDomain cdnDomain) throws BusinessException {
        DeleteDomainDTO deleteDomainDTO = new DeleteDomainDTO();
        deleteDomainDTO.setDomainName(cdnDomain.getDomainName());
        try {
            CdnetworksClient.DeleteDomain(deleteDomainDTO);
            log.info("域名 {} 删除成功", cdnDomain.getDomainName());
        } catch (CdnetworksException e) {
            log.error("域名 {} 删除失败：{}", cdnDomain.getDomainName(), e.getMessage());
            throw new BusinessException(e.getMessage());
        }
    }

    @Override
    public void ipv6(CdnDomain cdnDomain, Integer status) throws BusinessException {
        // 不支持
    }

    @Override
    public void saveSourceStationConfig(CdnDomain cdnDomain, CdnDomainSourcesVo config) throws BusinessException {
        CdnDomainSources main = config.getMain();
        UpdateOriginDTO updateOriginDTO = new UpdateOriginDTO();
        updateOriginDTO.setDomain(cdnDomain.getDomainName());
        updateOriginDTO.setOriginIps(main.getIpOrDomain());
        updateOriginDTO.setOriginHost(main.getHostName());
        updateOriginDTO.setOriginPort(main.getHttpPort() > 0 ? String.valueOf(main.getHttpPort()) : "");
        try {
            CdnetworksClient.UpdateOrigin(updateOriginDTO);
            log.info("域名 {} 回源配置：{}", cdnDomain.getDomainName(), config);
        } catch (CdnetworksException e) {
            log.error("域名 {} 回源配置失败：{}", cdnDomain.getDomainName(), e.getMessage());
            throw new BusinessException(e.getMessage());
        }
    }

    @Override
    public void change(CdnDomain cdnDomain) throws BusinessException {

    }

    @Override
    public void saveOriginProtocol(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        String protocol = domainOriginSettingVo.getOriginProtocol();
        UpdateOriginProtocolDTO updateOriginProtocolDTO;
        if ("follow".equals(protocol)) {
            updateOriginProtocolDTO = UpdateOriginProtocolDTO.Follow();
        } else {
            updateOriginProtocolDTO = "https".equals(protocol) ? UpdateOriginProtocolDTO.Https() : UpdateOriginProtocolDTO.Http();
        }
        updateOriginProtocolDTO.setDomain(cdnDomain.getDomainName());
        try {
            CdnetworksClient.UpdateOriginProtocol(updateOriginProtocolDTO);
        } catch (CdnetworksException e) {
            log.error("域名 {} 回源协议配置失败：{}", cdnDomain.getDomainName(), e.getMessage());
            throw new BusinessException(e.getMessage());
        }
        log.info("域名 {} 回源协议配置：{}", cdnDomain.getDomainName(), protocol);
    }

    @Override
    public void saveOriginRequestUrlRewrite(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {

    }

    @Override
    public void saveAdvancedReturnSource(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {

    }

    @Override
    public void saveRangeSwitch(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {

    }

    @Override
    public void saveRangeVerifyETag(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {

    }

    @Override
    public void saveOriginHost(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {

    }

    @Override
    public void saveRangeTimeOut(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {

    }

    @Override
    public void saveOriginRequestHeader(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        // 查询配置
        List<OriginRequestHeaderDTO> originRequestHeaders = domainOriginSettingVo.getOriginRequestHeader();
        QueryHttpHeaderDTO queryHttpHeaderDTO = new QueryHttpHeaderDTO(cdnDomain.getDomainName());
        QueryHttpHeaderVO queryHttpHeaderVO = CdnetworksClient.QueryHttpHeader(queryHttpHeaderDTO);
        // 更新配置
        List<UpdateHttpHeaderDTO.HeaderModifyRule> headerModifyRules = filterHttpHeaderRule(queryHttpHeaderVO.getHeaderModifyRules(), "cache2origin");
        for (OriginRequestHeaderDTO originRequestHeaderDTO : originRequestHeaders) {
            UpdateHttpHeaderDTO.HeaderModifyRule headerModifyRule = new UpdateHttpHeaderDTO.HeaderModifyRule();
            headerModifyRule.setCustomPattern("all");
            headerModifyRule.setHeaderDirection("cache2origin");
            headerModifyRule.setHeaderName(originRequestHeaderDTO.getName());
            if (!"delete".equals(originRequestHeaderDTO.getAction())) {
                headerModifyRule.setHeaderValue(originRequestHeaderDTO.getValue());
            }
            if ("set".equals(originRequestHeaderDTO.getAction())) {
                headerModifyRule.setAction("add");
            } else {
                headerModifyRule.setAction(originRequestHeaderDTO.getAction());
            }
            headerModifyRules.add(headerModifyRule);
        }
        UpdateHttpHeaderDTO updateHttpHeaderDTO = new UpdateHttpHeaderDTO();
        updateHttpHeaderDTO.setDomainName(cdnDomain.getDomainName());
        updateHttpHeaderDTO.setHeaderModifyRules(headerModifyRules);
        try {
            CdnetworksClient.UpdateHttpHeader(updateHttpHeaderDTO);
            log.info("域名 {} Request头配置：{}", cdnDomain.getDomainName(), domainOriginSettingVo);
        } catch (CdnetworksException e) {
            log.error("域名 {} Request头配置失败：{}", cdnDomain.getDomainName(), e.getMessage());
            throw new BusinessException(e.getMessage());
        }
    }

    public String randomCertName(String domainName) {
        return domainName + "-" + RandomUtil.randomString(8);
    }

    @Override
    public void httpsConfiguration(CdnDomain cdnDomain, DomainHttpsSettingVo config) throws BusinessException {
        HttpPutBodyDTO httpPutBodyDTO = config.getHttps();
        String httpsStatus = httpPutBodyDTO.getHttps_status();
        UpdateDomainCertDTO updateDomainCertDTO = new UpdateDomainCertDTO();
        updateDomainCertDTO.setDomain(cdnDomain.getDomainName());
        if ("on".equals(httpsStatus)) {
            // 先新增证书
            AddCertDTO addCertDTO = new AddCertDTO();
            addCertDTO.setName(randomCertName(cdnDomain.getDomainName()));
            addCertDTO.setCertificate(httpPutBodyDTO.getCertificate_value());
            addCertDTO.setPrivateKey(httpPutBodyDTO.getPrivate_key());
            addCertDTO.setComment(cdnDomain.getDomainName());
            AddCertVO addCertVO;
            try {
                addCertVO = CdnetworksClient.AddCert(addCertDTO);
            } catch (CdnetworksException e) {
                log.error("域名 {} Cert 新增失败：{}", cdnDomain.getDomainName(), e.getMessage());
                throw new BusinessException(e.getMessage());
            }
            // 更新证书
            updateDomainCertDTO.setCertificateId(addCertVO.getCsrId());
            updateDomainCertDTO.setTlsVersion("TLSv1.1;TLSv1.2;TLSv1.3");
        }
        try {
            CdnetworksClient.UpdateDomainCert(updateDomainCertDTO);
            log.info("域名 {} Cert 配置：{}", cdnDomain.getDomainName(), httpPutBodyDTO);
        } catch (CdnetworksException e) {
            log.error("域名 {} Cert 配置失败：{}", cdnDomain.getDomainName(), e.getMessage());
            throw new BusinessException(e.getMessage());
        }
    }

    @Override
    public void httpsConfigurationOther(CdnDomain cdnDomain, DomainHttpsSettingVo config) throws BusinessException {
        HttpPutBodyDTO httpPutBodyDTO = config.getHttps();
        String http2Status = httpPutBodyDTO.getHttp2_status();
        // HTTP 2
        if (Assert.notEmpty(http2Status)) {
            UpdateHttp2SettingsDTO updateHttp2SettingsDTO = new UpdateHttp2SettingsDTO();
            if ("on".equals(http2Status)) {
                updateHttp2SettingsDTO.setHttp2Settings(UpdateHttp2SettingsDTO.Http2Settings.EnableHttp2());
            } else {
                updateHttp2SettingsDTO.setHttp2Settings(UpdateHttp2SettingsDTO.Http2Settings.DisableHttp2());
            }
            updateHttp2SettingsDTO.setDomainName(cdnDomain.getDomainName());
            try {
                CdnetworksClient.UpdateHttp2Settings(updateHttp2SettingsDTO);
                log.info("域名 {} HTTP2 状态修改为 {}", cdnDomain.getDomainName(), http2Status);
            } catch (CdnetworksException e) {
                log.error("域名 {} HTTP2 状态修改失败：{}", cdnDomain.getDomainName(), e.getMessage());
                throw new BusinessException(e.getMessage());
            }
        }
    }

    @Override
    public void forcedToJump(CdnDomain cdnDomain, DomainHttpsSettingVo config,String redirectCode) throws BusinessException {
        ForceRedirectConfigDTO forceRedirectConfigDTO = config.getForceRedirect();
        UpdateInnerRedirectDTO updateInnerRedirectDTO = new UpdateInnerRedirectDTO();
        updateInnerRedirectDTO.setDomainName(cdnDomain.getDomainName());
        List<UpdateInnerRedirectDTO.RewriteRuleSetting> rewriteRuleSettings = new ArrayList<>();
        if ("on".equals(forceRedirectConfigDTO.getStatus())) {
            String code = String.valueOf(forceRedirectConfigDTO.getRedirect_code());
            if ("https".equals(forceRedirectConfigDTO.getType())) {
                rewriteRuleSettings.add(UpdateInnerRedirectDTO.createRewriteToHttpsRule(code));
            } else {
                rewriteRuleSettings.add(UpdateInnerRedirectDTO.createRewriteToHttpRule(code));
            }
        }
        updateInnerRedirectDTO.setRewriteRuleSettings(rewriteRuleSettings);
        try {
            CdnetworksClient.UpdateInnerRedirect(updateInnerRedirectDTO);
            log.info("域名 {} 强制跳转配置：{}", cdnDomain.getDomainName(), config);
        } catch (CdnetworksException e) {
            log.error("域名 {} 强制跳转配置失败：{}", cdnDomain.getDomainName(), e.getMessage());
            throw new BusinessException(e.getMessage());
        }
    }

    @Override
    public void saveCacheRules(CdnDomain cdnDomain, SettingCacheVo config) throws BusinessException {
        UpdateCacheTimeDTO updateCacheTimeDTO = new UpdateCacheTimeDTO();
        updateCacheTimeDTO.setDomainName(cdnDomain.getDomainName());

        String compareMaxAge = config.getCacheFollowOriginStatus();
        // 默认 off 防止为空情况
        if (Assert.isEmpty(compareMaxAge)) {
            compareMaxAge = "off";
        }

        ArrayList<UpdateCacheTimeDTO.CacheTimeBehavior> cacheTimeBehaviors = new ArrayList<>();

        for (CacheRuleDTO cacheRule : config.getCacheRules()) {
            String matchType = cacheRule.getMatch_type();
            String matchValue = cacheRule.getMatch_value();
            UpdateCacheTimeDTO.CacheTimeBehavior cacheTimeBehavior = new UpdateCacheTimeDTO.CacheTimeBehavior();
            switch (matchType) {
                case "full_path":
                    cacheTimeBehavior.setSpecifyUrlPattern(matchValue);
                    break;
                case "catalog":
                    cacheTimeBehavior.setDirectory(matchValue);
                    break;
                case "file_extension":
                    cacheTimeBehavior.setFileType(matchValue.replace(".", ""));
                    break;
                case "home_page":
                    cacheTimeBehavior.setCustomPattern("homepage");
                    break;
                default:
                    cacheTimeBehavior.setCustomPattern("all");
                    break;
            }
            if (0 == cacheRule.getTtl()) {
                cacheTimeBehavior.setCacheTtl("0s");
            } else {
                cacheTimeBehavior.setCacheTtl(String.format("%d%s", cacheRule.getTtl(), cacheRule.getTtl_unit()));
            }
            cacheTimeBehavior.setIgnoreCacheControl("false");
            cacheTimeBehavior.setIsRespectServer("on".equals(compareMaxAge) ? "true" : "false");
            cacheTimeBehavior.setIgnoreLetterCase("false");
            cacheTimeBehavior.setReloadManage("if-modified-since");
            cacheTimeBehavior.setPriority("10");
            cacheTimeBehavior.setIgnoreAuthenticationHeader("false");
            cacheTimeBehaviors.add(cacheTimeBehavior);
        }
        Collections.reverse(cacheTimeBehaviors);
        updateCacheTimeDTO.setCacheTimeBehaviors(cacheTimeBehaviors);
        try {
            CdnetworksClient.UpdateCacheTime(updateCacheTimeDTO);
            log.info("域名 {} 缓存规则配置：{}", cdnDomain.getDomainName(), config);
        } catch (CdnetworksException e) {
            log.error("域名 {} 缓存规则配置失败：{}", cdnDomain.getDomainName(), e.getMessage());
            throw new BusinessException(e.getMessage());
        }
    }

    @Override
    public void saveCacheFollowOriginStatusSwitch(CdnDomain cdnDomain, SettingCacheVo config) throws BusinessException {

    }

    @Override
    public void saveErrorCodeCache(CdnDomain cdnDomain, SettingCacheVo config) throws BusinessException {
        UpdateHttpCodeCacheDTO updateHttpCodeCacheDTO = new UpdateHttpCodeCacheDTO();
        updateHttpCodeCacheDTO.setDomainName(cdnDomain.getDomainName());
        ArrayList<UpdateHttpCodeCacheDTO.HttpCodeCacheRule> httpCodeCacheRules = new ArrayList<>();
        for (ErrorCodeCacheDTO errorCodeCacheDTO : config.getErrorCodeCache()) {
            UpdateHttpCodeCacheDTO.HttpCodeCacheRule httpCodeCacheRule = new UpdateHttpCodeCacheDTO.HttpCodeCacheRule();
            httpCodeCacheRule.setHttpCodes(new String[]{errorCodeCacheDTO.getCode().toString()});
            httpCodeCacheRule.setCacheTtl(String.format("%d", errorCodeCacheDTO.getTtl()));
            httpCodeCacheRules.add(httpCodeCacheRule);
        }
        updateHttpCodeCacheDTO.setHttpCodeCacheRules(httpCodeCacheRules);
        try {
            CdnetworksClient.UpdateHttpCodeCache(updateHttpCodeCacheDTO);
            log.info("域名 {} 错误码缓存配置：{}", cdnDomain.getDomainName(), config);
        } catch (CdnetworksException e) {
            log.error("域名 {} 错误码缓存配置失败：{}", cdnDomain.getDomainName(), e.getMessage());
            throw new BusinessException(e.getMessage());
        }
    }

    @Override
    public void saveHotlinkPrevention(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        RefererDTO refererDTO = config.getReferer();
        // 先获取当前配置
        QueryAntiHotlinkingDTO queryAntiHotlinkingDTO = new QueryAntiHotlinkingDTO(cdnDomain.getDomainName());
        QueryAntiHotlinkingVO queryAntiHotlinkingVO = CdnetworksClient.QueryAntiHotlinking(queryAntiHotlinkingDTO);
        // 更新配置
        UpdateAntiHotlinkingDTO updateAntiHotlinkingDTO = new UpdateAntiHotlinkingDTO();
        updateAntiHotlinkingDTO.setDomainName(cdnDomain.getDomainName());
        // 防盗链
        Integer refererType = refererDTO.getReferer_type();
        // 过滤配置
        List<UpdateAntiHotlinkingDTO.VisitControlRule> visitControlRules = filterVisitControlRule(Arrays.asList(queryAntiHotlinkingVO.getVisitControlRules()), "referer");
        if (refererType > 0) {
            UpdateAntiHotlinkingDTO.VisitControlRule visitControlRule = new UpdateAntiHotlinkingDTO.VisitControlRule();
            UpdateAntiHotlinkingDTO.RefererControlRule referer = new UpdateAntiHotlinkingDTO.RefererControlRule();
            String domainStr = String.join(";", refererDTO.getReferers());
            if (2 == refererType) {
                referer.setValidDomain(domainStr);
            } else {
                referer.setInvalidDomain(domainStr);
            }
            referer.setAllowNullReferer(refererDTO.getInclude_empty() ? "true" : "false");
            visitControlRule.setRefererControlRule(referer);
            visitControlRules.add(visitControlRule);
        }
        updateAntiHotlinkingDTO.setVisitControlRules(visitControlRules);
        try {
            CdnetworksClient.UpdateAntiHotlinking(updateAntiHotlinkingDTO);
            log.info("域名 {} 防盗链配置：{}", cdnDomain.getDomainName(), config);
        } catch (CdnetworksException e) {
            log.error("域名 {} 防盗链配置失败：{}", cdnDomain.getDomainName(), e.getMessage());
            throw new BusinessException(e.getMessage());
        }
    }

    public List<UpdateAntiHotlinkingDTO.VisitControlRule> filterVisitControlRule(List<QueryAntiHotlinkingVO.VisitControlRule> rules, String filter) {
        List<UpdateAntiHotlinkingDTO.VisitControlRule> visitControlRules = new ArrayList<>();
        if (null == rules) {
            return visitControlRules;
        }
        int state = 0;
        for (QueryAntiHotlinkingVO.VisitControlRule rule : rules) {
            QueryAntiHotlinkingVO.VisitControlRuleIpControlRule ipControlRule = rule.getIpControlRule();
            QueryAntiHotlinkingVO.VisitControlRuleUaControlRule uaControlRule = rule.getUaControlRule();
            QueryAntiHotlinkingVO.VisitControlRuleRefererControlRule refererControlRule = rule.getRefererControlRule();
            UpdateAntiHotlinkingDTO.VisitControlRule visitControlRule = new UpdateAntiHotlinkingDTO.VisitControlRule();
            if (isIpControlRule(ipControlRule) && (state & 1) != 1 && !"ip".equals(filter)) {
                UpdateAntiHotlinkingDTO.IpControlRule ip = new UpdateAntiHotlinkingDTO.IpControlRule();
                if (Assert.notEmpty(ipControlRule.getAllowedIps())) {
                    ip.setAllowedIps(ipControlRule.getAllowedIps());
                } else {
                    ip.setForbiddenIps(ipControlRule.getForbiddenIps());
                }
////                ip.setAllowedIps(ipControlRule.getAllowedIps());
////                ip.setForbiddenIps(ipControlRule.getForbiddenIps());
                visitControlRule.setIpControlRule(ip);
                state |= 1;
            } else if (isUaControlRule(uaControlRule) && (state & 2) != 2 && !"ua".equals(filter)) {
                UpdateAntiHotlinkingDTO.UaControlRule ua = new UpdateAntiHotlinkingDTO.UaControlRule();
//                ua.setInvalidUserAgents(uaControlRule.getInvalidUserAgents());
//                ua.setValidUserAgents(uaControlRule.getValidUserAgents());
                if (Assert.notEmpty(uaControlRule.getValidUserAgents())) {
                    ua.setValidUserAgents(uaControlRule.getValidUserAgents());
                } else {
                    ua.setInvalidUserAgents(uaControlRule.getInvalidUserAgents());
                }
                visitControlRule.setUaControlRule(ua);
                state |= 2;
            } else if (isRefererControlRule(refererControlRule) && (state & 4) != 4 && !"referer".equals(filter)) {
                UpdateAntiHotlinkingDTO.RefererControlRule referer = new UpdateAntiHotlinkingDTO.RefererControlRule();
//                referer.setInvalidReferer(refererControlRule.getInvalidReferer());
//                referer.setValidReferer(refererControlRule.getValidReferer());
                referer.setAllowNullReferer(refererControlRule.getAllowNullReferer());
//                referer.setInvalidDomain(refererControlRule.getInvalidDomain());
//                referer.setValidDomain(refererControlRule.getValidDomain());
                if (Assert.notEmpty(refererControlRule.getValidDomain())) {
                    referer.setValidDomain(refererControlRule.getValidDomain());
                } else {
                    referer.setInvalidDomain(refererControlRule.getInvalidDomain());
                }
//                referer.setInvalidUrl(refererControlRule.getInvalidUrl());
//                referer.setValidUrl(refererControlRule.getValidUrl());
                visitControlRule.setRefererControlRule(referer);
                state |= 4;
            } else {
                continue;
            }
            visitControlRules.add(visitControlRule);
        }
        return visitControlRules;
    }

    @Override
    public void saveIpBlackWhiteList(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        Integer banIpType = config.getType();
        // 先获取当前配置
        QueryAntiHotlinkingDTO queryAntiHotlinkingDTO = new QueryAntiHotlinkingDTO(cdnDomain.getDomainName());
        QueryAntiHotlinkingVO queryAntiHotlinkingVO = CdnetworksClient.QueryAntiHotlinking(queryAntiHotlinkingDTO);
        // 更新配置
        UpdateAntiHotlinkingDTO updateAntiHotlinkingDTO = new UpdateAntiHotlinkingDTO();
        updateAntiHotlinkingDTO.setDomainName(cdnDomain.getDomainName());
        // 过滤配置
        List<UpdateAntiHotlinkingDTO.VisitControlRule> visitControlRules = filterVisitControlRule(Arrays.asList(queryAntiHotlinkingVO.getVisitControlRules()), "ip");
        if (banIpType > 0) {
            UpdateAntiHotlinkingDTO.VisitControlRule visitControlRule = new UpdateAntiHotlinkingDTO.VisitControlRule();
            UpdateAntiHotlinkingDTO.IpControlRule ip = new UpdateAntiHotlinkingDTO.IpControlRule();
            String ipStr = String.join(";", config.getIps());
            if (2 == banIpType) {
                ip.setAllowedIps(ipStr);
            } else {
                ip.setForbiddenIps(ipStr);
            }
            visitControlRule.setIpControlRule(ip);
            visitControlRules.add(visitControlRule);
        }
        updateAntiHotlinkingDTO.setVisitControlRules(visitControlRules);
        try {
            CdnetworksClient.UpdateAntiHotlinking(updateAntiHotlinkingDTO);
            log.info("域名 {} IP黑白名单配置：{}", cdnDomain.getDomainName(), config);
        } catch (CdnetworksException e) {
            log.error("域名 {} IP黑白名单配置失败：{}", cdnDomain.getDomainName(), e.getMessage());
            throw new BusinessException(e.getMessage());
        }
    }

    @Override
    public void saveUserAgentFilter(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        UserAgentBlackAndWhiteListDTO userAgentBlackAndWhiteListDTO = config.getUserAgentBlackAndWhiteListDTO();
        Integer filterType = userAgentBlackAndWhiteListDTO.getType();
        // 先获取当前配置
        QueryAntiHotlinkingDTO queryAntiHotlinkingDTO = new QueryAntiHotlinkingDTO(cdnDomain.getDomainName());
        QueryAntiHotlinkingVO queryAntiHotlinkingVO = CdnetworksClient.QueryAntiHotlinking(queryAntiHotlinkingDTO);
        // 更新配置
        UpdateAntiHotlinkingDTO updateAntiHotlinkingDTO = new UpdateAntiHotlinkingDTO();
        updateAntiHotlinkingDTO.setDomainName(cdnDomain.getDomainName());
        // 过滤配置
        List<UpdateAntiHotlinkingDTO.VisitControlRule> visitControlRules = filterVisitControlRule(Arrays.asList(queryAntiHotlinkingVO.getVisitControlRules()), "ua");
        if (filterType > 0) {
            UpdateAntiHotlinkingDTO.VisitControlRule visitControlRule = new UpdateAntiHotlinkingDTO.VisitControlRule();
            UpdateAntiHotlinkingDTO.UaControlRule ua = new UpdateAntiHotlinkingDTO.UaControlRule();
            String uaStr = String.join(";", userAgentBlackAndWhiteListDTO.getUa_list());
            if (2 == filterType) {
                ua.setValidUserAgents(uaStr);
            } else {
                ua.setInvalidUserAgents(uaStr);
            }
            visitControlRule.setUaControlRule(ua);
            visitControlRules.add(visitControlRule);
        }
        updateAntiHotlinkingDTO.setVisitControlRules(visitControlRules);
        try {
            CdnetworksClient.UpdateAntiHotlinking(updateAntiHotlinkingDTO);
            log.info("域名 {} UserAgent黑白名单配置：{}", cdnDomain.getDomainName(), config);
        } catch (CdnetworksException e) {
            log.error("域名 {} UserAgent黑白名单配置失败：{}", cdnDomain.getDomainName(), e.getMessage());
            throw new BusinessException(e.getMessage());
        }
    }

    @Override
    public void saveUrlAuth(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {

    }

    @Override
    public void saveHttpHeader(CdnDomain cdnDomain, SettingHigherVo config) throws BusinessException {
        // 查询配置
        List<HttpResponseHeaderDTO> httpResponseHeaders = config.getHttpResponseHeaders();
        QueryHttpHeaderDTO queryHttpHeaderDTO = new QueryHttpHeaderDTO(cdnDomain.getDomainName());
        QueryHttpHeaderVO queryHttpHeaderVO = CdnetworksClient.QueryHttpHeader(queryHttpHeaderDTO);
        // 更新配置
        List<UpdateHttpHeaderDTO.HeaderModifyRule> headerModifyRules = filterHttpHeaderRule(queryHttpHeaderVO.getHeaderModifyRules(), "cache2visitor");
        for (HttpResponseHeaderDTO httpResponseHeader : httpResponseHeaders) {
            UpdateHttpHeaderDTO.HeaderModifyRule headerModifyRule = new UpdateHttpHeaderDTO.HeaderModifyRule();
            headerModifyRule.setCustomPattern("all");
            headerModifyRule.setHeaderDirection("cache2visitor");
            headerModifyRule.setHeaderName(httpResponseHeader.getName());
            if (!"delete".equals(httpResponseHeader.getAction())) {
                headerModifyRule.setHeaderValue(httpResponseHeader.getValue());
            }
            if ("set".equals(httpResponseHeader.getAction())) {
                headerModifyRule.setAction("add");
            } else {
                headerModifyRule.setAction(httpResponseHeader.getAction());
            }
            headerModifyRules.add(headerModifyRule);
        }
        UpdateHttpHeaderDTO updateHttpHeaderDTO = new UpdateHttpHeaderDTO();
        updateHttpHeaderDTO.setDomainName(cdnDomain.getDomainName());
        updateHttpHeaderDTO.setHeaderModifyRules(headerModifyRules);
        try {
            CdnetworksClient.UpdateHttpHeader(updateHttpHeaderDTO);
            log.info("域名 {} HTTP头配置：{}", cdnDomain.getDomainName(), config);
        } catch (CdnetworksException e) {
            log.error("域名 {} HTTP头配置失败：{}", cdnDomain.getDomainName(), e.getMessage());
            throw new BusinessException(e.getMessage());
        }
    }

    public List<UpdateHttpHeaderDTO.HeaderModifyRule> filterHttpHeaderRule(QueryHttpHeaderVO.HeaderModifyRule[] rules, String filter) {
        List<UpdateHttpHeaderDTO.HeaderModifyRule> headerModifyRules = new ArrayList<>();
        if (null == rules) {
            return headerModifyRules;
        }
        List<String> filterList = Arrays.asList("origin2cache", "visitor2cache");
        for (QueryHttpHeaderVO.HeaderModifyRule rule : rules) {
            if (filter.equals(rule.getHeaderDirection())) {
                continue;
            }
            if (filterList.contains(rule.getHeaderDirection())) {
                continue;
            }
            UpdateHttpHeaderDTO.HeaderModifyRule headerModifyRule = new UpdateHttpHeaderDTO.HeaderModifyRule();
            headerModifyRule.setCustomPattern("all");
            headerModifyRule.setHeaderDirection(rule.getHeaderDirection());
            headerModifyRule.setHeaderName(rule.getHeaderName());
            headerModifyRule.setHeaderValue(rule.getHeaderValue());
            headerModifyRule.setAction(rule.getAction());
//            headerModifyRule.setExceptPathPattern(rule.getExceptPathPattern());
            headerModifyRules.add(headerModifyRule);
        }
        return headerModifyRules;
    }

    @Override
    public void saveCustomErrorPageConfiguration(CdnDomain cdnDomain, SettingHigherVo config) throws BusinessException {

    }

    @Override
    public void saveCompress(CdnDomain cdnDomain, SettingHigherVo config) throws BusinessException {

    }

    @Override
    public DomainConfig getDomainConfig(String domainName) throws BusinessException {
        BasicDomainDTO basicDomainDTO = new BasicDomainDTO(domainName);
        CompletableFuture<BasicDomainVO> basicDomainVOCompletableFuture = executeAsync(() -> {
            try {
                return CdnetworksClient.BasicDomain(basicDomainDTO);
            } catch (CdnetworksException e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture.allOf(basicDomainVOCompletableFuture).join();

        BasicDomainVO vo;
        try {
            vo = basicDomainVOCompletableFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("获取域名配置失败", e);
            throw new BusinessException(e.getMessage());
        }
        String httpsStatus = "0";
        String isIpv6 = "0";
        // ## 回源信息
        DomainBasicInfo.SourceStationPrimaryInfo sourceStationPrimaryInfo = DomainBasicInfo.SourceStationPrimaryInfo.builder()
                .sourceStationType(OriginTypeEnum.IPADDR.getParam())
                .ipOrDomain("").httpPort("80").httpsPort("443").sourceHost(vo.getDomainName())
                .build();
        BasicDomainVO.OriginConfig originConfig = vo.getOriginConfig();
        sourceStationPrimaryInfo.setIpOrDomain(originConfig.getOriginIps());
        if (Assert.isEmpty(originConfig.getOriginPort())) {
            sourceStationPrimaryInfo.setHttpPort("0");
        } else {
            sourceStationPrimaryInfo.setHttpPort(originConfig.getOriginPort());
        }
        String defaultOriginHostHeader = originConfig.getDefaultOriginHostHeader();
        if (Assert.notEmpty(defaultOriginHostHeader)) {
            sourceStationPrimaryInfo.setSourceHost(defaultOriginHostHeader);
        }
        DomainBasicInfo.SourceStationStandbyInfo sourceStationStandbyInfo = DomainBasicInfo.SourceStationStandbyInfo.builder()
                .sourceStationType("").ipOrDomain("").httpPort("").httpsPort("").sourceHost("")
                .build();

        BasicDomainVO.Ssl ssl = vo.getSsl();
        if (null != ssl) {
            if ("true".equals(ssl.getUseSsl())) {
                httpsStatus = "1";
            }
        }

        DomainBasicInfo domainBasicInfo = DomainBasicInfo.builder()
                .domainName(domainName)
                .domainStatus(convertDomainStatus(vo.getStatus(), vo.getEnabled()))
                .httpsStatus(httpsStatus)
                .cname(vo.getCname())
                .businessType(BusinessTypeEnum.WEB.getParam())
                .serviceArea(ServiceAreaEnum.OUTSIDE_MAINLAND_CHINA.getParam())
                .isIpv6(isIpv6)
                .createTime(KuocaiDateUtil.strToDate(vo.getCreatedDate()))
                .updateTime(KuocaiDateUtil.strToDate(vo.getLastModified()))
                .sourceStationPrimaryInfo(sourceStationPrimaryInfo)
                .sourceStationStandbyInfo(sourceStationStandbyInfo)
                .build();
        String originProtocol = OriginProtocolEnum.HTTP.getParam();
        String originReceiveTimeout = "30";
        String originRangeStatus = "off";
        // 两个 header
        ArrayList<DomainBackSourceInfo.BackSourceRequestInfo> backSourceRequestInfos = new ArrayList<>();
        ArrayList<DomainAdvancedInfo.HttpResponseHeader> httpResponseHeaders = new ArrayList<>();
        DomainBackSourceInfo domainBackSourceInfo = DomainBackSourceInfo.builder()
                .origin_protocol(originProtocol)
                .port(80)  // 无该配置
                .origin_receive_timeout(originReceiveTimeout)
                .origin_range_status(originRangeStatus)
                .slice_etag_status("off")  // 无该配置
                .origin_request_url_rewrite(new ArrayList<>())  // 无该配置
                .flexible_origin(new ArrayList<>())  // 无该配置
                .origin_request_header(backSourceRequestInfos)
                .build();
        ArrayList<DomainCacheInfo.CacheRule> cacheRules = new ArrayList<>();
        ArrayList<DomainCacheInfo.ErrorCodeCache> errorCodeCaches = new ArrayList<>();
        DomainCacheInfo domainCacheInfo = DomainCacheInfo.builder()
                .cache_rules(cacheRules).error_code_cache(errorCodeCaches)
                .build();
        DomainVisitInfo.Referer referer = DomainVisitInfo.Referer.builder()
                .type("off").referer_type(0).value("").include_empty(false)
                .build();
        DomainVisitInfo.IpFilter ipFilter = DomainVisitInfo.IpFilter.builder()
                .type("off").value("")
                .build();
        DomainVisitInfo.UserAgentFilter userAgentFilter = DomainVisitInfo.UserAgentFilter.builder()
                .type("off").value("")
                .build();
        DomainVisitInfo domainVisitInfo = DomainVisitInfo.builder()
                .referer(referer).ip_filter(ipFilter).user_agent_filter(userAgentFilter)
                .build();
        ArrayList<DomainAdvancedInfo.ErrorCodeRedirectRules> errorCodeRedirectRules = new ArrayList<>();
        DomainAdvancedInfo.Compress compress = DomainAdvancedInfo.Compress.builder()
                .status("off").type("").file_type("")
                .build();
        DomainAdvancedInfo domainAdvancedInfo = DomainAdvancedInfo.builder()
                .http_response_header(httpResponseHeaders).error_code_redirect_rules(errorCodeRedirectRules).compress(compress)
                .build();
        DomainHttpsInfo.HttpGetBody httpGetBody = DomainHttpsInfo.HttpGetBody.builder()
                .https_status("off").certificate_name("").certificate_value("").expire_time(0L).certificate_source(0).certificate_type("").http2_status("off").tls_version("").ocsp_stapling_status("off").certId(0)
                .build();
        DomainHttpsInfo.ForceRedirect forceRedirect = DomainHttpsInfo.ForceRedirect.builder()
                .status("off").type("https").redirect_code("301")
                .build();
        DomainHttpsInfo domainHttpsInfo = DomainHttpsInfo.builder()
                .https(httpGetBody).force_redirect(forceRedirect)
                .build();

        return DomainConfig.builder()
                .domainBasicInfo(domainBasicInfo)
                .domainBackSourceInfo(domainBackSourceInfo)
                .domainCacheInfo(domainCacheInfo)
                .domainVisitInfo(domainVisitInfo)
                .domainAdvancedInfo(domainAdvancedInfo)
                .domainHttpsInfo(domainHttpsInfo)
                .build();
    }

    public DomainBasicInfo getDomainBasicInfo(String domainName) throws CdnetworksException {
        BasicDomainDTO basicDomainDTO = new BasicDomainDTO(domainName);
        BasicDomainVO basicDomainVO = CdnetworksClient.BasicDomain(basicDomainDTO);
        String httpsStatus = "0";
        String isIpv6 = "0";
        BasicDomainVO.Ssl ssl = basicDomainVO.getSsl();
        if (null != ssl) {
            if ("true".equals(ssl.getUseSsl())) {
                httpsStatus = "1";
            }
        }
        DomainBasicInfo.SourceStationPrimaryInfo sourceStationPrimaryInfo = DomainBasicInfo.SourceStationPrimaryInfo.builder()
                .sourceStationType(OriginTypeEnum.IPADDR.getParam())
                .ipOrDomain("").httpPort("80").httpsPort("443").sourceHost(basicDomainVO.getDomainName())
                .build();
        BasicDomainVO.OriginConfig originConfig = basicDomainVO.getOriginConfig();
        sourceStationPrimaryInfo.setIpOrDomain(originConfig.getOriginIps());
        if (Assert.isEmpty(originConfig.getOriginPort())) {
            sourceStationPrimaryInfo.setHttpPort("0");
        } else {
            sourceStationPrimaryInfo.setHttpPort(originConfig.getOriginPort());
        }
        String defaultOriginHostHeader = originConfig.getDefaultOriginHostHeader();
        if (Assert.notEmpty(defaultOriginHostHeader)) {
            sourceStationPrimaryInfo.setSourceHost(defaultOriginHostHeader);
        }
        DomainBasicInfo.SourceStationStandbyInfo sourceStationStandbyInfo = DomainBasicInfo.SourceStationStandbyInfo.builder()
                .sourceStationType("").ipOrDomain("").httpPort("").httpsPort("").sourceHost("")
                .build();
        return DomainBasicInfo.builder()
                .domainName(domainName)
                .domainStatus(convertDomainStatus(basicDomainVO.getStatus(), basicDomainVO.getEnabled()))
                .httpsStatus(httpsStatus)
                .cname(basicDomainVO.getCname())
                .businessType(BusinessTypeEnum.WEB.getParam())
                .serviceArea(ServiceAreaEnum.OUTSIDE_MAINLAND_CHINA.getParam())
                .isIpv6(isIpv6)
                .createTime(KuocaiDateUtil.strToDate(basicDomainVO.getCreatedDate()))
                .updateTime(KuocaiDateUtil.strToDate(basicDomainVO.getLastModified()))
                .sourceStationPrimaryInfo(sourceStationPrimaryInfo)
                .sourceStationStandbyInfo(sourceStationStandbyInfo)
                .build();
    }

    @Override
    public DomainBackSourceInfo getDomainBackSourceInfo(String domainName) {
        QueryHttpHeaderDTO queryHttpHeaderDTO = new QueryHttpHeaderDTO(domainName);
        log.info("开始获取加速域名配置");
        CompletableFuture<QueryHttpHeaderVO> queryHttpHeaderVOCompletableFuture = executeAsync(() -> {
            try {
                return CdnetworksClient.QueryHttpHeader(queryHttpHeaderDTO);
            } catch (CdnetworksException e) {
                log.error("获取加速域名配置失败");
                throw new RuntimeException(e);
            }
        });
        QueryOriginProtocolDTO queryOriginProtocolDTO = new QueryOriginProtocolDTO(domainName);
        CompletableFuture<QueryOriginProtocolVO> queryOriginProtocolVOCompletableFuture = executeAsync(() -> {
            try {
                return CdnetworksClient.QueryOriginProtocol(queryOriginProtocolDTO);
            } catch (CdnetworksException e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture.allOf(queryHttpHeaderVOCompletableFuture, queryOriginProtocolVOCompletableFuture).join();

        QueryHttpHeaderVO queryHttpHeaderVO = null;
        QueryOriginProtocolVO queryOriginProtocolVO = null;
        try {
            queryHttpHeaderVO = queryHttpHeaderVOCompletableFuture.get();
            queryOriginProtocolVO = queryOriginProtocolVOCompletableFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("获取加速域名配置失败", e);
        }

        String originProtocol = OriginProtocolEnum.FOLLOW.getParam();
        String originReceiveTimeout = "30";
        String originRangeStatus = "off";
        ArrayList<DomainBackSourceInfo.BackSourceRequestInfo> backSourceRequestInfos = new ArrayList<>();

        if (null != queryHttpHeaderVO) {
            QueryHttpHeaderVO.HeaderModifyRule[] headerModifyRules = queryHttpHeaderVO.getHeaderModifyRules();
            for (QueryHttpHeaderVO.HeaderModifyRule headerModifyRule : headerModifyRules) {
                if ("cache2origin".equals(headerModifyRule.getHeaderDirection())) {
                    DomainBackSourceInfo.BackSourceRequestInfo backSourceRequestInfo = DomainBackSourceInfo.BackSourceRequestInfo.builder()
                            .name(headerModifyRule.getHeaderName())
                            .value(null == headerModifyRule.getHeaderValue() ? "" : headerModifyRule.getHeaderValue())
                            .action(convertHeaderAction(headerModifyRule.getAction()))
                            .build();
                    backSourceRequestInfos.add(backSourceRequestInfo);
                }
            }
        }

        if (null != queryOriginProtocolVO) {
            QueryOriginProtocolVO.OriginProtocolData originProtocolData = queryOriginProtocolVO.getData();
            String protocol = originProtocolData.getBackToOriginRewriteRule().getProtocol();
            if ("http".equals(protocol)) {
                originProtocol = OriginProtocolEnum.HTTP.getParam();
            } else if ("https".equals(protocol)) {
                originProtocol = OriginProtocolEnum.HTTPS.getParam();
            }
        }

        return DomainBackSourceInfo.builder()
                .origin_protocol(originProtocol)
                .port(80)  // 无该配置
                .origin_receive_timeout(originReceiveTimeout)
                .origin_range_status(originRangeStatus)
                .slice_etag_status("off")  // 无该配置
                .origin_request_url_rewrite(new ArrayList<>())  // 无该配置
                .flexible_origin(new ArrayList<>())  // 无该配置
                .origin_request_header(backSourceRequestInfos)
                .build();
    }

    @Override
    public DomainCacheInfo getDomainCacheInfo(String domainName) {
        ArrayList<DomainCacheInfo.CacheRule> cacheRules = new ArrayList<>();
        ArrayList<DomainCacheInfo.ErrorCodeCache> errorCodeCaches = new ArrayList<>();

        QueryCacheTimeDTO queryCacheTimeDTO = new QueryCacheTimeDTO(domainName);
        CompletableFuture<QueryCacheTimeVO> queryCacheTimeVOCompletableFuture = executeAsync(() -> {
            try {
                return CdnetworksClient.QueryCacheTime(queryCacheTimeDTO);
            } catch (CdnetworksException e) {
                throw new RuntimeException(e);
            }
        });
        QueryHttpCodeCacheDTO queryHttpCodeCacheDTO = new QueryHttpCodeCacheDTO(domainName);
        CompletableFuture<QueryHttpCodeCacheVO> queryHttpCodeCacheVOCompletableFuture = executeAsync(() -> {
            try {
                return CdnetworksClient.QueryHttpCodeCacheConfig(queryHttpCodeCacheDTO);
            } catch (CdnetworksException e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture.allOf(queryCacheTimeVOCompletableFuture, queryHttpCodeCacheVOCompletableFuture).join();

        QueryCacheTimeVO queryCacheTimeVO = null;
        QueryHttpCodeCacheVO queryHttpCodeCacheVO = null;
        try {
            queryCacheTimeVO = queryCacheTimeVOCompletableFuture.get();
            queryHttpCodeCacheVO = queryHttpCodeCacheVOCompletableFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("获取加速域名配置失败", e);
        }

        if (null != queryCacheTimeVO) {
            QueryCacheTimeVO.CacheTimeBehavior[] cacheTimeBehaviors = queryCacheTimeVO.getCacheTimeBehaviors();
            if (Assert.notEmpty(cacheTimeBehaviors)) {
                Pattern ttlPattern = Pattern.compile("^(\\d+)([smhd])$");
                for (QueryCacheTimeVO.CacheTimeBehavior cacheTimeBehavior : cacheTimeBehaviors) {
                    DomainCacheInfo.CacheRule cacheRule = DomainCacheInfo.CacheRule.builder()
                            .match_value("").ttl(0).ttl_unit("s").follow_origin("off").url_parameter_type("").url_parameter_value("")
                            .build();
                    String customPattern = cacheTimeBehavior.getCustomPattern();
                    String directory = cacheTimeBehavior.getDirectory();
                    String specifyUrlPattern = cacheTimeBehavior.getSpecifyUrlPattern();
                    String filetype = cacheTimeBehavior.getFiletype();
                    if (Assert.notEmpty(customPattern)) {
                        if ("homepage".equals(customPattern)) {
                            cacheRule.setMatch_type("home_page");
                        } else {
                            cacheRule.setMatch_type("all");
                        }
                    } else if (Assert.notEmpty(directory)) {
                        cacheRule.setMatch_type("catalog");
                        cacheRule.setMatch_value(directory);
                    } else if (Assert.notEmpty(specifyUrlPattern)) {
                        cacheRule.setMatch_type("full_path");
                        cacheRule.setMatch_value(specifyUrlPattern);
                    } else if (Assert.notEmpty(filetype)) {
                        cacheRule.setMatch_type("file_extension");
                        cacheRule.setMatch_value("." + filetype.replace(";", ";."));
                    } else { // 兜底
                        cacheRule.setMatch_type("all");
                    }
                    cacheRule.setFollow_origin("true".equals(cacheTimeBehavior.getIsRespectServer()) ? "on" : "off");
                    Matcher matcher = ttlPattern.matcher(cacheTimeBehavior.getCacheTtl());
                    if (matcher.matches()) {
                        cacheRule.setTtl(Integer.valueOf(matcher.group(1)));
                        cacheRule.setTtl_unit(matcher.group(2));
                    }
                    cacheRules.add(cacheRule);
                }
            }
            Collections.reverse(cacheRules);
        }

        if (null != queryHttpCodeCacheVO) {
            QueryHttpCodeCacheVO.HttpCodeCacheRule[] httpCodeCacheRules = queryHttpCodeCacheVO.getHttpCodeCacheRules();
            if (Assert.notEmpty(httpCodeCacheRules)) {
                for (QueryHttpCodeCacheVO.HttpCodeCacheRule httpCodeCacheRule : httpCodeCacheRules) {
                    DomainCacheInfo.ErrorCodeCache errorCodeCache = DomainCacheInfo.ErrorCodeCache.builder()
                            .code(Integer.valueOf(httpCodeCacheRule.getHttpCodes()[0]))
                            .ttl(Integer.valueOf(httpCodeCacheRule.getCacheTtl()))
                            .build();
                    errorCodeCaches.add(errorCodeCache);
                }
            }
        }

        return DomainCacheInfo.builder()
                .cache_rules(cacheRules).error_code_cache(errorCodeCaches)
                .build();
    }

    @Override
    public DomainHttpsInfo getDomainHttpsInfo(String domainName) {
        QueryDomainCertDTO queryDomainCertDTO = new QueryDomainCertDTO(domainName);
        CompletableFuture<QueryDomainCertVO> queryDomainCertVOCompletableFuture = executeAsync(() -> {
            try {
                return CdnetworksClient.QueryDomainCert(queryDomainCertDTO);
            } catch (CdnetworksException e) {
                throw new RuntimeException(e);
            }
        });
        QueryHttp2SettingsDTO queryHttp2SettingsDTO = new QueryHttp2SettingsDTO(domainName);
        CompletableFuture<QueryHttp2SettingsVO> queryHttp2SettingsVOCompletableFuture = executeAsync(() -> {
            try {
                return CdnetworksClient.QueryHttp2Settings(queryHttp2SettingsDTO);
            } catch (CdnetworksException e) {
                throw new RuntimeException(e);
            }
        });
        QueryInnerRedirectDTO queryInnerRedirectDTO = new QueryInnerRedirectDTO(domainName);
        CompletableFuture<QueryInnerRedirectVO> queryInnerRedirectVOCompletableFuture = executeAsync(() -> {
            try {
                return CdnetworksClient.QueryInnerRedirect(queryInnerRedirectDTO);
            } catch (CdnetworksException e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture.allOf(queryDomainCertVOCompletableFuture, queryHttp2SettingsVOCompletableFuture, queryInnerRedirectVOCompletableFuture).join();

        QueryDomainCertVO queryDomainCertVO = null;
        QueryHttp2SettingsVO queryHttp2SettingsVO = null;
        QueryInnerRedirectVO queryInnerRedirectVO = null;
        try {
            queryDomainCertVO = queryDomainCertVOCompletableFuture.get();
            queryHttp2SettingsVO = queryHttp2SettingsVOCompletableFuture.get();
            queryInnerRedirectVO = queryInnerRedirectVOCompletableFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("获取加速域名配置失败", e);
        }

        DomainHttpsInfo.HttpGetBody httpGetBody = DomainHttpsInfo.HttpGetBody.builder()
                .https_status("off").certificate_name("").certificate_value("").expire_time(0L).certificate_source(0).certificate_type("").http2_status("off").tls_version("").ocsp_stapling_status("off").certId(0)
                .build();

        if (null != queryDomainCertVO) {
            QueryDomainCertVO.CertData certData = queryDomainCertVO.getData();
            if (Assert.notEmpty(certData.getCertificateId())) {
                httpGetBody.setHttps_status("on");
                httpGetBody.setCertificate_name(certData.getCertificateId());
                httpGetBody.setCertId(Integer.valueOf(certData.getCertificateId()));
                httpGetBody.setOcsp_stapling_status("true".equals(certData.getEnableOCSP()) ? "on" : "off");
            }
        }

        if (null != queryHttp2SettingsVO) {
            QueryHttp2SettingsVO.Http2SettingsData http2SettingsData = queryHttp2SettingsVO.getData();
            if (null != http2SettingsData && Assert.notEmpty(http2SettingsData.getHttp2Settings())) {
                httpGetBody.setHttp2_status("true".equals(http2SettingsData.getHttp2Settings().getEnableHttp2()) ? "on" : "off");
            }
        }

        DomainHttpsInfo.ForceRedirect forceRedirect = DomainHttpsInfo.ForceRedirect.builder()
                .status("off").type("https").redirect_code("301")
                .build();

        if (null != queryInnerRedirectVO) {
            QueryInnerRedirectVO.RewriteRuleSetting[] rewriteRuleSettings = queryInnerRedirectVO.getRewriteRuleSettings();
            for (QueryInnerRedirectVO.RewriteRuleSetting rewriteRuleSetting : rewriteRuleSettings) {
                String afterValue = rewriteRuleSetting.getAfterValue();
                if (afterValue.startsWith("301")) {
                    forceRedirect.setRedirect_code("301");
                } else if (afterValue.startsWith("302")) {
                    forceRedirect.setRedirect_code("302");
                }
                if (QueryInnerRedirectVO.isRewriteToHttps(rewriteRuleSetting)) {
                    forceRedirect.setStatus("on");
                    forceRedirect.setType("HTTPS");
                    break;
                }
                if (QueryInnerRedirectVO.isRewriteToHttp(rewriteRuleSetting)) {
                    forceRedirect.setStatus("on");
                    forceRedirect.setType("HTTP");
                    break;
                }
            }
        }

        return DomainHttpsInfo.builder()
                .https(httpGetBody).force_redirect(forceRedirect)
                .build();
    }

    @Override
    public DomainVisitInfo getDomainVisitInfo(String domainName) {
        QueryAntiHotlinkingDTO queryAntiHotlinkingDTO = new QueryAntiHotlinkingDTO(domainName);
        CompletableFuture<QueryAntiHotlinkingVO> queryAntiHotlinkingVOCompletableFuture = executeAsync(() -> {
            try {
                return CdnetworksClient.QueryAntiHotlinking(queryAntiHotlinkingDTO);
            } catch (CdnetworksException e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture.allOf(queryAntiHotlinkingVOCompletableFuture).join();

        QueryAntiHotlinkingVO queryAntiHotlinkingVO = null;
        try {
            queryAntiHotlinkingVO = queryAntiHotlinkingVOCompletableFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("获取加速域名配置失败", e);
        }

        DomainVisitInfo.Referer referer = DomainVisitInfo.Referer.builder()
                .type("off").referer_type(0).value("").include_empty(false)
                .build();
        DomainVisitInfo.IpFilter ipFilter = DomainVisitInfo.IpFilter.builder()
                .type("off").value("")
                .build();
        DomainVisitInfo.UserAgentFilter userAgentFilter = DomainVisitInfo.UserAgentFilter.builder()
                .type("off").value("")
                .build();

        if (null != queryAntiHotlinkingVO) {
            QueryAntiHotlinkingVO.VisitControlRule[] visitControlRules = queryAntiHotlinkingVO.getVisitControlRules();
            if (Assert.notEmpty(visitControlRules)) {
                for (QueryAntiHotlinkingVO.VisitControlRule visitControlRule : visitControlRules) {
                    QueryAntiHotlinkingVO.VisitControlRuleIpControlRule ipControlRule = visitControlRule.getIpControlRule();
                    QueryAntiHotlinkingVO.VisitControlRuleRefererControlRule refererControlRule = visitControlRule.getRefererControlRule();
                    QueryAntiHotlinkingVO.VisitControlRuleUaControlRule uaControlRule = visitControlRule.getUaControlRule();
                    if (isIpControlRule(ipControlRule)) {
                        if (Assert.notEmpty(ipControlRule.getAllowedIps())) {
                            ipFilter.setType("white");
                            ipFilter.setValue(ipControlRule.getAllowedIps().replaceAll(";", "\n"));
                        } else {
                            ipFilter.setType("black");
                            ipFilter.setValue(ipControlRule.getForbiddenIps().replaceAll(";", "\n"));
                        }
                    } else if (isRefererControlRule(refererControlRule)) {
                        referer.setInclude_empty("true".equals(refererControlRule.getAllowNullReferer()));
                        if (Assert.notEmpty(refererControlRule.getValidDomain())) {
                            referer.setType("white");
                            referer.setReferer_type(2);
                            referer.setValue(refererControlRule.getValidDomain().replaceAll(";", "\n"));
                        } else {
                            referer.setType("black");
                            referer.setReferer_type(1);
                            referer.setValue(refererControlRule.getInvalidDomain().replaceAll(";", "\n"));
                        }
                    } else if (isUaControlRule(uaControlRule)) {
                        if (Assert.notEmpty(uaControlRule.getValidUserAgents())) {
                            userAgentFilter.setType("white");
                            userAgentFilter.setValue(uaControlRule.getValidUserAgents().replaceAll(";", "\n"));
                        } else {
                            userAgentFilter.setType("black");
                            userAgentFilter.setValue(uaControlRule.getInvalidUserAgents().replaceAll(";", "\n"));
                        }
                    }
                }
            }
        }

        return DomainVisitInfo.builder()
                .referer(referer).ip_filter(ipFilter).user_agent_filter(userAgentFilter)
                .build();
    }

    @Override
    public DomainAdvancedInfo getDomainAdvancedInfo(String domainName) {
        QueryHttpHeaderDTO queryHttpHeaderDTO = new QueryHttpHeaderDTO(domainName);
        CompletableFuture<QueryHttpHeaderVO> queryHttpHeaderVOCompletableFuture = executeAsync(() -> {
            try {
                return CdnetworksClient.QueryHttpHeader(queryHttpHeaderDTO);
            } catch (CdnetworksException e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture.allOf(queryHttpHeaderVOCompletableFuture).join();

        QueryHttpHeaderVO queryHttpHeaderVO = null;
        try {
            queryHttpHeaderVO = queryHttpHeaderVOCompletableFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("获取加速域名配置失败", e);
        }

        ArrayList<DomainAdvancedInfo.HttpResponseHeader> httpResponseHeaders = new ArrayList<>();
        if (null != queryHttpHeaderVO) {
            QueryHttpHeaderVO.HeaderModifyRule[] headerModifyRules = queryHttpHeaderVO.getHeaderModifyRules();
            for (QueryHttpHeaderVO.HeaderModifyRule headerModifyRule : headerModifyRules) {
                if ("Ws-Hdr".equals(headerModifyRule.getHeaderName()) || "PWS/8.3.1.0.8".equals(headerModifyRule.getHeaderValue())) {
                    continue;
                }
                if ("cache2visitor".equals(headerModifyRule.getHeaderDirection())) {
                    DomainAdvancedInfo.HttpResponseHeader httpResponseHeader = DomainAdvancedInfo.HttpResponseHeader.builder()
                            .name(headerModifyRule.getHeaderName())
                            .value(null == headerModifyRule.getHeaderValue() ? "" : headerModifyRule.getHeaderValue())
                            .action(convertHeaderAction(headerModifyRule.getAction()))
                            .build();
                    httpResponseHeaders.add(httpResponseHeader);
                }
            }
        }

        ArrayList<DomainAdvancedInfo.ErrorCodeRedirectRules> errorCodeRedirectRules = new ArrayList<>();
        DomainAdvancedInfo.Compress compress = DomainAdvancedInfo.Compress.builder()
                .status("off").type("").file_type("")
                .build();
        return DomainAdvancedInfo.builder()
                .http_response_header(httpResponseHeaders).error_code_redirect_rules(errorCodeRedirectRules).compress(compress)
                .build();
    }

    @Override
    public DomainConfig getDomainBasicConfig(String domainName) throws BusinessException {
        CompletableFuture<DomainBasicInfo> domainBasicInfoCompletableFuture = executeAsync(() -> {
            try {
                return getDomainBasicInfo(domainName);
            } catch (CdnetworksException e) {
                throw new RuntimeException(e);
            }
        });
        CompletableFuture<DomainBackSourceInfo> domainBackSourceInfoCompletableFuture = executeAsync(() -> getDomainBackSourceInfo(domainName));

        CompletableFuture.allOf(domainBasicInfoCompletableFuture, domainBackSourceInfoCompletableFuture).join();

        DomainBasicInfo domainConfig;
        DomainBackSourceInfo domainBackSourceInfo;
        try {
            domainConfig = domainBasicInfoCompletableFuture.get();
            domainBackSourceInfo = domainBackSourceInfoCompletableFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("获取加速域名配置失败", e);
            throw new BusinessException(e.getMessage());
        }

        return DomainConfig.builder()
                .domainBasicInfo(domainConfig)
                .domainBackSourceInfo(domainBackSourceInfo)
                .build();
    }

    private boolean isIpControlRule(QueryAntiHotlinkingVO.VisitControlRuleIpControlRule rule) {
        return null != rule && (Assert.notEmpty(rule.getAllowedIps()) || Assert.notEmpty(rule.getForbiddenIps()));
    }

    private boolean isRefererControlRule(QueryAntiHotlinkingVO.VisitControlRuleRefererControlRule rule) {
        List<String> check = Arrays.asList("true", "false");
        return null != rule && check.contains(rule.getAllowNullReferer());
    }

    private boolean isUaControlRule(QueryAntiHotlinkingVO.VisitControlRuleUaControlRule rule) {
        return null != rule && (Assert.notEmpty(rule.getInvalidUserAgents()) || Assert.notEmpty(rule.getValidUserAgents()));
    }

    public String convertHeaderAction(String action) {
        if (action.equals("delete")) {
            return "delete";
        }
        return "set";
    }

    public String convertDomainStatus(String status, String enabled) {
        String domainStatus = DomainStatus.CONFIGURING.getParam();
        if ("Deployed".equals(status)) {
            if ("true".equals(enabled)) {
                return DomainStatus.ONLINE.getParam();
            }
            return DomainStatus.OFFLINE.getParam();
        }
        return domainStatus;
    }
}
