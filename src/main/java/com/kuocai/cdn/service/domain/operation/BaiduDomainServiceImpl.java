package com.kuocai.cdn.service.domain.operation;

import cn.hutool.core.util.RandomUtil;
import com.baidubce.BceServiceException;
import com.baidubce.services.cdn.CdnClient;
import com.baidubce.services.cdn.model.*;
import com.baidubce.services.cdn.model.certificate.Cert;
import com.baidubce.services.cdn.model.certificate.GetDomainCertResponse;
import com.baidubce.services.cdn.model.certificate.SetDomainCertRequest;
import com.baidubce.services.cdn.model.domain.*;
import com.kuocai.cdn.api.*;
import com.kuocai.cdn.api.baidu.cdn.helper.GetHowToVerifyHelper;
import com.kuocai.cdn.api.baidu.cdn.vo.DomainTxtPair;
import com.kuocai.cdn.api.baidu.cdn.vo.GetHowToVerifyResponse;
import com.kuocai.cdn.api.baidu.cdn.vo.HowToVerify;
import com.kuocai.cdn.api.huawei.cdn.dto.*;
import com.kuocai.cdn.api.tencent.dns.CreateRecordResponse;
import com.kuocai.cdn.api.tencent.dns.TencentApi;
import com.kuocai.cdn.api.tencent.dns.dto.CreateRecordDTO;
import com.kuocai.cdn.api.tencent.dns.properties.TencentDns;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.entity.CdnDomainSources;
import com.kuocai.cdn.enumeration.domainmerage.CdnRoute;
import com.kuocai.cdn.enumeration.domainmerage.domain.*;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.service.domain.operation.optional.ICdnDomainVerifyService;
import com.kuocai.cdn.util.*;
import com.kuocai.cdn.vo.*;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.kuocai.cdn.api.baidu.cdn.BaiduCdnErrorCodeHandler.catchException;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@Service
public class BaiduDomainServiceImpl extends BaseService<CdnDomain> implements ICdnPlatformService, ICdnDomainVerifyService {

    private final CdnClient baiduCdnClient;

    private final Pattern peerPattern;

    private final Pattern peerDomainPattern;

    private final Executor executorService;

    BaiduDomainServiceImpl(@Qualifier("baiduCdnClient") CdnClient baiduCdnClient,
                           @Qualifier("cdnDomainExecutor") Executor executorService) {
        this.baiduCdnClient = baiduCdnClient;
        this.executorService = executorService;
        this.peerPattern = Pattern.compile("(https?)://(\\[?([0-9a-fA-F:.]+)]?|[a-zA-Z0-9.-]+):(\\d+)");
        this.peerDomainPattern = Pattern.compile("([\\w-]+\\.)+[a-z]+");
    }

    private <T> CompletableFuture<T> executeAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(ThreadMdcUtils.wrapAsync(supplier, MDC.getCopyOfContextMap()), executorService).exceptionally(throwable -> {
            log.error("百度云CDN异步执行失败", throwable);
            throw new CompletionException(throwable);
        });
    }

    private CdnClient getClient() {
        return baiduCdnClient;
    }

    private String getForm(String businessType) {
        switch (businessType) {
            case "web":
                return "image";
            case "download":
                return "download";
            case "video":
                return "dynamic";
            default:
                return "default";
        }
    }

    @Override
    public CdnDomain create(Long userId, String domainName, String businessType, String serviceArea, String originType, String ipOrDomain) throws BusinessException, InterruptedException {
        CdnClient client = getClient();
        // 创建域名
        CreateDomainRequest request = new CreateDomainRequest();
        request.setDomain(domainName);
        request.setDefaultHost(domainName);
        request.setForm(getForm(businessType));
        request.setFollow302(false);
        // origin
        List<OriginPeer> origin = new ArrayList<>();
        for (String str : ipOrDomain.split(";")) {
            OriginPeer originHttpPeer = new OriginPeer()
                    .withBackup(false).withWeight(1).withPeer(String.format("http://%s:80", str));
            origin.add(originHttpPeer);
            OriginPeer originHttpsPeer = new OriginPeer()
                    .withBackup(false).withWeight(1).withPeer(String.format("https://%s:443", str));
            origin.add(originHttpsPeer);
        }
        request.setOrigin(origin);
        // 创建域名
        CreateDomainResponse response;
        try {
            response = client.createDomain(request);

            // ========== 一次性排障日志：百度 CreateDomain ==========
            Map<String, Object> dbg = new HashMap<>();
            dbg.put("ts", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            dbg.put("traceId", UUID.randomUUID().toString().replace("-", ""));
            dbg.put("api", "Baidu.CreateDomain");
            dbg.put("req", String.format("DomainName=%s&Origin=%s", domainName, ipOrDomain));
            dbg.put("code", 200);
            dbg.put("msg", "success");
            log.info("{}", com.alibaba.fastjson.JSON.toJSONString(dbg));
            // ======================================================

            log.info("百度CDN创建域名 {} 成功", domainName);
        } catch (BceServiceException e) {
            log.error("百度CDN创建域名 {} 失败", domainName);

            // 失败路径同样打印一次简易 JSON 日志
            Map<String, Object> dbg = new HashMap<>();
            dbg.put("ts", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            dbg.put("traceId", UUID.randomUUID().toString().replace("-", ""));
            dbg.put("api", "Baidu.CreateDomain");
            dbg.put("req", String.format("DomainName=%s&Origin=%s", domainName, ipOrDomain));
            dbg.put("code", e.getStatusCode());
            dbg.put("msg", e.getErrorMessage());
            log.info("{}", com.alibaba.fastjson.JSON.toJSONString(dbg));
            if (e.getErrorMessage().contains("you should verify DNS_TXT")) {
                throw new BusinessException("创建时发生错误，域名解析未进行验证");
            }
            throw catchException(e);
        } catch (Exception e) {
            throw catchException(e);
        }
        // 保存信息
        CdnDomain cdnDomain = CdnDomain.builder()
                .userId(userId)
                .domainName(domainName)
                .businessType(businessType)
                .serviceArea(serviceArea)
                .domainId(response.getInsId())
                .cnameBaidu(response.getCname())
                .domainStatus(DomainStatus.CONFIGURING.getParam())
                .route(CdnRoute.BAIDU.getCode())
                .build();
        return save(cdnDomain);
    }

    @Override
    public CdnDomain configDNS(CdnDomain cdnDomain) throws TencentCloudSDKException, BusinessException {
        String domainName = cdnDomain.getDomainName();
        String cname = cdnDomain.getCnameBaidu();
        CreateRecordDTO createRecordDTO = new CreateRecordDTO();
        createRecordDTO.setDomain(TencentDns.LOCAL_DOMAIN_NAME).setSubDomain(DomainUtil.convertSubDomain(domainName)).setValue(cname);
        CreateRecordResponse createRecordResponse = TencentApi.createRecord(createRecordDTO);
        if (Assert.isEmpty(createRecordResponse.getRecordId())) {
            log.error("创建域名 {} 解析失败", domainName);
            throw new BusinessException("dns 解析失败");
        }
        log.info("创建域名 {} 解析成功", domainName);
        cdnDomain.setCname(createRecordDTO.getSubDomain() + "." + TencentDns.LOCAL_DOMAIN_NAME);
        cdnDomain.setTencentDnsId(createRecordResponse.getRecordId());
        return save(cdnDomain);
    }

    @Override
    public void save(CdnDomain cdnDomain, String businessType, String serviceArea) throws BusinessException {

    }

    @Override
    public void disable(CdnDomain cdnDomain) throws BusinessException {
        CdnClient client = getClient();
        String domain = cdnDomain.getDomainName();
        try {
            client.disableDomain(domain);
            log.info("百度CDN停用域名 {} 成功", domain);
        } catch (Exception e) {
            log.error("百度CDN停用域名 {} 失败", domain);
            throw catchException(e);
        }
    }

    @Override
    public void enable(CdnDomain cdnDomain) throws BusinessException {
        CdnClient client = getClient();
        String domain = cdnDomain.getDomainName();
        try {
            client.enableDomain(domain);
            log.info("百度CDN启用域名 {} 成功", domain);
        } catch (Exception e) {
            log.error("百度CDN启用域名 {} 失败", domain);
            throw catchException(e);
        }
    }

    @Override
    public void delete(CdnDomain cdnDomain) throws BusinessException {
        CdnClient client = getClient();
        String domain = cdnDomain.getDomainName();
        try {
            client.deleteDomain(domain);
            log.info("百度CDN删除域名 {} 成功", domain);
        } catch (Exception e) {
            log.error("百度CDN删除域名 {} 失败", domain);
            throw catchException(e);
        }
    }

    @Override
    public void ipv6(CdnDomain cdnDomain, Integer status) throws BusinessException {
        CdnClient client = getClient();
        String domain = cdnDomain.getDomainName();
        Enable enable = new Enable().withEnable(status == 1);
        SetDomainIPv6DispatchRequest request = new SetDomainIPv6DispatchRequest()
                .withDomain(domain).withIPv6Dispatch(enable);
        try {
            client.setDomainIPv6Dispatch(request);
            log.info("百度CDN设置域名 {} ipv6 {} 成功", domain, status == 1 ? "启用" : "停用");
        } catch (Exception e) {
            log.error("百度CDN设置域名 {} ipv6 {} 失败", domain, status == 1 ? "启用" : "停用");
            throw catchException(e);
        }
    }

    private void withOriginPeer(ArrayList<OriginPeer> origin, CdnDomainSources sources, String content) {
        origin.add(new OriginPeer().withPeer("http://" + content + ":" + sources.getHttpPort()).withWeight(100));
        origin.add(new OriginPeer().withPeer("https://" + content + ":" + sources.getHttpsPort()).withWeight(100));
    }

    private ArrayList<OriginPeer> convertSource(CdnDomainSources sources) {
        ArrayList<OriginPeer> origin = new ArrayList<>();
        String content = sources.getIpOrDomain();
        if ("domain".equals(sources.getOriginType())) {
            withOriginPeer(origin, sources, content);
        } else {
            if (content.contains(";")) {
                for (String it : content.split(";")) {
                    withOriginPeer(origin, sources, it);
                }
            } else {
                withOriginPeer(origin, sources, content);
            }
        }
        return origin;
    }

    @Override
    public void saveSourceStationConfig(CdnDomain cdnDomain, CdnDomainSourcesVo config) throws BusinessException {
        String domain = cdnDomain.getDomainName();
        CdnClient client = getClient();
        CdnDomainSources main = config.getMain();
        SetDomainOriginRequest request = new SetDomainOriginRequest().withDomain(domain);
        request.withDefaultHost(main.getHostName());
        request.setOrigin(convertSource(main));
        try {
            client.setDomainOrigin(request);
            log.info("百度CDN设置域名 {} 源站成功", domain);
        } catch (Exception e) {
            log.error("百度CDN设置域名 {} 源站失败", domain);
            throw catchException(e);
        }
    }

    @Override
    public void change(CdnDomain cdnDomain) throws BusinessException {

    }

    @Override
    public void saveOriginProtocol(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        String domain = cdnDomain.getDomainName();
        CdnClient client = getClient();
        try {
            client.setDomainOriginProtocol(new SetDomainOriginProtocolRequest()
                    .withDomain(domain)
                    .withOriginProtocol(new OriginProtocol().withValue(convertOriginProtocol(domainOriginSettingVo.getOriginProtocol()))));
            log.info("百度CDN设置域名 {} 源站协议成功", domain);
        } catch (Exception e) {
            log.error("百度CDN设置域名 {} 源站协议失败", domain);
            throw catchException(e);
        }
    }

    @Override
    public void saveOriginRequestUrlRewrite(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {

    }

    @Override
    public void saveAdvancedReturnSource(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
    }

    @Override
    public void saveRangeSwitch(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        CdnClient client = getClient();
        String domain = cdnDomain.getDomainName();
        try {
            client.setDomainRangeSwitch(domain, "on".equals(domainOriginSettingVo.getStatus()));
            log.info("百度CDN设置域名 {} Range 成功", domain);
        } catch (Exception e) {
            log.error("百度CDN设置域名 {} Range 失败", domain);
            throw catchException(e);
        }
    }

    @Override
    public void saveRangeVerifyETag(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {

    }

    @Override
    public void saveOriginHost(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {

    }

    @Override
    public void saveRangeTimeOut(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        String domain = cdnDomain.getDomainName();
        CdnClient client = getClient();
        SetDomainOriginTimeoutRequest request = new SetDomainOriginTimeoutRequest()
                .withDomain(domain)
                .withOriginTimeout(new OriginTimeout(5, domainOriginSettingVo.getOriginReceiveTimeOut()));
        try {
            client.setDomainOriginTimeout(request);
            log.info("百度CDN设置域名 {} 超时时间成功", domain);
        } catch (Exception e) {
            log.error("百度CDN设置域名 {} 超时时间失败", domain);
            throw catchException(e);
        }
    }

    @Override
    public void saveOriginRequestHeader(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        String domain = cdnDomain.getDomainName();
        // 获取原有配置
        GetDomainHttpHeaderResponse response = getHttpHeader(domain);
        // 处理请求头
        SetDomainHttpHeaderRequest httpHeaderRequest = new SetDomainHttpHeaderRequest().withDomain(domain);
        List<OriginRequestHeaderDTO> originRequestHeader = domainOriginSettingVo.getOriginRequestHeader();
        if (!originRequestHeader.isEmpty()) {
            for (OriginRequestHeaderDTO header : originRequestHeader) {
                HttpHeader httpHeader = new HttpHeader()
                        .withType("origin")
                        .withHeader(header.getName())
                        .withValue(header.getValue())
                        .withAction("delete".equals(header.getAction()) ? "remove" : "add");
                httpHeaderRequest.addHttpHeader(httpHeader);
            }
        }
        // 设置请求头
        List<HttpHeader> httpHeader = response.getHttpHeader();
        if (httpHeader != null) {
            for (HttpHeader header : httpHeader) {
                if ("response".equals(header.getType())) {
                    httpHeaderRequest.addHttpHeader(header);
                }
            }
        }
        setHttpHeader(domain, httpHeaderRequest);
    }

    private String randomCertName() {
        return "cert-" + RandomUtil.randomString(8);
    }

    @Override
    public void httpsConfiguration(CdnDomain cdnDomain, DomainHttpsSettingVo config) throws BusinessException {
        CdnClient client = getClient();
        HttpPutBodyDTO httpPutBodyDTO = config.getHttps();
        String domainName = cdnDomain.getDomainName();
        if ("on".equals(httpPutBodyDTO.getHttps_status())) {
            Cert cert = new Cert().withCertName(randomCertName())
                    .withCertServerData(httpPutBodyDTO.getCertificate_value())
                    .withCertPrivateData(httpPutBodyDTO.getPrivate_key()).withCertType(1);
            SetDomainCertRequest request = new SetDomainCertRequest()
                    .withDomain(domainName).withHttpsEnable("ON").withCertificate(cert);
            try {
                client.setDomainCert(request);
                log.info("百度CDN设置域名 {} 证书成功", domainName);
            } catch (Exception e) {
                log.error("百度CDN设置域名 {} 证书失败", domainName);
                throw catchException(e);
            }
        } else {
            try {
                client.deleteDomainCert(domainName);
                log.info("百度CDN删除域名 {} 证书成功", domainName);
            } catch (Exception e) {
                log.error("百度CDN删除域名 {} 证书失败", domainName);
                throw catchException(e);
            }
        }
    }

    private List<String> getTlsVersionList(String tlsVersion) {
        List<String> tlsVersions = new ArrayList<>();
        if (!tlsVersion.isEmpty()) {
            for (String s : tlsVersion.split(",")) {
                switch (s) {
                    case "TLSv1":
                        tlsVersions.add("TLSv1.0");
                        break;
                    case "TLSv1.1":
                        tlsVersions.add("TLSv1.1");
                        break;
                    case "TLSv1.2":
                        tlsVersions.add("TLSv1.2");
                        break;
                    case "TLSv1.3":
                        tlsVersions.add("TLSv1.3");
                        break;
                }
            }
        }
        return tlsVersions;
    }

    private HttpsConfig getDafaultHttpsConfig(String domainName) throws BusinessException {
        HttpsConfig httpsConfig = new HttpsConfig().withEnabled(true).withCertId("");
        try {
            GetDomainCertResponse response = getClient().getDomainCert(domainName);
            httpsConfig.withCertId(response.getCertId());
        } catch (Exception e) {
            log.error("百度CDN获取域名 {} 证书失败", domainName);
            throw catchException(e);
        }
        return httpsConfig;
    }

    @Override
    public void httpsConfigurationOther(CdnDomain cdnDomain, DomainHttpsSettingVo config) throws BusinessException {
        HttpPutBodyDTO httpPutBodyDTO = config.getHttps();
        String http2Status = httpPutBodyDTO.getHttp2_status();
        String ocspStaplingStatus = httpPutBodyDTO.getOcsp_stapling_status();
        String tlsVersion = httpPutBodyDTO.getTls_version();
        String domainName = cdnDomain.getDomainName();
        CdnClient client = getClient();
        if (Assert.notEmpty(http2Status)) {
            HttpsConfig httpsConfig = getDafaultHttpsConfig(domainName);
            httpsConfig.withHttp2Enabled("on".equals(http2Status));
            try {
                client.setDomainHttpsConfig(new SetDomainHttpsConfigRequest().withDomain(domainName).withHttps(httpsConfig));
                log.info("百度CDN设置域名 {} http2 成功", domainName);
            } catch (Exception e) {
                log.error("百度CDN设置域名 {} http2 失败", domainName);
                throw catchException(e);
            }
        }
        if (Assert.notEmpty(ocspStaplingStatus)) {
            try {
                client.setDomainOCSPSwitch(domainName, "on".equals(ocspStaplingStatus));
                log.info("百度CDN设置域名 {} ocsp 成功", domainName);
            } catch (Exception e) {
                log.error("百度CDN设置域名 {} ocsp 失败", domainName);
                throw catchException(e);
            }
        }
        if (Assert.notEmpty(tlsVersion)) {
            HttpsConfig httpsConfig = getDafaultHttpsConfig(domainName);
            httpsConfig.withSslProtocols(getTlsVersionList(tlsVersion));
            try {
                client.setDomainHttpsConfig(new SetDomainHttpsConfigRequest().withDomain(domainName).withHttps(httpsConfig));
                log.info("百度CDN设置域名 {} tls 成功", domainName);
            } catch (Exception e) {
                log.error("百度CDN设置域名 {} tls 失败", domainName);
                throw catchException(e);
            }
        }
    }

    @Override
    public void forcedToJump(CdnDomain cdnDomain, DomainHttpsSettingVo config,String redirectCode) throws BusinessException {
        ForceRedirectConfigDTO forceRedirectConfigDTO = config.getForceRedirect();
        String domainName = cdnDomain.getDomainName();
        HttpsConfig httpsConfig = getDafaultHttpsConfig(domainName);
        if ("on".equals(forceRedirectConfigDTO.getStatus())) {
            if ("https".equals(forceRedirectConfigDTO.getType())) {
                httpsConfig.withHttpRedirect(true).withHttpsRedirect(false).withHttpRedirectCode(forceRedirectConfigDTO.getRedirect_code());
            } else {
                httpsConfig.withHttpRedirect(false).withHttpsRedirect(true).withHttpsRedirectCode(forceRedirectConfigDTO.getRedirect_code());
            }
        } else {
            httpsConfig.withHttpRedirect(false).withHttpsRedirect(false);
        }
        try {
            getClient().setDomainHttpsConfig(new SetDomainHttpsConfigRequest().withDomain(domainName).withHttps(httpsConfig));
            log.info("百度CDN设置域名 {} 强制跳转成功", domainName);
        } catch (Exception e) {
            log.error("百度CDN设置域名 {} 强制跳转失败", domainName);
            throw catchException(e);
        }
    }

    private GetDomainCacheTTLResponse getDomainCacheTTLResponse(String domain) throws BusinessException {
        CdnClient client = getClient();
        try {
            return client.getDomainCacheTTL(domain);
        } catch (Exception e) {
            throw catchException(e);
        }
    }

    private void setDomainCacheTTLResponse(String domain, List<CacheTTL> list) throws BusinessException {
        CdnClient client = getClient();
        try {
            SetDomainCacheTTLRequest request = new SetDomainCacheTTLRequest().withDomain(domain);
            if (list.isEmpty()) {
                request.setCacheTTL(new ArrayList<>());
            } else {
                list.forEach(request::addCacheTTL);
            }
            client.setDomainCacheTTL(request);
            log.info("百度CDN设置域名 {} 缓存成功", domain);
        } catch (Exception e) {
            log.error("百度CDN设置域名 {} 失败成功", domain);
            throw catchException(e);
        }
    }

    private String convertCacheType(String type) {
        switch (type) {
            case "catalog":
                return "path";
            case "full_path":
                return "exactPath";
            case "file_extension":
                return "suffix";
        }
        return "exactPath";
    }

    @Override
    public void saveCacheRules(CdnDomain cdnDomain, SettingCacheVo config) throws BusinessException {
        List<CacheRuleDTO> cacheRules = config.getCacheRules();
        String domain = cdnDomain.getDomainName();
        GetDomainCacheTTLResponse response = getDomainCacheTTLResponse(domain);
        List<CacheTTL> cacheTTL = response.getCacheTTL();
        cacheTTL.removeIf(cacheIt -> !"code".equals(cacheIt.getType()));
        int weight = cacheRules.size();
        if (0 < weight) {
            for (CacheRuleDTO cacheDTO : cacheRules) {
                int intExact;
                try {
                    intExact = Math.toIntExact(KuocaiBaseUtil.toSeconds(cacheDTO.getTtl(), cacheDTO.getTtl_unit()));
                } catch (ArithmeticException e) {
                    intExact = 0;
                }
                cacheTTL.add(new CacheTTL().withType(convertCacheType(cacheDTO.getMatch_type())).withValue(cacheDTO.getMatch_value())
                        .withTtl(intExact).withWeigth(weight));
                weight = weight - 1;
            }
        }
        setDomainCacheTTLResponse(domain, cacheTTL);
    }

    @Override
    public void saveCacheFollowOriginStatusSwitch(CdnDomain cdnDomain, SettingCacheVo config) throws BusinessException {

    }

    @Override
    public void saveErrorCodeCache(CdnDomain cdnDomain, SettingCacheVo config) throws BusinessException {
        String domain = cdnDomain.getDomainName();
        List<ErrorCodeCacheDTO> errorCodeCache = config.getErrorCodeCache();
        GetDomainCacheTTLResponse response = getDomainCacheTTLResponse(domain);
        List<CacheTTL> cacheTTL = response.getCacheTTL();
        cacheTTL.removeIf(cacheIt -> "code".equals(cacheIt.getType()));
        for (ErrorCodeCacheDTO cacheDTO : errorCodeCache) {
            cacheTTL.add(new CacheTTL().withType("code").withValue(String.valueOf(cacheDTO.getCode())).withTtl(cacheDTO.getTtl()).withWeigth(1));
        }
        setDomainCacheTTLResponse(domain, cacheTTL);
    }

    @Override
    public void saveHotlinkPrevention(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        RefererDTO refererDTO = config.getReferer();
        String domain = cdnDomain.getDomainName();
        List<String> referers = refererDTO.getReferers();
        RefererACL acl = new RefererACL().withAllowEmpty(refererDTO.getInclude_empty());
        switch (refererDTO.getReferer_type()) {
            case 1:
                referers.forEach(acl::addBlackList);
                break;
            case 2:
                referers.forEach(acl::addWhiteList);
                break;
        }
        CdnClient client = getClient();
        SetDomainRefererACLRequest request = new SetDomainRefererACLRequest().withDomain(domain).withRefererACL(acl);
        try {
            client.setDomainRefererACL(request);
            log.info("百度CDN设置域名 {} referer 成功", domain);
        } catch (Exception e) {
            log.error("百度CDN设置域名 {} referer 失败", domain);
            throw catchException(e);
        }
    }

    @Override
    public void saveIpBlackWhiteList(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        IpACL acl = new IpACL();
        String domain = cdnDomain.getDomainName();
        List<String> ips = config.getIps();
        switch (config.getType()) {
            case 0:
                acl.setBlackList(new ArrayList<>());
                break;
            case 1:
                ips.forEach(acl::addBlackList);
                break;
            case 2:
                ips.forEach(acl::addWhiteList);
                break;
        }
        CdnClient client = getClient();
        SetDomainIpACLRequest request = new SetDomainIpACLRequest()
                .withDomain(domain)
                .withIpACL(acl);
        try {
            client.setDomainIpACL(request);
            log.info("百度CDN设置域名 {} ip 名单成功", domain);
        } catch (Exception e) {
            log.error("百度CDN设置域名 {} ip 名单失败", domain);
            throw catchException(e);
        }
    }

    @Override
    public void saveUserAgentFilter(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        UserAgentBlackAndWhiteListDTO userAgentBlackAndWhiteListDTO = config.getUserAgentBlackAndWhiteListDTO();
        String domain = cdnDomain.getDomainName();
        UaAcl uaAcl = new UaAcl();
        List<String> uaList = userAgentBlackAndWhiteListDTO.getUa_list();
        switch (userAgentBlackAndWhiteListDTO.getType()) {
            case 0:
                uaAcl.setBlackList(new ArrayList<>());
                break;
            case 1:
                uaList.forEach(uaAcl::addBlackList);
                break;
            case 2:
                uaList.forEach(uaAcl::addWhiteList);
                break;
        }
        CdnClient client = getClient();
        try {
            client.setDomainUaAcl(new SetDomainUaAclRequest().withDomain(domain).withUaAcl(uaAcl));
            log.info("百度CDN设置域名 {} ua 名单成功", domain);
        } catch (Exception e) {
            log.error("百度CDN设置域名 {} ua 名单失败", domain);
            throw catchException(e);
        }
    }

    @Override
    public void saveUrlAuth(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {

    }

    private GetDomainHttpHeaderResponse getHttpHeader(String domain) throws BusinessException {
        CdnClient client = getClient();
        try {
            GetDomainHttpHeaderResponse response = client.getDomainHttpHeader(domain);
            log.info("百度CDN获取域名 {} 请求头成功", domain);
            return response;
        } catch (Exception e) {
            log.error("百度CDN获取域名 {} 请求头失败", domain);
            throw catchException(e);
        }
    }

    private void setHttpHeader(String domain, SetDomainHttpHeaderRequest httpHeaderRequest) throws BusinessException {
        CdnClient client = getClient();
        try {
            client.setDomainHttpHeader(httpHeaderRequest);
            log.info("百度CDN设置域名 {} 请求头成功", domain);
        } catch (Exception e) {
            log.error("百度CDN设置域名 {} 请求头失败", domain);
            throw catchException(e);
        }
    }

    @Override
    public void saveHttpHeader(CdnDomain cdnDomain, SettingHigherVo config) throws BusinessException {
        String domain = cdnDomain.getDomainName();
        // 获取原有配置
        GetDomainHttpHeaderResponse response = getHttpHeader(domain);
        // 处理请求头
        SetDomainHttpHeaderRequest httpHeaderRequest = new SetDomainHttpHeaderRequest().withDomain(domain);
        List<HttpResponseHeaderDTO> httpResponseHeaders = config.getHttpResponseHeaders();
        if (!httpResponseHeaders.isEmpty()) {
            for (HttpResponseHeaderDTO header : httpResponseHeaders) {
                HttpHeader httpHeader = new HttpHeader()
                        .withType("response")
                        .withHeader(header.getName())
                        .withValue(header.getValue())
                        .withAction("delete".equals(header.getAction()) ? "remove" : "add");
                httpHeaderRequest.addHttpHeader(httpHeader);
            }
        }
        // 设置请求头
        List<HttpHeader> httpHeader = response.getHttpHeader();
        if (httpHeader != null) {
            for (HttpHeader header : httpHeader) {
                if ("origin".equals(header.getType())) {
                    httpHeaderRequest.addHttpHeader(header);
                }
            }
        }
        setHttpHeader(domain, httpHeaderRequest);
    }

    @Override
    public void saveCustomErrorPageConfiguration(CdnDomain cdnDomain, SettingHigherVo config) throws BusinessException {

    }

    @Override
    public void saveCompress(CdnDomain cdnDomain, SettingHigherVo config) throws BusinessException {
        CompressDTO compressDTO = config.getCompress();
        String domain = cdnDomain.getDomainName();
        Compress compress = new Compress().withAllow(true).withType(compressDTO.getType());
        if ("off".equals(compressDTO.getStatus())) {
            compress.withAllow(false).withType("all");
        }
        switch (compressDTO.getType()) {
            case "gzip,br":
            case "br,gzip":
                compress.withType("all");
                break;
        }
        CdnClient client = getClient();
        try {
            client.setDomainCompress(new SetDomainCompressRequest().withDomain(domain).withCompress(compress));
            log.info("百度CDN设置域名 {} compress 成功", domain);
        } catch (Exception e) {
            log.error("百度CDN设置域名 {} compress 失败", domain);
            throw catchException(e);
        }
    }

    @Override
    public DomainConfig getDomainConfig(String domainName) throws BusinessException {
        CdnClient client = getClient();

        DomainBasicInfo.SourceStationPrimaryInfo sourceStationPrimaryInfo = DomainBasicInfo.SourceStationPrimaryInfo.builder()
                .sourceStationType(OriginTypeEnum.IPADDR.getParam())
                .ipOrDomain("").httpPort("80").httpsPort("443").sourceHost(domainName)
                .build();

        DomainBasicInfo.SourceStationStandbyInfo sourceStationStandbyInfo = DomainBasicInfo.SourceStationStandbyInfo.builder()
                .sourceStationType("").ipOrDomain("").httpPort("").httpsPort("").sourceHost("")
                .build();

        DomainBasicInfo domainBasicInfo = DomainBasicInfo.builder()
                .domainName(domainName)
                .domainStatus(DomainStatus.CONFIGURING.getParam())
                .httpsStatus("0")
                .cname("")
                .businessType(BusinessTypeEnum.WEB.getParam())
                .serviceArea(ServiceAreaEnum.MAINLAND_CHINA.getParam())
                .isIpv6("0")
                .createTime(KuocaiDateUtil.strToDate("2021-01-01 00:00:00"))
                .updateTime(KuocaiDateUtil.strToDate("2021-01-01 00:00:00"))
                .sourceStationPrimaryInfo(sourceStationPrimaryInfo)
                .sourceStationStandbyInfo(sourceStationStandbyInfo)
                .build();

        ArrayList<DomainBackSourceInfo.BackSourceRequestInfo> backSourceRequestInfos = new ArrayList<>();
        ArrayList<DomainAdvancedInfo.HttpResponseHeader> httpResponseHeaders = new ArrayList<>();
        DomainBackSourceInfo domainBackSourceInfo = DomainBackSourceInfo.builder()
                .origin_protocol(OriginProtocolEnum.HTTP.getParam())
                .port(80)  // 无该配置
                .origin_receive_timeout("30")
                .origin_range_status("off")
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
                .status("off").type("https").redirect_code("302")
                .build();
        DomainHttpsInfo domainHttpsInfo = DomainHttpsInfo.builder()
                .https(httpGetBody).force_redirect(forceRedirect)
                .build();

        // 获取配置
        CompletableFuture<GetDomainConfigResponse> domainConfigResponseCompletableFuture = executeAsync(() -> client.getDomainConfig(domainName));
        CompletableFuture<GetDomainOCSPSwitchResponse> domainOCSPSwitchResponseCompletableFuture = executeAsync(() -> client.getDomainOCSPSwitch(domainName));
        CompletableFuture<GetDomainIPv6DispatchResponse> domainIPv6DispatchResponseCompletableFuture = executeAsync(() -> client.getDomainIPv6Dispatch(domainName));
        CompletableFuture<GetDomainRangeSwitchResponse> domainRangeSwitchResponseCompletableFuture = executeAsync(() -> client.getDomainRangeSwitch(domainName));
        CompletableFuture<GetDomainOriginTimeoutResponse> domainOriginTimeoutResponseCompletableFuture = executeAsync(() -> client.getDomainOriginTimeout(domainName));
        CompletableFuture<GetDomainOriginProtocolResponse> domainOriginProtocolResponseCompletableFuture = executeAsync(() -> client.getDomainOriginProtocol(domainName));
        CompletableFuture<GetDomainHttpHeaderResponse> domainHttpHeaderResponseCompletableFuture = executeAsync(() -> client.getDomainHttpHeader(domainName));
        CompletableFuture<GetDomainCacheTTLResponse> domainCacheTTLResponseCompletableFuture = executeAsync(() -> client.getDomainCacheTTL(domainName));
        CompletableFuture<GetDomainRefererACLResponse> domainRefererACLResponseCompletableFuture = executeAsync(() -> client.getDomainRefererACL(domainName));
        CompletableFuture<GetDomainIpACLResponse> domainIpACLResponseCompletableFuture = executeAsync(() -> client.getDomainIpACL(domainName));
        CompletableFuture<GetDomainUaAclResponse> domainUaAclResponseCompletableFuture = executeAsync(() -> client.getDomainUaAcl(domainName));
        CompletableFuture<GetDomainCompressResponse> domainCompressResponseCompletableFuture = executeAsync(() -> client.getDomainCompress(domainName));


        CompletableFuture.allOf(domainConfigResponseCompletableFuture, domainOCSPSwitchResponseCompletableFuture, domainRangeSwitchResponseCompletableFuture, domainOriginTimeoutResponseCompletableFuture, domainOriginProtocolResponseCompletableFuture, domainCacheTTLResponseCompletableFuture).join();

        GetDomainConfigResponse response;
        GetDomainOCSPSwitchResponse ocspSwitchResponse;
        GetDomainIPv6DispatchResponse iPv6DispatchResponse;
        GetDomainRangeSwitchResponse rangeSwitchResponse;
        GetDomainOriginTimeoutResponse originTimeoutResponse;
        GetDomainOriginProtocolResponse originProtocolResponse;
        GetDomainHttpHeaderResponse httpHeaderResponse;
        GetDomainCacheTTLResponse cacheTTLResponse;
        GetDomainRefererACLResponse refererACLResponse;
        GetDomainIpACLResponse ipACLResponse;
        GetDomainUaAclResponse uaAclResponse;
        GetDomainCompressResponse compressResponse;

        try {
            response = domainConfigResponseCompletableFuture.get();
            ocspSwitchResponse = domainOCSPSwitchResponseCompletableFuture.get();
            iPv6DispatchResponse = domainIPv6DispatchResponseCompletableFuture.get();
            rangeSwitchResponse = domainRangeSwitchResponseCompletableFuture.get();
            originTimeoutResponse = domainOriginTimeoutResponseCompletableFuture.get();
            originProtocolResponse = domainOriginProtocolResponseCompletableFuture.get();
            httpHeaderResponse = domainHttpHeaderResponseCompletableFuture.get();
            cacheTTLResponse = domainCacheTTLResponseCompletableFuture.get();
            refererACLResponse = domainRefererACLResponseCompletableFuture.get();
            ipACLResponse = domainIpACLResponseCompletableFuture.get();
            uaAclResponse = domainUaAclResponseCompletableFuture.get();
            compressResponse = domainCompressResponseCompletableFuture.get();
        } catch (Exception e) {
            throw catchException(e);
        }

        domainBasicInfo.setCname(response.getCname());
        domainBasicInfo.setServiceArea("mainland_china");
        domainBasicInfo.setDomainStatus(convertDomainStatus(response.getStatus()));
        // 时间
        domainBasicInfo.setCreateTime(KuocaiDateUtil.isoStrToDate(response.getCreateTime()));
        domainBasicInfo.setUpdateTime(KuocaiDateUtil.isoStrToDate(response.getLastModifyTime()));
        // host
        sourceStationPrimaryInfo.setSourceHost(response.getDomain());
        if (Assert.notEmpty(response.getDefaultHost())) {
            sourceStationPrimaryInfo.setSourceHost(response.getDefaultHost());
        }
        // origin
        convertSource(response.getOrigin(), sourceStationPrimaryInfo);
        // https
        if (response.getHttps().isEnabled()) {
            domainBasicInfo.setHttpsStatus("1");
            httpGetBody.setHttps_status("on");
            httpGetBody.setCertificate_name(response.getHttps().getCertId());
        }
        // http2
        if (response.getHttps().getHttp2Enabled()) {
            httpGetBody.setHttp2_status("on");
        }
        // ssl version
        httpGetBody.setTls_version(String.join(";", response.getHttps().getSslProtocols()));
        // forceRedirect
        if (response.getHttps().getHttpRedirect()) {
            forceRedirect.setStatus("on");
            forceRedirect.setType("HTTPS");
            forceRedirect.setRedirect_code(String.valueOf(response.getHttps().getHttpRedirectCode()));
        }
        if (response.getHttps().getHttpsRedirect()) {
            forceRedirect.setStatus("on");
            forceRedirect.setType("HTTP");
            forceRedirect.setRedirect_code(String.valueOf(response.getHttps().getHttpsRedirectCode()));
        }
        // ocsp
        httpGetBody.setOcsp_stapling_status(ocspSwitchResponse.isOcsp() ? "on" : "off");
        // ipv6
        domainBasicInfo.setIsIpv6(iPv6DispatchResponse.getIpv6Dispatch().isEnable() ? "1" : "0");
        // range
        domainBackSourceInfo.setOrigin_range_status(rangeSwitchResponse.getRangeSwitch());
        // timeout
        domainBackSourceInfo.setOrigin_receive_timeout(String.valueOf(originTimeoutResponse.getOriginTimeout().getLoadTimeout()));
        // protocol
        domainBackSourceInfo.setOrigin_protocol(convertOriginProtocol(originProtocolResponse.getOriginProtocol().getValue()));
        // HttpHeader
        List<HttpHeader> httpHeader = httpHeaderResponse.getHttpHeader();
        if (httpHeader != null) {
            for (HttpHeader header : httpHeader) {
                String action = "remove".equals(header.getAction()) ? "delete" : "set";
                switch (header.getType()) {
                    case "origin":
                        backSourceRequestInfos.add(DomainBackSourceInfo.BackSourceRequestInfo.builder()
                                .name(header.getHeader()).value(header.getValue()).action(action).build());
                        break;
                    case "response":
                        httpResponseHeaders.add(DomainAdvancedInfo.HttpResponseHeader.builder()
                                .name(header.getHeader()).value(header.getValue()).action(action).build());
                        break;
                }
            }
        }
        // cache
        List<CacheTTL> cacheTTL = cacheTTLResponse.getCacheTTL();
        if (Assert.notEmpty(cacheTTL)) {
            for (CacheTTL cacheIt : cacheTTL) {
                switch (cacheIt.getType()) {
                    case "code":
                        DomainCacheInfo.ErrorCodeCache errorCodeCache = DomainCacheInfo.ErrorCodeCache.builder()
                                .code(Integer.valueOf(cacheIt.getValue())).ttl(cacheIt.getTtl()).build();
                        errorCodeCaches.add(errorCodeCache);
                        break;
                    case "path":
                        DomainCacheInfo.CacheRule pathCacheRule = DomainCacheInfo.CacheRule.builder()
                                .match_type("catalog")
                                .match_value(cacheIt.getValue())
                                .ttl(KuocaiBaseUtil.getUnitCacheTime(cacheIt.getTtl()))
                                .ttl_unit(KuocaiBaseUtil.getCacheTimeUnit(cacheIt.getTtl()))
                                .priority(cacheIt.getWeight())
                                .follow_origin("off")
                                .build();
                        cacheRules.add(pathCacheRule);
                        break;
                    case "exactPath":
                        DomainCacheInfo.CacheRule fullPathCacheRule = DomainCacheInfo.CacheRule.builder()
                                .match_type("full_path")
                                .match_value(cacheIt.getValue())
                                .ttl(KuocaiBaseUtil.getUnitCacheTime(cacheIt.getTtl()))
                                .ttl_unit(KuocaiBaseUtil.getCacheTimeUnit(cacheIt.getTtl()))
                                .priority(cacheIt.getWeight())
                                .follow_origin("off")
                                .build();
                        cacheRules.add(fullPathCacheRule);
                        break;
                    case "suffix":
                        DomainCacheInfo.CacheRule fileExtensionCacheRule = DomainCacheInfo.CacheRule.builder()
                                .match_type("file_extension")
                                .match_value(cacheIt.getValue())
                                .ttl(KuocaiBaseUtil.getUnitCacheTime(cacheIt.getTtl()))
                                .ttl_unit(KuocaiBaseUtil.getCacheTimeUnit(cacheIt.getTtl()))
                                .priority(cacheIt.getWeight())
                                .follow_origin("off")
                                .build();
                        cacheRules.add(fileExtensionCacheRule);
                        break;
                }
            }
        }
        // referer
        RefererACL refererACL = refererACLResponse.getRefererACL();
        List<String> blackList = refererACL.getBlackList();
        if (Assert.notEmpty(blackList)) {
            referer.setType("black");
            referer.setReferer_type(1);
            referer.setValue(String.join("\n", blackList));
        }
        List<String> whiteList = refererACL.getWhiteList();
        if (Assert.notEmpty(whiteList)) {
            referer.setType("white");
            referer.setReferer_type(2);
            referer.setValue(String.join("\n", whiteList));
        }
        referer.setInclude_empty(refererACL.isAllowEmpty());
        // ip
        IpACL ipACL = ipACLResponse.getIpACL();
        List<String> ipBlackList = ipACL.getBlackList();
        if (Assert.notEmpty(ipBlackList)) {
            ipFilter.setType("black");
            ipFilter.setValue(String.join("\n", ipBlackList));
        }
        List<String> ipWhiteList = ipACL.getWhiteList();
        if (Assert.notEmpty(ipWhiteList)) {
            ipFilter.setType("white");
            ipFilter.setValue(String.join("\n", ipWhiteList));
        }
        // ua
        UaAcl uaAcl = uaAclResponse.getUaAcl();
        List<String> uaBlackList = uaAcl.getBlackList();
        if (Assert.notEmpty(uaBlackList)) {
            userAgentFilter.setType("black");
            userAgentFilter.setValue(String.join("\n", uaBlackList));
        }
        List<String> uaWhiteList = uaAcl.getWhiteList();
        if (Assert.notEmpty(uaWhiteList)) {
            userAgentFilter.setType("white");
            userAgentFilter.setValue(String.join("\n", uaWhiteList));
        }
        // compress
        CompressResponse compressCompress = compressResponse.getCompress();
        compress.setStatus(compressCompress.getAllow());
        switch (compressCompress.getType()) {
            case "gzip":
                compress.setType("gzip");
                break;
            case "br":
                compress.setType("br");
                break;
            case "all":
                compress.setType("gzip,br");
                break;
        }

        return DomainConfig.builder()
                .domainBasicInfo(domainBasicInfo)
                .domainBackSourceInfo(domainBackSourceInfo)
                .domainCacheInfo(domainCacheInfo)
                .domainVisitInfo(domainVisitInfo)
                .domainAdvancedInfo(domainAdvancedInfo)
                .domainHttpsInfo(domainHttpsInfo)
                .build();
    }

    private String convertOriginProtocol(String s) {
        switch (s) {
            case "http":
                return "http";
            case "https":
                return "https";
            case "*":
                return "follow";
            case "follow":
            default:
                return "*";
        }
    }

    @Override
    public DomainVerifyRecordInfo createVerifyRecord(String domainName) throws BusinessException {
        CdnClient client = getClient();
        GetHowToVerifyResponse howToVerifyResponse = GetHowToVerifyHelper.invoke(client, domainName);
        List<HowToVerify> howToVerify = howToVerifyResponse.getHowToVerify();
        if (howToVerify == null) {
            throw new BusinessException("获取验证信息失败");
        }
        HowToVerify verify = howToVerify.get(0);
        DomainTxtPair domainTxtPair = verify.getDetails().get(0);
        int len = domainTxtPair.getVerifyDomain().length();
        for (DomainTxtPair detail : verify.getDetails()) {
            if (len > detail.getVerifyDomain().length()) {
                domainTxtPair = detail;
                len = detail.getVerifyDomain().length();
            }
        }
        return DomainVerifyRecordInfo.builder()
                .subDomain("bdy-verify")
                .recordType("TXT")
                .record(domainTxtPair.getTargetTxt())
                .fileVerifyUrl("#")
                .fileVerifyDomains(new String[]{})
                .fileVerifyName("[不可用]")
                .content("")
                .build();
    }

    @Override
    public void verifyDomainRecord(String domainName, String verifyType) throws BusinessException {
        CdnClient client = getClient();
        CheckDomainValidResponse response;
        try {
            response = client.checkDomainValid(domainName);
        } catch (Exception e) {
            log.error("百度CDN验证域名 {} 错误", domainName);
            throw catchException(e);
        }
        if (response.isValid()) {
            log.info("百度CDN验证域名 {} 成功", domainName);
        } else {
            log.warn("百度CDN验证域名 {} 失败: {}", domainName, response.getMessage());
            switch (response.getMessage()) {
                case "domain DNS_TXT verify not ok":
                    response.setMessage("请检查域名的 TXT 记录是否正确");
                    break;
                case "the domain already exists":
                    response.setMessage("域名已存在，请联系客服");
                    break;
            }
            throw new BusinessException(response.getMessage());
        }
    }

    private void convertSource(List<OriginPeer> origin, DomainBasicInfo.SourceStationPrimaryInfo sourceStationPrimaryInfo) {
        Set<String> strings = new HashSet<>();
        StringBuilder s = new StringBuilder();
        for (OriginPeer originPeer : origin) {
            // 1、IPv4类型源站：https://171.107.86.35:443
            // 171.107.86.35
            // 2、IPv6类型源站：http://[240e:00a5:4200:0100::171.107.86.35]:80
            // 240e:00a5:4200:0100::171.107.86.35
            // 3、域名类型源站：http://myself.baidu.com:8080
            // myself.baidu.com
            // 正则处理 peer 获取 scheme uri protocol
            Matcher matcher = peerPattern.matcher(originPeer.getPeer());
            if (matcher.find()) {
                String scheme = matcher.group(1);
                String protocol = matcher.group(4);
                if (scheme.equals("http")) {
                    sourceStationPrimaryInfo.setHttpPort(protocol);
                } else {
                    sourceStationPrimaryInfo.setHttpsPort(protocol);
                }
                String uri = matcher.group(3) == null ? matcher.group(2) : matcher.group(3);
                if (strings.contains(uri)) {
                    continue;
                }
                strings.add(uri);
                if (s.length() > 0) {
                    s.append(";").append(uri);
                } else {
                    s.append(uri);
                }
            }
        }
        if (!strings.isEmpty() && peerDomainPattern.matcher(s).matches()) {
            sourceStationPrimaryInfo.setSourceStationType(OriginTypeEnum.DOMAIN.getParam());
        }
        sourceStationPrimaryInfo.setIpOrDomain(s.toString());
    }

    private String convertDomainStatus(String s) {
        switch (s) {
            case "RUNNING":
                return "online";
            case "STOPPED":
                return "offline";
            default:
                return "configuring";
        }
    }
}
