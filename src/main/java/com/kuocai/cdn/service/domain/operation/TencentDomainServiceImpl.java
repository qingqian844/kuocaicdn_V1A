package com.kuocai.cdn.service.domain.operation;

import cn.hutool.core.util.RandomUtil;
import com.kuocai.cdn.api.*;
import com.kuocai.cdn.api.huawei.cdn.dto.*;
import com.kuocai.cdn.api.tencent.cdn.TencentClient;
import com.kuocai.cdn.api.tencent.cdn.TencentErrorCodeHandler;
import com.kuocai.cdn.api.tencent.cdn.enumeration.TencentDomainStatus;
import com.kuocai.cdn.api.tencent.dns.CreateRecordResponse;
import com.kuocai.cdn.api.tencent.dns.TencentApi;
import com.kuocai.cdn.api.tencent.dns.dto.CreateRecordDTO;
import com.kuocai.cdn.api.tencent.dns.properties.TencentDns;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.entity.CdnDomainSources;
import com.kuocai.cdn.enumeration.domainmerage.CdnRoute;
import com.kuocai.cdn.enumeration.domainmerage.domain.*;
import com.kuocai.cdn.enumeration.domainmerage.route.CdnOperationRoute;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.service.domain.operation.optional.ICdnDomainVerifyService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.DomainUtil;
import com.kuocai.cdn.util.KuocaiBaseUtil;
import com.kuocai.cdn.util.KuocaiDateUtil;
import com.kuocai.cdn.vo.*;
import com.tencentcloudapi.cdn.v20180606.CdnClient;
import com.tencentcloudapi.cdn.v20180606.models.*;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TencentDomainServiceImpl extends BaseService<CdnDomain> implements ICdnPlatformService, ICdnDomainVerifyService {

    @Override
    public DomainVerifyRecordInfo createVerifyRecord(String domainName) throws BusinessException {
        CdnClient cdn = TencentClient.getCdnClient();
        try {
            CreateVerifyRecordRequest req = new CreateVerifyRecordRequest();
            req.setDomain(domainName);
            CreateVerifyRecordResponse resp = cdn.CreateVerifyRecord(req);
            return DomainVerifyRecordInfo.builder()
                    .subDomain(resp.getSubDomain())
                    .record(resp.getRecord())
                    .recordType(resp.getRecordType())
                    .fileVerifyUrl(resp.getFileVerifyUrl())
                    .fileVerifyDomains(resp.getFileVerifyDomains())
                    .fileVerifyName(resp.getFileVerifyName())
                    .content(resp.getRecord())
                    .build();
        } catch (Exception e) {
            throw new BusinessException(e.getMessage());
        }
    }

    @Override
    public void verifyDomainRecord(String domainName, String verifyType) throws BusinessException {
        CdnClient cdn = TencentClient.getCdnClient();
        try {
            VerifyDomainRecordRequest req = new VerifyDomainRecordRequest();
            req.setDomain(domainName);
            req.setVerifyType(verifyType);
            VerifyDomainRecordResponse resp = cdn.VerifyDomainRecord(req);
            if (resp.getResult()) {
                log.info("域名 {} 验证成功", domainName);
            } else {  // 失败应该抛出异常
                log.error("域名 {} 验证失败返回：{}", domainName, resp.getResult());
            }
        } catch (Exception e) {
            log.error("域名 {} 验证失败：{}", domainName, e.getMessage());
            throw new BusinessException(e.getMessage());
        }
    }

    @Override
    public CdnDomain create(Long userId, String domainName, String businessType, String serviceArea, String originType, String ipOrDomain) throws BusinessException, InterruptedException {
        return create(userId, domainName, businessType, serviceArea, originType, ipOrDomain, "http");
    }

    private CdnDomain create(Long userId, String domainName, String businessType, String serviceArea, String originType, String ipOrDomain, String originProtocol) throws BusinessException, InterruptedException {
        try {
            // ## 创建域名
            AddCdnDomainRequest req = new AddCdnDomainRequest();
            // 域名
            req.setDomain(domainName);
            // 源站类型 加速域名业务类型 web：网页小文件 download：下载大文件 media：音视频点播 hybrid: 动静加速 dynamic: 动态加速
            req.setServiceType(BusinessTypeEnum.getOtherParam(businessType).getTencent());
            // 域名加速区域 mainland：中国境内加速 overseas：中国境外加速 global：全球加速 使用中国境外加速、全球加速时，需要先开通中国境外加速服务
            req.setArea(ServiceAreaEnum.getOtherParam(serviceArea).getTencent());
            // 源站类型：ipaddr：IP源站 domain：域名源站
            Origin origin = new Origin();
            origin.setOriginType(OriginTypeEnum.getOtherParam(originType).getTencent());
            // 源站协议：http：回源站强制使用HTTP协议 https：回源站强制使用HTTPS协议 follow：回源协议跟随协议
            origin.setOriginPullProtocol(OriginProtocolEnum.getOtherParam(normalizeTencentOriginProtocol(originProtocol)).getTencent());
            origin.setOrigins(splitTencentOrigins(ipOrDomain));
            req.setOrigin(origin);
            // ## 默认缓存配置
//            Cache cache = new Cache();
            // 全部文件缓存 30天
//            RuleCache ruleCache = new RuleCache();
//            ruleCache.setRuleType("all");
//            ruleCache.setRulePaths(new String[]{"*"});
//            RuleCacheConfig ruleCacheConfig = new RuleCacheConfig();
//            CacheConfigCache cacheConfigCache = new CacheConfigCache();
//            cacheConfigCache.setSwitch("on");
//            cacheConfigCache.setCacheTime(KuocaiBaseUtil.toSeconds(30, "d"));
//            cacheConfigCache.setCompareMaxAge("on");
//            cacheConfigCache.setIgnoreCacheControl("off");
//            cacheConfigCache.setIgnoreSetCookie("off");
//            ruleCacheConfig.setCache(cacheConfigCache);
//            ruleCache.setCacheConfig(ruleCacheConfig);
            // 设置缓存规则
//            cache.setRuleCache(new RuleCache[]{ruleCache});
//            req.setCache(cache);
            // 提交
            CdnClient cdn = TencentClient.getCdnClient();
            try {
                AddCdnDomainResponse resp = cdn.AddCdnDomain(req);
                log.info("创建域名 {} 成功：{}", domainName, AddCdnDomainResponse.toJsonString(resp));
            } catch (TencentCloudSDKException e) {
                log.error("创建域名 {} 失败：{} - {}", domainName, e.getErrorCode(), e.getMessage());
                String code = "创建时发生错误，";
                if ("UnauthorizedOperation.CdnDomainRecordNotVerified".equals(e.getErrorCode())) {
                    code += "域名解析未进行验证";
                } else {
                    code += TencentErrorCodeHandler.getErrorDescription(e);
                }
                throw new BusinessException(code + buildTencentErrorTrace(e));
            }
            // ## 获取域名信息
            BriefDomain domain = getBriefDomain(domainName);
            // ## cdn域名信息
            CdnDomain cdnDomain = CdnDomain.builder()
                    .userId(userId)
                    .domainName(domainName)
                    .businessType(businessType)
                    .serviceArea(serviceArea)
                    .domainId(domain.getResourceId())
                    .cnameTencent(domain.getCname())
                    .domainStatus(DomainStatus.getSelfParam(domain.getStatus(), CdnOperationRoute.TENCENT))
                    .route(CdnRoute.TENCENT.getCode())
                    .build();
            return save(cdnDomain);
        } catch (Exception e) {
            log.error("创建域名 {} 流程失败：{}", domainName, e.getMessage());
            throw new BusinessException(e.getMessage());
        }
    }

    @Override
    public CdnDomain create(Long userId, String domainName, String businessType, String serviceArea, String originType, String ipOrDomain,
                            String originProtocol, Integer httpPort, Integer httpsPort, String originHost, Integer originWeight) throws BusinessException, InterruptedException {
        String protocol = normalizeTencentOriginProtocol(originProtocol);
        String formattedOrigins = String.join(";", buildTencentOrigins(ipOrDomain, protocol, httpPort, httpsPort, null, false));
        CdnDomain cdnDomain = create(userId, domainName, businessType, serviceArea, originType, formattedOrigins, protocol);
        CdnDomainSources main = CdnDomainSources.builder()
                .originType(originType)
                .ipOrDomain(stripTencentOriginDecorations(ipOrDomain))
                .httpPort(defaultPort(httpPort, 80))
                .httpsPort(defaultPort(httpsPort, 443))
                .activeStandby(1)
                .hostName(originHost)
                .build();
        saveSourceStationConfig(cdnDomain, CdnDomainSourcesVo.builder()
                .main(main)
                .originProtocol(protocol)
                .build());
        return cdnDomain;
    }

    @Override
    public CdnDomain configDNS(CdnDomain cdnDomain) throws TencentCloudSDKException, BusinessException {
        String domainName = cdnDomain.getDomainName();
        String cnameTencent = cdnDomain.getCnameTencent();
        CreateRecordDTO createRecordDTO = new CreateRecordDTO();
        createRecordDTO.setDomain(TencentDns.LOCAL_DOMAIN_NAME).setSubDomain(DomainUtil.convertSubDomain(domainName)).setValue(cnameTencent);
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
        CdnClient cdn = TencentClient.getCdnClient();
        StopCdnDomainRequest req = new StopCdnDomainRequest();
        req.setDomain(cdnDomain.getDomainName());
        try {
            // 通常没有报错即为成功
            StopCdnDomainResponse resp = cdn.StopCdnDomain(req);
            log.info("停用域名 {} 成功：{}", cdnDomain.getDomainName(), StopCdnDomainResponse.toJsonString(resp));
        } catch (TencentCloudSDKException e) {
            log.error("停用域名 {} 失败：{} - {}", cdnDomain.getDomainName(), e.getErrorCode(), e.getMessage());
            throw new BusinessException("停用域名失败，" + TencentErrorCodeHandler.getErrorDescription(e));
        }
    }

    @Override
    public void enable(CdnDomain cdnDomain) throws BusinessException {
        CdnClient cdn = TencentClient.getCdnClient();
        StartCdnDomainRequest req = new StartCdnDomainRequest();
        req.setDomain(cdnDomain.getDomainName());
        try {
            StartCdnDomainResponse resp = cdn.StartCdnDomain(req);
            log.info("启用域名 {} 成功：{}", cdnDomain.getDomainName(), StartCdnDomainResponse.toJsonString(resp));
        } catch (TencentCloudSDKException e) {
            log.error("启用域名 {} 失败：{} - {}", cdnDomain.getDomainName(), e.getErrorCode(), e.getMessage());
            throw new BusinessException("启用域名失败，" + TencentErrorCodeHandler.getErrorDescription(e));
        }
    }

    @Override
    public void delete(CdnDomain cdnDomain) throws BusinessException {
        CdnClient cdn = TencentClient.getCdnClient();
        DeleteCdnDomainRequest req = new DeleteCdnDomainRequest();
        req.setDomain(cdnDomain.getDomainName());
        try {
            DeleteCdnDomainResponse resp = cdn.DeleteCdnDomain(req);
            log.info("删除域名 {} 成功：{}", cdnDomain.getDomainName(), DeleteCdnDomainResponse.toJsonString(resp));
        } catch (TencentCloudSDKException e) {
            log.error("删除域名 {} 失败：{} - {}", cdnDomain.getDomainName(), e.getErrorCode(), e.getMessage());
            throw new BusinessException("删除域名失败，" + TencentErrorCodeHandler.getErrorDescription(e));
        }
    }

    @Override
    public void ipv6(CdnDomain cdnDomain, Integer status) throws BusinessException {
        CdnClient cdn = TencentClient.getCdnClient();
        UpdateDomainConfigRequest req = new UpdateDomainConfigRequest();
        req.setDomain(cdnDomain.getDomainName());
        Ipv6Access ipv6Access = new Ipv6Access();
        ipv6Access.setSwitch(status == 1 ? "on" : "off");
        req.setIpv6Access(ipv6Access);
        try {
            UpdateDomainConfigResponse resp = cdn.UpdateDomainConfig(req);
            log.info("更新域名 {} ipv6 配置成功：{}", cdnDomain.getDomainName(), UpdateDomainConfigResponse.toJsonString(resp));
        } catch (TencentCloudSDKException e) {
            log.error("更新域名 {} ipv6 配置失败：{} - {}", cdnDomain.getDomainName(), e.getErrorCode(), e.getMessage());
            throw new BusinessException("更新域名 ipv6 配置失败，" + TencentErrorCodeHandler.getErrorDescription(e));
        }
    }

    @Override
    public void saveSourceStationConfig(CdnDomain cdnDomain, CdnDomainSourcesVo config) throws BusinessException {
        CdnClient cdn = TencentClient.getCdnClient();
        CdnDomainSources main = config.getMain();
        CdnDomainSources back = config.getBack();
        UpdateDomainConfigRequest req = new UpdateDomainConfigRequest();
        req.setDomain(cdnDomain.getDomainName());
        Origin origin = new Origin();
        origin.setOriginType(OriginTypeEnum.getOtherParam(main.getOriginType()).getTencent());
        String originProtocol = Assert.isEmpty(config.getOriginProtocol()) ? "http" : config.getOriginProtocol();
        origin.setOriginPullProtocol(OriginProtocolEnum.getOtherParam(originProtocol).getTencent());
        origin.setOrigins(main.getIpOrDomain().split(";"));
        origin.setServerName(main.getHostName());
        if (Assert.notEmpty(back)) {
            origin.setBackupOrigins(back.getIpOrDomain().split(";"));
            origin.setBackupOriginType(OriginTypeEnum.getOtherParam(back.getOriginType()).getTencent());
            origin.setBackupServerName(back.getHostName());
        }
        req.setOrigin(origin);
        try {
            UpdateDomainConfigResponse resp = cdn.UpdateDomainConfig(req);
            log.info("更新域名 {} 源站配置成功：{}", cdnDomain.getDomainName(), UpdateDomainConfigResponse.toJsonString(resp));
        } catch (TencentCloudSDKException e) {
            log.error("更新域名 {} 源站配置失败：{} - {}", cdnDomain.getDomainName(), e.getErrorCode(), e.getMessage());
            throw new BusinessException("更新源站配置失败，" + TencentErrorCodeHandler.getErrorDescription(e));
        }
    }

    @Override
    public void change(CdnDomain cdnDomain) throws BusinessException {
        CdnClient cdn = TencentClient.getCdnClient();
        // ## 获取域名配置信息
        BriefDomain domain = getBriefDomain(cdnDomain.getDomainName());
        Origin origin = domain.getOrigin();
        if (Assert.isEmpty(origin.getBackupOrigins())) {
            throw new BusinessException("加速域名未配置备源站");
        }
        String originType = origin.getOriginType();
        String[] origins = origin.getOrigins();
        String hostName = origin.getServerName();
        if (Assert.isEmpty(hostName)) {
            origin.setOrigins(origin.getBackupOrigins());
            origin.setBackupServerName(hostName);
        }
        origin.setOriginType(origin.getBackupOriginType());
        origin.setBackupOriginType(originType);
        origin.setOrigins(origin.getBackupOrigins());
        origin.setBackupOrigins(origins);
        // ## 更新配置
        UpdateDomainConfigRequest req = new UpdateDomainConfigRequest();
        req.setDomain(cdnDomain.getDomainName());
        req.setOrigin(origin);
        try {
            UpdateDomainConfigResponse resp = cdn.UpdateDomainConfig(req);
            log.info("更新域名 {} 切换主备配置成功：{}", cdnDomain.getDomainName(), UpdateDomainConfigResponse.toJsonString(resp));
        } catch (TencentCloudSDKException e) {
            log.error("更新域名 {} 切换主备配置失败：{} - {}", cdnDomain.getDomainName(), e.getErrorCode(), e.getMessage());
            throw new BusinessException("更新切换主备配置失败，" + TencentErrorCodeHandler.getErrorDescription(e));
        }
    }

    @Override
    public void saveOriginProtocol(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        CdnClient cdn = TencentClient.getCdnClient();
        // ## 获取域名配置信息
        BriefDomain domain = getBriefDomain(cdnDomain.getDomainName());
        Origin origin = domain.getOrigin();
        String originProtocol = OriginProtocolEnum.getOtherParam(domainOriginSettingVo.getOriginProtocol()).getTencent();
        origin.setOriginPullProtocol(originProtocol);
        // ## 更新配置
        UpdateDomainConfigRequest req = new UpdateDomainConfigRequest();
        req.setDomain(cdnDomain.getDomainName());
        req.setOrigin(origin);
        try {
            UpdateDomainConfigResponse resp = cdn.UpdateDomainConfig(req);
            log.info("更新域名 {} 回源协议配置成功：{}", cdnDomain.getDomainName(), UpdateDomainConfigResponse.toJsonString(resp));
        } catch (TencentCloudSDKException e) {
            log.error("更新域名 {} 回源协议配置失败：{} - {}", cdnDomain.getDomainName(), e.getErrorCode(), e.getMessage());
            throw new BusinessException("更新回源协议配置失败，" + TencentErrorCodeHandler.getErrorDescription(e));
        }
    }

    @Override
    public void saveOriginRequestUrlRewrite(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        // 无该配置
    }

    @Override
    public void saveAdvancedReturnSource(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        // 无该配置
    }

    @Override
    public void saveRangeSwitch(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        String switchStatus = domainOriginSettingVo.getStatus();
        RangeOriginPull rangeOriginPull = new RangeOriginPull();
        if ("off".equals(switchStatus)) {
            rangeOriginPull.setSwitch("off");
        } else {
            rangeOriginPull.setSwitch("on");
            RangeOriginPullRule rangeOriginPullRule = new RangeOriginPullRule();
            rangeOriginPullRule.setRuleType("file");
            // ## 静态资源后缀
            rangeOriginPullRule.setRulePaths(new String[]{"jpg", "png", "gif", "css", "js", "html", "htm", "xml", "txt", "mp4", "mp3", "avi", "flv", "swf", "ico", "woff", "woff2", "ttf", "eot", "svg"});
            rangeOriginPull.setRangeRules(new RangeOriginPullRule[]{rangeOriginPullRule});
        }
        CdnClient cdn = TencentClient.getCdnClient();
        UpdateDomainConfigRequest req = new UpdateDomainConfigRequest();
        req.setDomain(cdnDomain.getDomainName());
        req.setRangeOriginPull(rangeOriginPull);
        try {
            UpdateDomainConfigResponse resp = cdn.UpdateDomainConfig(req);
            log.info("更新域名 {} Range回源配置成功：{}", cdnDomain.getDomainName(), UpdateDomainConfigResponse.toJsonString(resp));
        } catch (TencentCloudSDKException e) {
            log.error("更新域名 {} Range回源配置失败：{} - {}", cdnDomain.getDomainName(), e.getErrorCode(), e.getMessage());
            throw new BusinessException("更新Range回源配置失败，" + TencentErrorCodeHandler.getErrorDescription(e));
        }
    }

    @Override
    public void saveRangeVerifyETag(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        // 无该配置
    }

    @Override
    public void saveOriginHost(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {

    }

    @Override
    public void saveRangeTimeOut(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        Integer originReceiveTimeOut = domainOriginSettingVo.getOriginReceiveTimeOut();
        if (Assert.isEmpty(originReceiveTimeOut)) {
            throw new BusinessException("参数异常");
        }
        CdnClient cdn = TencentClient.getCdnClient();
        OriginPullTimeout originPullTimeout = new OriginPullTimeout();
        originPullTimeout.setReceiveTimeout(Long.valueOf(originReceiveTimeOut));
        UpdateDomainConfigRequest req = new UpdateDomainConfigRequest();
        req.setDomain(cdnDomain.getDomainName());
        req.setOriginPullTimeout(originPullTimeout);
        try {
            UpdateDomainConfigResponse resp = cdn.UpdateDomainConfig(req);
            log.info("更新域名 {} 回源超时时间配置成功：{}", cdnDomain.getDomainName(), UpdateDomainConfigResponse.toJsonString(resp));
        } catch (TencentCloudSDKException e) {
            log.error("更新域名 {} 回源超时时间配置失败：{} - {}", cdnDomain.getDomainName(), e.getErrorCode(), e.getMessage());
            throw new BusinessException("更新回源超时时间配置失败，" + TencentErrorCodeHandler.getErrorDescription(e));
        }
    }

    @Override
    public void saveOriginRequestHeader(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        List<OriginRequestHeaderDTO> originRequestHeaders = domainOriginSettingVo.getOriginRequestHeader();
        RequestHeader originRequestHeader = new RequestHeader();
        if (Assert.isEmpty(originRequestHeaders)) {
            originRequestHeader.setSwitch("off");
        } else {
            originRequestHeader.setSwitch("on");
            ArrayList<HttpHeaderPathRule> httpHeaderPathRules = new ArrayList<>();
            for (OriginRequestHeaderDTO originRequestHeaderDTO : originRequestHeaders) {
                HttpHeaderPathRule httpHeaderPathRule = new HttpHeaderPathRule();
                httpHeaderPathRule.setHeaderMode("delete".equals(originRequestHeaderDTO.getAction()) ? "del" : "set");
                httpHeaderPathRule.setHeaderName(originRequestHeaderDTO.getName());
                httpHeaderPathRule.setHeaderValue(originRequestHeaderDTO.getValue());
                httpHeaderPathRule.setRuleType("all");
                httpHeaderPathRule.setRulePaths(new String[]{"*"});
                httpHeaderPathRules.add(httpHeaderPathRule);
            }
            originRequestHeader.setHeaderRules(httpHeaderPathRules.toArray(new HttpHeaderPathRule[0]));
        }
        CdnClient cdn = TencentClient.getCdnClient();
        UpdateDomainConfigRequest req = new UpdateDomainConfigRequest();
        req.setDomain(cdnDomain.getDomainName());
        req.setRequestHeader(originRequestHeader);
        try {
            UpdateDomainConfigResponse resp = cdn.UpdateDomainConfig(req);
            log.info("更新域名 {} 回源请求头配置成功：{}", cdnDomain.getDomainName(), UpdateDomainConfigResponse.toJsonString(resp));
        } catch (TencentCloudSDKException e) {
            log.error("更新域名 {} 回源请求头配置失败：{} - {}", cdnDomain.getDomainName(), e.getErrorCode(), e.getMessage());
            throw new BusinessException("更新回源请求头配置失败，" + TencentErrorCodeHandler.getErrorDescription(e));
        }
    }

    @Override
    public void httpsConfiguration(CdnDomain cdnDomain, DomainHttpsSettingVo config) throws BusinessException {
        HttpPutBodyDTO httpPutBodyDTO = config.getHttps();
        String httpsStatus = httpPutBodyDTO.getHttps_status();
        Https https = new Https();
        if ("on".equals(httpsStatus)) {
            String certificate = normalizePem(httpPutBodyDTO.getCertificate_value());
            String privateKey = normalizePem(httpPutBodyDTO.getPrivate_key());
            validateCertificateConfig(cdnDomain.getDomainName(), certificate, privateKey);

            https.setSwitch("on");
            ServerCert serverCert = new ServerCert();
            serverCert.setMessage(httpPutBodyDTO.getCertificate_name());
            serverCert.setCertificate(certificate);
            serverCert.setPrivateKey(privateKey);
            https.setCertInfo(serverCert);
        } else {
            https.setSwitch("off");
        }
        CdnClient cdn = TencentClient.getCdnClient();
        UpdateDomainConfigRequest req = new UpdateDomainConfigRequest();
        req.setDomain(cdnDomain.getDomainName());
        req.setHttps(https);
        try {
            UpdateDomainConfigResponse resp = cdn.UpdateDomainConfig(req);
            log.info("更新域名 {} Https 配置成功：{}", cdnDomain.getDomainName(), UpdateDomainConfigResponse.toJsonString(resp));
        } catch (TencentCloudSDKException e) {
            log.error("更新域名 {} Https 配置失败：{} - {}", cdnDomain.getDomainName(), e.getErrorCode(), e.getMessage());
            throw new BusinessException(buildTencentErrorMessage("更新 HTTPS 配置失败", e));
        }
    }

    private String normalizePem(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replace("\\r\\n", "\n").replace("\\n", "\n").replace("\r\n", "\n").replace('\r', '\n');
    }

    private void validateCertificateConfig(String domainName, String certificate, String privateKey) throws BusinessException {
        if (Assert.isEmpty(certificate) || !certificate.contains("-----BEGIN CERTIFICATE-----") || !certificate.contains("-----END CERTIFICATE-----")) {
            throw new BusinessException("证书内容格式不正确，请填写 PEM 格式证书并包含 BEGIN/END CERTIFICATE");
        }
        if (Assert.isEmpty(privateKey) || (!privateKey.contains("-----BEGIN PRIVATE KEY-----") && !privateKey.contains("-----BEGIN RSA PRIVATE KEY-----"))
                || (!privateKey.contains("-----END PRIVATE KEY-----") && !privateKey.contains("-----END RSA PRIVATE KEY-----"))) {
            throw new BusinessException("私钥内容格式不正确，请填写 PEM 格式私钥并包含 BEGIN/END PRIVATE KEY");
        }

        X509Certificate x509Certificate;
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            x509Certificate = (X509Certificate) certificateFactory.generateCertificate(
                    new ByteArrayInputStream(certificate.getBytes(StandardCharsets.US_ASCII)));
            x509Certificate.checkValidity();
        } catch (Exception e) {
            throw new BusinessException("证书内容无效或已过期：" + e.getMessage());
        }

        if (!certificateMatchesDomain(x509Certificate, domainName)) {
            throw new BusinessException("证书域名与当前加速域名不匹配，证书需包含：" + domainName);
        }
    }

    private boolean certificateMatchesDomain(X509Certificate certificate, String domainName) {
        String normalizedDomain = domainName == null ? "" : domainName.toLowerCase();
        try {
            Collection<List<?>> names = certificate.getSubjectAlternativeNames();
            if (names != null) {
                for (List<?> name : names) {
                    if (name.size() >= 2 && Integer.valueOf(2).equals(name.get(0)) && matchesCertificateName(String.valueOf(name.get(1)), normalizedDomain)) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        String commonName = getCertificateCommonName(certificate);
        return matchesCertificateName(commonName, normalizedDomain);
    }

    private String getCertificateCommonName(X509Certificate certificate) {
        try {
            LdapName ldapName = new LdapName(certificate.getSubjectX500Principal().getName());
            for (Rdn rdn : ldapName.getRdns()) {
                if ("CN".equalsIgnoreCase(rdn.getType())) {
                    return String.valueOf(rdn.getValue());
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private boolean matchesCertificateName(String certificateName, String domainName) {
        if (Assert.isEmpty(certificateName) || Assert.isEmpty(domainName)) {
            return false;
        }
        String name = certificateName.toLowerCase();
        if (name.equals(domainName)) {
            return true;
        }
        if (!name.startsWith("*.")) {
            return false;
        }
        String suffix = name.substring(1);
        return domainName.endsWith(suffix) && domainName.length() > suffix.length()
                && domainName.substring(0, domainName.length() - suffix.length()).indexOf('.') < 0;
    }

    private String buildTencentErrorMessage(String prefix, TencentCloudSDKException e) {
        String description = TencentErrorCodeHandler.getErrorDescription(e);
        String message = e.getMessage();
        String code = e.getErrorCode();
        StringBuilder builder = new StringBuilder(prefix);
        if (Assert.notEmpty(description)) {
            builder.append("，").append(description);
        }
        if (Assert.notEmpty(code) && (Assert.isEmpty(description) || !description.contains(code))) {
            builder.append("（").append(code).append("）");
        }
        if (Assert.notEmpty(message) && !message.equals(description)) {
            builder.append("：").append(message);
        }
        return builder.toString();
    }

    @Override
    public void httpsConfigurationOther(CdnDomain cdnDomain, DomainHttpsSettingVo config) throws BusinessException {
        HttpPutBodyDTO httpPutBodyDTO = config.getHttps();
        String http2Status = httpPutBodyDTO.getHttp2_status();
        String ocspStaplingStatus = httpPutBodyDTO.getOcsp_stapling_status();
        String tlsVersion = httpPutBodyDTO.getTls_version();
        // ## 获取域名配置信息
        DetailDomain domain = getDetailDomain(cdnDomain.getDomainName());
        Https https = domain.getHttps();
        // HTTP 2
        if (Assert.notEmpty(http2Status)) {
            if ("on".equals(http2Status)) {
                https.setHttp2("on");
            } else {
                https.setHttp2("off");
            }
        }
        // OCSP Stapling
        if (Assert.notEmpty(ocspStaplingStatus)) {
            if ("on".equals(ocspStaplingStatus)) {
                https.setOcspStapling("on");
            } else {
                https.setOcspStapling("off");
            }
        }
        // TLS 版本
        if (Assert.notEmpty(tlsVersion)) {
            https.setTlsVersion(normalizeTencentTlsVersions(tlsVersion));
        }
        CdnClient cdn = TencentClient.getCdnClient();
        UpdateDomainConfigRequest req = new UpdateDomainConfigRequest();
        req.setDomain(cdnDomain.getDomainName());
        req.setHttps(https);
        try {
            UpdateDomainConfigResponse resp = cdn.UpdateDomainConfig(req);
            log.info("更新域名 {} Https 配置成功：{}", cdnDomain.getDomainName(), UpdateDomainConfigResponse.toJsonString(resp));
        } catch (TencentCloudSDKException e) {
            log.error("更新域名 {} Https 配置失败：{} - {}", cdnDomain.getDomainName(), e.getErrorCode(), e.getMessage());
            throw new BusinessException("更新 Https 配置失败，" + TencentErrorCodeHandler.getErrorDescription(e));
        }
    }

    private String[] normalizeTencentTlsVersions(String tlsVersion) throws BusinessException {
        List<String> versions = new ArrayList<>();
        for (String item : tlsVersion.split("[,;]")) {
            String version = item.trim();
            if (version.isEmpty()) {
                continue;
            }
            if ("TLSv1.0".equalsIgnoreCase(version) || "TLSv1".equalsIgnoreCase(version)) {
                version = "TLSv1";
            } else if ("TLSv1.1".equalsIgnoreCase(version)) {
                version = "TLSv1.1";
            } else if ("TLSv1.2".equalsIgnoreCase(version)) {
                version = "TLSv1.2";
            } else if ("TLSv1.3".equalsIgnoreCase(version)) {
                version = "TLSv1.3";
            } else {
                throw new BusinessException("不支持的 TLS 版本：" + item);
            }
            if (!versions.contains(version)) {
                versions.add(version);
            }
        }
        if (versions.isEmpty()) {
            throw new BusinessException("请至少选择一个 TLS 版本");
        }

        List<String> order = Arrays.asList("TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3");
        int minIndex = order.size();
        int maxIndex = -1;
        for (String version : versions) {
            int index = order.indexOf(version);
            minIndex = Math.min(minIndex, index);
            maxIndex = Math.max(maxIndex, index);
        }
        for (int i = minIndex; i <= maxIndex; i++) {
            if (!versions.contains(order.get(i))) {
                throw new BusinessException("TLS 版本必须连续开启，例如 TLSv1.0,TLSv1.1,TLSv1.2");
            }
        }
        return versions.toArray(new String[0]);
    }

    @Override
    public void forcedToJump(CdnDomain cdnDomain, DomainHttpsSettingVo config,String redirectCode) throws BusinessException {
        ForceRedirectConfigDTO forceRedirectConfigDTO = config.getForceRedirect();
        ForceRedirect forceRedirect = new ForceRedirect();
        if ("on".equals(forceRedirectConfigDTO.getStatus())) {
            forceRedirect.setSwitch("on");
            forceRedirect.setRedirectType(forceRedirectConfigDTO.getType());
            forceRedirect.setRedirectStatusCode(Long.valueOf(forceRedirectConfigDTO.getRedirect_code()));
        } else {
            forceRedirect.setSwitch("off");
        }
        CdnClient cdn = TencentClient.getCdnClient();
        UpdateDomainConfigRequest req = new UpdateDomainConfigRequest();
        req.setDomain(cdnDomain.getDomainName());
        req.setForceRedirect(forceRedirect);
        try {
            UpdateDomainConfigResponse resp = cdn.UpdateDomainConfig(req);
            log.info("更新域名 {} 强制跳转配置成功：{}", cdnDomain.getDomainName(), UpdateDomainConfigResponse.toJsonString(resp));
        } catch (TencentCloudSDKException e) {
            log.error("更新域名 {} 强制跳转配置失败：{} - {}", cdnDomain.getDomainName(), e.getErrorCode(), e.getMessage());
            throw new BusinessException("更新强制跳转配置失败，" + TencentErrorCodeHandler.getErrorDescription(e));
        }
    }

    @Override
    public void saveCacheRules(CdnDomain cdnDomain, SettingCacheVo config) throws BusinessException {
        UpdateDomainConfigRequest req = new UpdateDomainConfigRequest();
        // ## 缓存配置
        Cache cache = new Cache();
        ArrayList<RuleCache> ruleCaches = new ArrayList<>();
        // ## 遵循源站 on 开启 off 关闭
        String compareMaxAge = config.getCacheFollowOriginStatus();
        // 默认 off 防止为空情况
        if (Assert.isEmpty(compareMaxAge)) {
            compareMaxAge = "off";
        }
//        List<CacheRuleDTO> configCacheRules = config.getCacheRules();
//        boolean unlocked = false;
//        if (configCacheRules.isEmpty()) {
//            configCacheRules.add(CacheRuleDTO.builder().match_type("all").match_value("*").ttl(30).ttl_unit("d").build());
//        } else {
//            unlocked = 1 < configCacheRules.size();
//        }
        for (CacheRuleDTO cacheRule : config.getCacheRules()) {
            String matchType = cacheRule.getMatch_type();
            String matchValue = cacheRule.getMatch_value();
            // 跳过 0 & all
//            if (0 == cacheRule.getTtl() && "all".equals(matchType) && unlocked) {
//                continue;
//            }
            RuleCache ruleCache = new RuleCache();
            // ## 缓存规则类型
            if ("full_path".equals(matchType)) {
                ruleCache.setRuleType("path");
                ruleCache.setRulePaths(new String[]{matchValue});
            } else if ("catalog".equals(matchType)) {
                ruleCache.setRuleType("directory");
                ruleCache.setRulePaths(matchValue.split(";"));
            } else if ("file_extension".equals(matchType)) {
                ruleCache.setRuleType("file");
                ruleCache.setRulePaths(matchValue.replace(".", "").split(";"));
            } else if ("home_page".equals(matchType)) {
                ruleCache.setRuleType("index");
                ruleCache.setRulePaths(new String[]{"/"});
            } else {
                ruleCache.setRuleType("all");
                ruleCache.setRulePaths(new String[]{"*"});
            }
            RuleCacheConfig ruleCacheConfig = new RuleCacheConfig();
            if (0 == cacheRule.getTtl()) {
                CacheConfigNoCache cacheConfigNoCache = new CacheConfigNoCache();
                cacheConfigNoCache.setSwitch("on");
                cacheConfigNoCache.setRevalidate("off");
                ruleCacheConfig.setNoCache(cacheConfigNoCache);
            } else {
                CacheConfigCache cacheConfigCache = new CacheConfigCache();
                cacheConfigCache.setSwitch("on");
                cacheConfigCache.setCacheTime(KuocaiBaseUtil.toSeconds(cacheRule.getTtl(), cacheRule.getTtl_unit()));
                // ## 遵循源站 ?? 暂时这样
                cacheConfigCache.setCompareMaxAge(compareMaxAge);
                cacheConfigCache.setIgnoreCacheControl("off");
                cacheConfigCache.setIgnoreSetCookie("off");
                ruleCacheConfig.setCache(cacheConfigCache);
            }
            // ## 设置缓存规则
            ruleCache.setCacheConfig(ruleCacheConfig);
            ruleCaches.add(ruleCache);
        }
        // 优先级从下到上 需要反转
        Collections.reverse(ruleCaches);
        cache.setRuleCache(ruleCaches.toArray(new RuleCache[0]));
        // ## 更新配置
        req.setDomain(cdnDomain.getDomainName());
        req.setCache(cache);
        CdnClient cdn = TencentClient.getCdnClient();
        try {
            UpdateDomainConfigResponse resp = cdn.UpdateDomainConfig(req);
            log.info("更新域名 {} 缓存配置成功：{}", cdnDomain.getDomainName(), UpdateDomainConfigResponse.toJsonString(resp));
        } catch (TencentCloudSDKException e) {
            log.error("更新域名 {} 缓存配置失败：{} - {}", cdnDomain.getDomainName(), e.getErrorCode(), e.getMessage());
            throw new BusinessException("更新缓存配置失败，" + TencentErrorCodeHandler.getErrorDescription(e));
        }
    }

    @Override
    public void saveCacheFollowOriginStatusSwitch(CdnDomain cdnDomain, SettingCacheVo config) throws BusinessException {
        // 未使用
    }

    @Override
    public void saveErrorCodeCache(CdnDomain cdnDomain, SettingCacheVo config) throws BusinessException {
        UpdateDomainConfigRequest req = new UpdateDomainConfigRequest();
        CdnClient cdn = TencentClient.getCdnClient();
        // ## 更新配置
        req.setDomain(cdnDomain.getDomainName());
        // ## 状态码缓存配置
        StatusCodeCache statusCodeCache = new StatusCodeCache();
        statusCodeCache.setSwitch("on");
        ArrayList<StatusCodeCacheRule> statusCodeCacheRules = new ArrayList<>();
        for (ErrorCodeCacheDTO errorCodeCacheDTO : config.getErrorCodeCache()) {
            StatusCodeCacheRule statusCodeCacheRule = new StatusCodeCacheRule();
            statusCodeCacheRule.setStatusCode(errorCodeCacheDTO.getCode().toString());
            statusCodeCacheRule.setCacheTime(errorCodeCacheDTO.getTtl().longValue());
            statusCodeCacheRules.add(statusCodeCacheRule);
        }
        statusCodeCache.setCacheRules(statusCodeCacheRules.toArray(new StatusCodeCacheRule[0]));
        req.setStatusCodeCache(statusCodeCache);
        try {
            UpdateDomainConfigResponse resp = cdn.UpdateDomainConfig(req);
            log.info("更新域名 {} 状态码缓存配置成功：{}", cdnDomain.getDomainName(), UpdateDomainConfigResponse.toJsonString(resp));
        } catch (TencentCloudSDKException e) {
            log.error("更新域名 {} 状态码缓存存配置失败：{} - {}", cdnDomain.getDomainName(), e.getErrorCode(), e.getMessage());
            throw new BusinessException("更新状态码缓存配置失败，" + TencentErrorCodeHandler.getErrorDescription(e));
        }
    }

    @Override
    public void saveHotlinkPrevention(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        RefererDTO refererDTO = config.getReferer();
        Referer referer = new Referer();
        Integer refererType = refererDTO.getReferer_type();
        if (0 == refererType) {
            referer.setSwitch("off");
        } else {
            referer.setSwitch("on");
            // ## 防盗链规则
            RefererRule refererRule = new RefererRule();
            refererRule.setRefererType("blacklist");
            if (2 == refererType) {
                refererRule.setRefererType("whitelist");
            }
            refererRule.setReferers(refererDTO.getReferers().toArray(new String[0]));
            refererRule.setAllowEmpty(refererDTO.getInclude_empty());
            refererRule.setRuleType("all");
            refererRule.setRulePaths(new String[]{"*"});
            referer.setRefererRules(new RefererRule[]{refererRule});
        }
        // ## 更新配置
        CdnClient cdn = TencentClient.getCdnClient();
        UpdateDomainConfigRequest req = new UpdateDomainConfigRequest();
        req.setDomain(cdnDomain.getDomainName());
        req.setReferer(referer);
        try {
            UpdateDomainConfigResponse resp = cdn.UpdateDomainConfig(req);
            log.info("更新域名 {} 防盗链配置成功：{}", cdnDomain.getDomainName(), UpdateDomainConfigResponse.toJsonString(resp));
        } catch (TencentCloudSDKException e) {
            log.error("更新域名 {} 防盗链配置失败：{} - {}", cdnDomain.getDomainName(), e.getErrorCode(), e.getMessage());
            throw new BusinessException("更新防盗链配置失败，" + TencentErrorCodeHandler.getErrorDescription(e));
        }
    }

    @Override
    public void saveIpBlackWhiteList(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        Integer banIpType = config.getType();
        IpFilter ipFilter = new IpFilter();
        if (0 == banIpType) {
            ipFilter.setSwitch("off");
        } else {
            ipFilter.setSwitch("on");
            ipFilter.setFilterType("blacklist");
            if (2 == banIpType) {
                ipFilter.setFilterType("whitelist");
            }
            ipFilter.setFilters(config.getIps().toArray(new String[0]));
        }
        CdnClient cdn = TencentClient.getCdnClient();
        UpdateDomainConfigRequest req = new UpdateDomainConfigRequest();
        req.setDomain(cdnDomain.getDomainName());
        req.setIpFilter(ipFilter);
        try {
            UpdateDomainConfigResponse resp = cdn.UpdateDomainConfig(req);
            log.info("更新域名 {} IP黑白名单配置成功：{}", cdnDomain.getDomainName(), UpdateDomainConfigResponse.toJsonString(resp));
        } catch (TencentCloudSDKException e) {
            log.error("更新域名 {} IP黑白名单配置失败：{} - {}", cdnDomain.getDomainName(), e.getErrorCode(), e.getMessage());
            throw new BusinessException("更新IP黑白名单配置失败，" + TencentErrorCodeHandler.getErrorDescription(e));
        }
    }

    @Override
    public void saveUserAgentFilter(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        UserAgentBlackAndWhiteListDTO userAgentBlackAndWhiteListDTO = config.getUserAgentBlackAndWhiteListDTO();
        Integer filterType = userAgentBlackAndWhiteListDTO.getType();
        UserAgentFilter userAgentFilter = new UserAgentFilter();
        if (0 == filterType) {
            userAgentFilter.setSwitch("off");
        } else {
            userAgentFilter.setSwitch("on");
            List<String> uaList = userAgentBlackAndWhiteListDTO.getUa_list();
            UserAgentFilterRule userAgentFilterRule = new UserAgentFilterRule();
            userAgentFilterRule.setFilterType("blacklist");
            if (2 == filterType) {
                userAgentFilterRule.setFilterType("whitelist");
            }
            userAgentFilterRule.setUserAgents(uaList.toArray(new String[0]));
            userAgentFilterRule.setRuleType("all");
            userAgentFilterRule.setRulePaths(new String[]{"*"});
            userAgentFilter.setFilterRules(new UserAgentFilterRule[]{userAgentFilterRule});
        }
        CdnClient cdn = TencentClient.getCdnClient();
        UpdateDomainConfigRequest req = new UpdateDomainConfigRequest();
        req.setDomain(cdnDomain.getDomainName());
        req.setUserAgentFilter(userAgentFilter);
        try {
            UpdateDomainConfigResponse resp = cdn.UpdateDomainConfig(req);
            log.info("更新域名 {} UA黑白名单配置成功：{}", cdnDomain.getDomainName(), UpdateDomainConfigResponse.toJsonString(resp));
        } catch (TencentCloudSDKException e) {
            log.error("更新域名 {} UA黑白名单配置失败：{} - {}", cdnDomain.getDomainName(), e.getErrorCode(), e.getMessage());
            throw new BusinessException("更新UA黑白名单配置失败，" + TencentErrorCodeHandler.getErrorDescription(e));
        }
    }

    @Override
    public void saveUrlAuth(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {

    }

    @Override
    public void saveHttpHeader(CdnDomain cdnDomain, SettingHigherVo config) throws BusinessException {
        List<HttpResponseHeaderDTO> httpResponseHeaders = config.getHttpResponseHeaders();
        ResponseHeader responseHeader = new ResponseHeader();
        if (Assert.isEmpty(httpResponseHeaders)) {
            responseHeader.setSwitch("off");
        } else {
            responseHeader.setSwitch("on");
            ArrayList<HttpHeaderPathRule> httpHeaderPathRules = new ArrayList<>();
            for (HttpResponseHeaderDTO httpResponseHeaderDTO : httpResponseHeaders) {
                HttpHeaderPathRule httpHeaderPathRule = new HttpHeaderPathRule();
                httpHeaderPathRule.setHeaderMode("delete".equals(httpResponseHeaderDTO.getAction()) ? "del" : "set");
                httpHeaderPathRule.setHeaderName(httpResponseHeaderDTO.getName());
                httpHeaderPathRule.setHeaderValue(httpResponseHeaderDTO.getValue());
                httpHeaderPathRule.setRuleType("all");
                httpHeaderPathRule.setRulePaths(new String[]{"*"});
                httpHeaderPathRules.add(httpHeaderPathRule);
            }
            responseHeader.setHeaderRules(httpHeaderPathRules.toArray(new HttpHeaderPathRule[0]));
        }
        CdnClient cdn = TencentClient.getCdnClient();
        UpdateDomainConfigRequest req = new UpdateDomainConfigRequest();
        req.setDomain(cdnDomain.getDomainName());
        req.setResponseHeader(responseHeader);
        try {
            UpdateDomainConfigResponse resp = cdn.UpdateDomainConfig(req);
            log.info("更新域名 {} 响应头配置成功：{}", cdnDomain.getDomainName(), UpdateDomainConfigResponse.toJsonString(resp));
        } catch (TencentCloudSDKException e) {
            log.error("更新域名 {} 响应头配置失败：{} - {}", cdnDomain.getDomainName(), e.getErrorCode(), e.getMessage());
            throw new BusinessException("更新响应头配置失败，" + TencentErrorCodeHandler.getErrorDescription(e));
        }
    }

    @Override
    public void saveCustomErrorPageConfiguration(CdnDomain cdnDomain, SettingHigherVo config) throws BusinessException {
        // 暂未使用
    }

    @Override
    public void saveCompress(CdnDomain cdnDomain, SettingHigherVo config) throws BusinessException {
        CompressDTO compressDTO = config.getCompress();
        String status = compressDTO.getStatus();
        String type = compressDTO.getType();
        Compression compression = new Compression();
        if ("on".equals(status)) {
            compression.setSwitch("on");
            CompressionRule compressionRule = new CompressionRule();
            compressionRule.setCompress(true);
            compressionRule.setAlgorithms(type.replace("br", "brotli").split(","));
            compressionRule.setMinLength(256L);
            compressionRule.setMaxLength(5 * 1024 * 1024L);
            compression.setCompressionRules(new CompressionRule[]{compressionRule});
        } else {
            compression.setSwitch("off");
        }
        CdnClient cdn = TencentClient.getCdnClient();
        UpdateDomainConfigRequest req = new UpdateDomainConfigRequest();
        req.setDomain(cdnDomain.getDomainName());
        req.setCompression(compression);
        try {
            UpdateDomainConfigResponse resp = cdn.UpdateDomainConfig(req);
            log.info("更新域名 {} 压缩配置成功：{}", cdnDomain.getDomainName(), UpdateDomainConfigResponse.toJsonString(resp));
        } catch (TencentCloudSDKException e) {
            log.error("更新域名 {} 压缩配置失败：{} - {}", cdnDomain.getDomainName(), e.getErrorCode(), e.getMessage());
            throw new BusinessException("更新压缩配置失败，" + TencentErrorCodeHandler.getErrorDescription(e));
        }
    }

    @Override
    public DomainConfig getDomainConfig(String domainName) throws BusinessException {
        DetailDomain domain = getDetailDomain(domainName);
        Origin origin = domain.getOrigin();
        if (origin == null) {
            origin = new Origin();
        }
        // ## 回源信息
        DomainBasicInfo.SourceStationPrimaryInfo sourceStationPrimaryInfo = DomainBasicInfo.SourceStationPrimaryInfo.builder()
                .sourceStationType("").ipOrDomain("").httpPort("").httpsPort("").sourceHost("")
                .build();
        DomainBasicInfo.SourceStationStandbyInfo sourceStationStandbyInfo = DomainBasicInfo.SourceStationStandbyInfo.builder()
                .sourceStationType("").ipOrDomain("").httpPort("").httpsPort("").sourceHost("")
                .build();
        if (Assert.notEmpty(origin.getOrigins())) {
            sourceStationPrimaryInfo.setSourceStationType(OriginTypeEnum.getSelfParam(origin.getOriginType(), CdnOperationRoute.TENCENT));
            sourceStationPrimaryInfo.setIpOrDomain(String.join(";", origin.getOrigins()));
            sourceStationPrimaryInfo.setHttpPort("80");
            sourceStationPrimaryInfo.setHttpsPort("443");
            sourceStationPrimaryInfo.setSourceHost(origin.getServerName());
        }
        if (Assert.notEmpty(origin.getBackupOrigins())) {
            sourceStationStandbyInfo.setSourceStationType(OriginTypeEnum.getSelfParam(origin.getBackupOriginType(), CdnOperationRoute.TENCENT));
            sourceStationStandbyInfo.setIpOrDomain(String.join(";", origin.getBackupOrigins()));
            sourceStationStandbyInfo.setHttpPort("80");
            sourceStationStandbyInfo.setHttpsPort("443");
            sourceStationStandbyInfo.setSourceHost(origin.getBackupServerName());
        }
        // ## https 配置信息
        DomainHttpsInfo.HttpGetBody httpGetBody = DomainHttpsInfo.HttpGetBody.builder()
                .https_status("off").certificate_name("").certificate_value("").expire_time(0L).certificate_source(0).certificate_type("").http2_status("off").tls_version("").ocsp_stapling_status("off").certId(0)
                .build();
        DomainHttpsInfo.ForceRedirect forceRedirect = DomainHttpsInfo.ForceRedirect.builder()
                .status("off").type("https").redirect_code("301")
                .build();
        String httpsStatus = "0";
        if (Assert.notEmpty(domain.getHttps())) {
            Https https = domain.getHttps();
            httpsStatus = "on".equals(https.getSwitch()) ? "1" : "0";
            httpGetBody.setHttps_status(https.getSwitch());
            if (Assert.notEmpty(https.getCertInfo())) {
                httpGetBody.setCertificate_name(https.getCertInfo().getMessage());
                httpGetBody.setCertificate_value(https.getCertInfo().getCertificate());
            }
            httpGetBody.setHttp2_status(https.getHttp2());
            httpGetBody.setTls_version(String.join(";", Arrays.stream(emptyIfNull(https.getTlsVersion()))
                    .map(s -> s.equals("TLSv1") ? "TLSv1.0" : s)
                    .toArray(String[]::new)));
            httpGetBody.setOcsp_stapling_status(https.getOcspStapling());
        }
        if (Assert.notEmpty(domain.getForceRedirect())) {
            forceRedirect.setStatus(domain.getForceRedirect().getSwitch());
            forceRedirect.setType(domain.getForceRedirect().getRedirectType().toUpperCase());
            forceRedirect.setRedirect_code(302 == domain.getForceRedirect().getRedirectStatusCode() ? "302" : "301");
        }
        DomainHttpsInfo domainHttpsInfo = DomainHttpsInfo.builder()
                .https(httpGetBody).force_redirect(forceRedirect)
                .build();
        // ## 是否开启 ipv6
        String isIpv6 = "0";
        if (Assert.notEmpty(domain.getIpv6Access())) {
            isIpv6 = "on".equals(domain.getIpv6Access().getSwitch()) ? "1" : "0";
        }
        // ## 域名基础信息
        DomainBasicInfo domainBasicInfo = DomainBasicInfo.builder()
                .domainName(domain.getDomain())
                .domainStatus(convertDomainStatus(domain.getStatus()))
                .httpsStatus(httpsStatus)
                .cname(domain.getCname())
                .businessType(BusinessTypeEnum.getSelfParam(domain.getServiceType(), CdnOperationRoute.TENCENT))
                .serviceArea(ServiceAreaEnum.getSelfParam(domain.getArea(), CdnOperationRoute.TENCENT))
                .isIpv6(isIpv6)
                .createTime(KuocaiDateUtil.toDate(domain.getCreateTime()))
                .updateTime(KuocaiDateUtil.toDate(domain.getUpdateTime()))
                .sourceStationPrimaryInfo(sourceStationPrimaryInfo)
                .sourceStationStandbyInfo(sourceStationStandbyInfo)
                .build();
        // ## 回源配置信息
        String originProtocol = OriginProtocolEnum.HTTP.getParam();
        if (Assert.notEmpty(origin.getOriginPullProtocol())) {
            originProtocol = OriginProtocolEnum.getSelfParam(origin.getOriginPullProtocol(), CdnOperationRoute.TENCENT);
        }
        String originReceiveTimeout = "30";
        if (Assert.notEmpty(domain.getOriginPullTimeout())) {
            originReceiveTimeout = String.valueOf(domain.getOriginPullTimeout().getReceiveTimeout());
        }
        String originRangeStatus = "off";
        if (Assert.notEmpty(domain.getRangeOriginPull())) {
            originRangeStatus = "on".equals(domain.getRangeOriginPull().getSwitch()) ? "on" : "off";
        }
        ArrayList<DomainBackSourceInfo.BackSourceRequestInfo> backSourceRequestInfos = new ArrayList<>();
        // RequestHeader ResponseHeader 一样的 headerMode
        ArrayList<String> headerMode = new ArrayList<>(Arrays.asList("del", "set"));
        RequestHeader domainRequestHeader = domain.getRequestHeader();
        if (Assert.notEmpty(domainRequestHeader) && "on".equals(domainRequestHeader.getSwitch())) {
            for (HttpHeaderPathRule httpHeaderPathRule : emptyIfNull(domainRequestHeader.getHeaderRules())) {
                if (httpHeaderPathRule == null) {
                    continue;
                }
                if (headerMode.contains(httpHeaderPathRule.getHeaderMode())) {
                    DomainBackSourceInfo.BackSourceRequestInfo backSourceRequestInfo = DomainBackSourceInfo.BackSourceRequestInfo.builder()
                            .action("del".equals(httpHeaderPathRule.getHeaderMode()) ? "delete" : "set")
                            .name(httpHeaderPathRule.getHeaderName())
                            .value(httpHeaderPathRule.getHeaderValue())
                            .build();
                    backSourceRequestInfos.add(backSourceRequestInfo);
                }
            }
        }
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
        // ## 缓存配置信息
        ArrayList<DomainCacheInfo.CacheRule> cacheRules = new ArrayList<>();
        if (Assert.notEmpty(domain.getCache())) {
            SimpleCache simpleCache = domain.getCache().getSimpleCache();
            if (Assert.notEmpty(simpleCache) && Assert.notEmpty(simpleCache.getCacheRules())) {
                for (SimpleCacheRule simpleCacheRule : simpleCache.getCacheRules()) {
                    DomainCacheInfo.CacheRule cacheRule = DomainCacheInfo.CacheRule.builder()
                            .match_value("").ttl(0).ttl_unit("s").follow_origin("off").url_parameter_type("").url_parameter_value("")
                            .build();
                    if (simpleCacheRule == null) {
                        continue;
                    }
                    String[] rulePaths = emptyIfNull(simpleCacheRule.getCacheContents());
                    switch (simpleCacheRule.getCacheType()) {
                        case "file":
                            cacheRule.setMatch_type("file_extension");
                            cacheRule.setMatch_value("." + String.join(";.", rulePaths));
                            break;
                        case "directory":
                            cacheRule.setMatch_type("catalog");
                            cacheRule.setMatch_value(String.join(";", rulePaths));
                            break;
                        case "path":
                            cacheRule.setMatch_type("full_path");
                            if (rulePaths.length > 0) {
                                cacheRule.setMatch_value(rulePaths[0]);
                            }
                            break;
                        case "index":
                            cacheRule.setMatch_type("home_page");
                            break;
                        default:
                            cacheRule.setMatch_type("all");
                            break;
                    }
                    Integer cacheTime = simpleCacheRule.getCacheTime() == null ? 0 : simpleCacheRule.getCacheTime().intValue();
                    cacheRule.setTtl(KuocaiBaseUtil.getUnitCacheTime(cacheTime));
                    cacheRule.setTtl_unit(KuocaiBaseUtil.getCacheTimeUnit(cacheTime));
                    cacheRule.setFollow_origin(simpleCache.getFollowOrigin());
                    cacheRules.add(cacheRule);
                }
            }
            RuleCache[] ruleCaches = domain.getCache().getRuleCache();
            if (Assert.notEmpty(ruleCaches)) {
                ArrayList<String> ruleTypes = new ArrayList<>(Arrays.asList("all", "file", "directory", "path", "index"));
                for (RuleCache ruleCache : ruleCaches) {
                    if (ruleCache == null) {
                        continue;
                    }
                    if (ruleTypes.contains(ruleCache.getRuleType())) {
                        DomainCacheInfo.CacheRule cacheRule = DomainCacheInfo.CacheRule.builder()
                                .match_value("").ttl(0).ttl_unit("s").follow_origin("off").url_parameter_type("").url_parameter_value("")
                                .build();
                        String ruleType = ruleCache.getRuleType();
                        String[] rulePaths = emptyIfNull(ruleCache.getRulePaths());
                        if ("file".equals(ruleType)) {
                            cacheRule.setMatch_type("file_extension");
                            cacheRule.setMatch_value("." + String.join(";.", rulePaths));
                        } else if ("directory".equals(ruleType)) {
                            cacheRule.setMatch_type("catalog");
                            cacheRule.setMatch_value(String.join(";", rulePaths));
                        } else if ("path".equals(ruleType)) {
                            cacheRule.setMatch_type("full_path");
                            if (rulePaths.length > 0) {
                                cacheRule.setMatch_value(rulePaths[0]);
                            }
                        } else if ("index".equals(ruleType)) {
                            cacheRule.setMatch_type("home_page");
                        } else {
                            cacheRule.setMatch_type("all");
                        }
                        RuleCacheConfig cacheConfig = ruleCache.getCacheConfig();
                        if (Assert.notEmpty(cacheConfig)) {
                            CacheConfigCache cacheConfigCache = cacheConfig.getCache();
                            if (Assert.notEmpty(cacheConfigCache) && "on".equals(cacheConfigCache.getSwitch())) {
                                Integer cacheTime = cacheConfigCache.getCacheTime().intValue();
                                cacheRule.setTtl(KuocaiBaseUtil.getUnitCacheTime(cacheTime));
                                cacheRule.setTtl_unit(KuocaiBaseUtil.getCacheTimeUnit(cacheTime));
                                cacheRule.setFollow_origin(cacheConfigCache.getCompareMaxAge());
                            }
                        }
                        cacheRules.add(cacheRule);
                    }
                }
            }
        }
        Collections.reverse(cacheRules);
        // ## 状态码缓存配置
        ArrayList<DomainCacheInfo.ErrorCodeCache> errorCodeCaches = new ArrayList<>();
        if (Assert.notEmpty(domain.getStatusCodeCache())) {
            StatusCodeCacheRule[] statusCodeCacheCacheRules = domain.getStatusCodeCache().getCacheRules();
            if (Assert.notEmpty(statusCodeCacheCacheRules)) {
                for (StatusCodeCacheRule statusCodeCacheRule : statusCodeCacheCacheRules) {
                    if (statusCodeCacheRule == null || statusCodeCacheRule.getStatusCode() == null || statusCodeCacheRule.getCacheTime() == null) {
                        continue;
                    }
                    DomainCacheInfo.ErrorCodeCache errorCodeCache = DomainCacheInfo.ErrorCodeCache.builder()
                            .code(Integer.valueOf(statusCodeCacheRule.getStatusCode()))
                            .ttl(statusCodeCacheRule.getCacheTime().intValue())
                            .build();
                    errorCodeCaches.add(errorCodeCache);
                }
            }
        }
        DomainCacheInfo domainCacheInfo = DomainCacheInfo.builder()
                .cache_rules(cacheRules).error_code_cache(errorCodeCaches)
                .build();
        // ## referer 防盗链
        DomainVisitInfo.Referer referer = DomainVisitInfo.Referer.builder()
                .type("off").referer_type(0).value("").include_empty(false)
                .build();
        Referer domainReferer = domain.getReferer();
        if (Assert.notEmpty(domainReferer)) {
            referer.setType(domainReferer.getSwitch());
            RefererRule[] refererRules = domainReferer.getRefererRules();
            if (Assert.notEmpty(refererRules)) {
                RefererRule refererRule = refererRules[0];
                if (refererRule == null) {
                    refererRule = new RefererRule();
                }
                if ("on".equals(domainReferer.getSwitch())) {
                    boolean isDomainRefererWhitelist = "whitelist".equals(refererRule.getRefererType());
                    referer.setType(isDomainRefererWhitelist ? "white" : "black");
                    referer.setReferer_type(isDomainRefererWhitelist ? 2 : 1);
                }
                referer.setValue(String.join("\n", emptyIfNull(refererRule.getReferers())));
                referer.setInclude_empty(Boolean.TRUE.equals(refererRule.getAllowEmpty()));
            }
        }
        // ## ip 黑白名单
        DomainVisitInfo.IpFilter ipFilter = DomainVisitInfo.IpFilter.builder()
                .type("off").value("")
                .build();
        IpFilter domainIpFilter = domain.getIpFilter();
        if (Assert.notEmpty(domainIpFilter) && "on".equals(domainIpFilter.getSwitch())) {
            ipFilter.setType("whitelist".equals(domainIpFilter.getFilterType()) ? "white" : "black");
            ipFilter.setValue(String.join("\n", emptyIfNull(domainIpFilter.getFilters())));
        }
        // ## user-agent 黑白名单
        DomainVisitInfo.UserAgentFilter userAgentFilter = DomainVisitInfo.UserAgentFilter.builder()
                .type("off").value("")
                .build();
        UserAgentFilter domainUserAgentFilter = domain.getUserAgentFilter();
        if (Assert.notEmpty(domainUserAgentFilter) && "on".equals(domainUserAgentFilter.getSwitch())) {
            UserAgentFilterRule[] filterRules = domainUserAgentFilter.getFilterRules();
            if (Assert.notEmpty(filterRules) && filterRules[0] != null) {
                userAgentFilter.setType("whitelist".equals(filterRules[0].getFilterType()) ? "white" : "black");
                userAgentFilter.setValue(String.join("\n", emptyIfNull(filterRules[0].getUserAgents())));
            }
        }
        DomainVisitInfo domainVisitInfo = DomainVisitInfo.builder()
                .referer(referer).ip_filter(ipFilter).user_agent_filter(userAgentFilter)
                .build();
        // ## 自定义响应头
        ArrayList<DomainAdvancedInfo.HttpResponseHeader> httpResponseHeaders = new ArrayList<>();
        ResponseHeader domainResponseHeader = domain.getResponseHeader();
        if (Assert.notEmpty(domainResponseHeader) && "on".equals(domainResponseHeader.getSwitch())) {
            for (HttpHeaderPathRule httpHeaderPathRule : emptyIfNull(domainResponseHeader.getHeaderRules())) {
                if (httpHeaderPathRule == null) {
                    continue;
                }
                if (headerMode.contains(httpHeaderPathRule.getHeaderMode())) {
                    DomainAdvancedInfo.HttpResponseHeader httpResponseHeader = DomainAdvancedInfo.HttpResponseHeader.builder()
                            .action("del".equals(httpHeaderPathRule.getHeaderMode()) ? "delete" : "set")
                            .name(httpHeaderPathRule.getHeaderName())
                            .value(httpHeaderPathRule.getHeaderValue())
                            .build();
                    httpResponseHeaders.add(httpResponseHeader);
                }
            }
        }
        // ## 自定义错误码重定向
        ArrayList<DomainAdvancedInfo.ErrorCodeRedirectRules> errorCodeRedirectRules = new ArrayList<>();
        ErrorPage domainErrorPage = domain.getErrorPage();
        if (Assert.notEmpty(domainErrorPage)) {
            if ("on".equals(domainErrorPage.getSwitch())) {
                for (ErrorPageRule errorPageRule : emptyIfNull(domainErrorPage.getPageRules())) {
                    if (errorPageRule == null || errorPageRule.getStatusCode() == null) {
                        continue;
                    }
                    DomainAdvancedInfo.ErrorCodeRedirectRules codeRedirectRule = DomainAdvancedInfo.ErrorCodeRedirectRules.builder()
                            // 这里转换 ?? Long -> Integer
                            .error_code(Integer.valueOf(errorPageRule.getStatusCode().toString()))
                            .target_code(Long.valueOf(302L).equals(errorPageRule.getRedirectCode()) ? "302" : "301")
                            .target_link(errorPageRule.getRedirectUrl())
                            .build();
                    errorCodeRedirectRules.add(codeRedirectRule);
                }
            }
        }
        DomainAdvancedInfo.Compress compress = DomainAdvancedInfo.Compress.builder()
                .status("off").type("").file_type("")
                .build();
        Compression domainCompression = domain.getCompression();
        if (Assert.notEmpty(domainCompression) && "on".equals(domainCompression.getSwitch())) {
            CompressionRule[] compressionRules = domainCompression.getCompressionRules();
            if (Assert.notEmpty(compressionRules) && compressionRules[0] != null) {
                if (Boolean.TRUE.equals(compressionRules[0].getCompress())) {
                    compress.setStatus("on");
                    compress.setType(String.join(";", emptyIfNull(compressionRules[0].getAlgorithms())).replace("brotli", "br"));
                    compress.setFile_type(String.join(",", emptyIfNull(compressionRules[0].getFileExtensions())));
                } else {
                    compress.setStatus("off");
                }
            }
        }
        DomainAdvancedInfo domainAdvancedInfo = DomainAdvancedInfo.builder()
                .http_response_header(httpResponseHeaders).error_code_redirect_rules(errorCodeRedirectRules).compress(compress)
                .build();
        return DomainConfig.builder()
                .domainBasicInfo(domainBasicInfo)
                .domainBackSourceInfo(domainBackSourceInfo)
                .domainHttpsInfo(domainHttpsInfo)
                .domainCacheInfo(domainCacheInfo)
                .domainVisitInfo(domainVisitInfo)
                .domainAdvancedInfo(domainAdvancedInfo)
                .build();
    }

    public BriefDomain getBriefDomain(String domainName) throws BusinessException {
        CdnClient cdn = TencentClient.getCdnClient();
        DescribeDomainsRequest domainsRequest = new DescribeDomainsRequest();
        DomainFilter domainFilter = new DomainFilter();
        domainFilter.setName("domain");
        domainFilter.setValue(new String[]{domainName});
        domainFilter.setFuzzy(false);
        domainsRequest.setFilters(new DomainFilter[]{domainFilter});
        DescribeDomainsResponse domainsResponse;
        try {
            domainsResponse = cdn.DescribeDomains(domainsRequest);
        } catch (TencentCloudSDKException e) {
            log.error("获取域名 {} 信息失败：{} - {}", domainName, e.getErrorCode(), e.getMessage());
            throw new BusinessException("获取域名信息失败，" + TencentErrorCodeHandler.getErrorDescription(e));
        }
        // 不是1个域名 说明有问题
        if (domainsResponse.getTotalNumber() != 1) {
            log.error("获取域名 {} 信息失败：{}", domainName, "域名不存在");
            throw new BusinessException("获取域名信息失败，请重试");
        }
        // ## 获取域名配置信息
        return domainsResponse.getDomains()[0];
    }

    public DetailDomain getDetailDomain(String domainName) throws BusinessException {
        CdnClient cdn = TencentClient.getCdnClient();
        DescribeDomainsConfigRequest domainsRequest = new DescribeDomainsConfigRequest();
        DomainFilter domainFilter = new DomainFilter();
        domainFilter.setName("domain");
        domainFilter.setValue(new String[]{domainName});
        domainFilter.setFuzzy(false);
        domainsRequest.setFilters(new DomainFilter[]{domainFilter});
        DescribeDomainsConfigResponse domainsResponse;
        try {
            domainsResponse = cdn.DescribeDomainsConfig(domainsRequest);
            System.out.println(DescribeDomainsConfigResponse.toJsonString(domainsResponse));
        } catch (TencentCloudSDKException e) {
            log.error("获取域名 {} 详细信息失败：{} - {}", domainName, e.getErrorCode(), e.getMessage());
            throw new BusinessException("获取域名详细信息失败，" + TencentErrorCodeHandler.getErrorDescription(e));
        }
        // 不是1个域名 说明有问题
        if (domainsResponse.getTotalNumber() != 1) {
            log.error("获取域名 {} 详细信息失败：{}", domainName, "域名不存在");
            throw new BusinessException("获取域名详细信息失败，请重试");
        }
        // ## 获取域名配置信息
        return domainsResponse.getDomains()[0];
    }

    private String convertDomainStatus(String domainStatus) {
        TencentDomainStatus status = TencentDomainStatus.convert(domainStatus);
        switch (status) {
            case ONLINE:
                return "online";
            case OFFLINE:
                return "offline";
            case REJECTED:
                return "check_failed";
            case CLOSING:
            case PROCESSING:
            default:
                return "configuring";
        }
    }

    private String[] buildTencentOrigins(String originAddr, String originProtocol, Integer httpPort, Integer httpsPort, Integer originWeight, boolean includeWeight) {
        if (Assert.isEmpty(originAddr)) {
            return new String[0];
        }
        int port = getTencentOriginPort(originProtocol, httpPort, httpsPort);
        return Arrays.stream(originAddr.split(";"))
                .map(String::trim)
                .filter(Assert::notEmpty)
                .map(this::stripTencentOriginDecoration)
                .map(origin -> appendTencentOriginPort(origin, port, originWeight, includeWeight))
                .toArray(String[]::new);
    }

    private String[] splitTencentOrigins(String originAddr) {
        if (Assert.isEmpty(originAddr)) {
            return new String[0];
        }
        return Arrays.stream(originAddr.split(";"))
                .map(String::trim)
                .filter(Assert::notEmpty)
                .map(this::stripTencentOriginWeight)
                .toArray(String[]::new);
    }

    private String appendTencentOriginPort(String origin, int port, Integer originWeight, boolean includeWeight) {
        if (Assert.isEmpty(origin)) {
            return origin;
        }
        String value = origin + ":" + port;
        if (includeWeight && originWeight != null && originWeight >= 1 && originWeight <= 100) {
            value += ":" + originWeight;
        }
        return value;
    }

    private String stripTencentOriginDecorations(String originAddr) {
        if (Assert.isEmpty(originAddr)) {
            return "";
        }
        return Arrays.stream(originAddr.split(";"))
                .map(String::trim)
                .filter(Assert::notEmpty)
                .map(this::stripTencentOriginDecoration)
                .collect(Collectors.joining(";"));
    }

    private String stripTencentOriginDecoration(String origin) {
        if (Assert.isEmpty(origin)) {
            return "";
        }
        String value = origin.trim();
        String[] parts = value.split(":");
        if (parts.length == 2 && isPort(parts[1])) {
            return parts[0];
        }
        if (parts.length == 3 && isPort(parts[1]) && isWeight(parts[2])) {
            return parts[0];
        }
        return value;
    }

    private String stripTencentOriginWeight(String origin) {
        if (Assert.isEmpty(origin)) {
            return "";
        }
        String value = origin.trim();
        String[] parts = value.split(":");
        if (parts.length == 3 && isPort(parts[1]) && isWeight(parts[2])) {
            return parts[0] + ":" + parts[1];
        }
        return value;
    }

    private String extractTencentOriginPort(String[] origins, String originProtocol) {
        int defaultPort = "https".equals(originProtocol) ? 443 : 80;
        for (String origin : emptyIfNull(origins)) {
            String port = extractTencentOriginPort(origin);
            if (Assert.notEmpty(port)) {
                return port;
            }
        }
        return String.valueOf(defaultPort);
    }

    private String extractTencentOriginPort(String origin) {
        if (Assert.isEmpty(origin)) {
            return "";
        }
        String[] parts = origin.trim().split(":");
        return parts.length >= 2 && isPort(parts[1]) ? parts[1] : "";
    }

    private int getTencentOriginPort(String originProtocol, Integer httpPort, Integer httpsPort) {
        String protocol = normalizeTencentOriginProtocol(originProtocol);
        if ("https".equals(protocol)) {
            return defaultPort(httpsPort, 443);
        }
        return defaultPort(httpPort, 80);
    }

    private String normalizeTencentOriginProtocol(String originProtocol) {
        if ("https".equals(originProtocol) || "follow".equals(originProtocol)) {
            return originProtocol;
        }
        return "http";
    }

    private int defaultPort(Integer port, int defaultValue) {
        return port == null || port < 1 || port > 65535 ? defaultValue : port;
    }

    private boolean isPort(String value) {
        if (Assert.isEmpty(value)) {
            return false;
        }
        try {
            int port = Integer.parseInt(value);
            return port >= 1 && port <= 65535;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isWeight(String value) {
        if (Assert.isEmpty(value)) {
            return false;
        }
        try {
            int weight = Integer.parseInt(value);
            return weight >= 1 && weight <= 100;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String buildTencentErrorTrace(TencentCloudSDKException e) {
        List<String> traces = new ArrayList<>();
        if (Assert.notEmpty(e.getErrorCode())) {
            traces.add("errorCode=" + e.getErrorCode());
        }
        if (Assert.notEmpty(e.getRequestId())) {
            traces.add("requestId=" + e.getRequestId());
        }
        return traces.isEmpty() ? "" : " (" + String.join(", ", traces) + ")";
    }

    private String[] emptyIfNull(String[] values) {
        return values == null ? new String[0] : values;
    }

    private HttpHeaderPathRule[] emptyIfNull(HttpHeaderPathRule[] values) {
        return values == null ? new HttpHeaderPathRule[0] : values;
    }

    private RuleCache[] emptyIfNull(RuleCache[] values) {
        return values == null ? new RuleCache[0] : values;
    }

    private ErrorPageRule[] emptyIfNull(ErrorPageRule[] values) {
        return values == null ? new ErrorPageRule[0] : values;
    }
}
