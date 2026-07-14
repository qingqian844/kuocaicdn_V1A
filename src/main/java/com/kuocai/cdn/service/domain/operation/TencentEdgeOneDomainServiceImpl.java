package com.kuocai.cdn.service.domain.operation;

import com.kuocai.cdn.api.DomainAdvancedInfo;
import com.kuocai.cdn.api.DomainBackSourceInfo;
import com.kuocai.cdn.api.DomainBasicInfo;
import com.kuocai.cdn.api.DomainCacheInfo;
import com.kuocai.cdn.api.DomainConfig;
import com.kuocai.cdn.api.DomainHttpsInfo;
import com.kuocai.cdn.api.DomainVerifyRecordInfo;
import com.kuocai.cdn.api.DomainVisitInfo;
import com.kuocai.cdn.api.huawei.cdn.dto.CacheRuleDTO;
import com.kuocai.cdn.api.huawei.cdn.dto.CompressDTO;
import com.kuocai.cdn.api.huawei.cdn.dto.ErrorCodeCacheDTO;
import com.kuocai.cdn.api.huawei.cdn.dto.HttpPutBodyDTO;
import com.kuocai.cdn.api.huawei.cdn.dto.HttpResponseHeaderDTO;
import com.kuocai.cdn.api.huawei.cdn.dto.RefererDTO;
import com.kuocai.cdn.api.huawei.cdn.dto.UrlAuthDTO;
import com.kuocai.cdn.api.huawei.cdn.dto.UserAgentBlackAndWhiteListDTO;
import com.kuocai.cdn.api.tencent.dns.CreateRecordResponse;
import com.kuocai.cdn.api.tencent.dns.TencentApi;
import com.kuocai.cdn.api.tencent.dns.dto.CreateRecordDTO;
import com.kuocai.cdn.api.tencent.dns.properties.TencentDns;
import com.kuocai.cdn.api.tencent.edgeone.TencentEdgeOneClient;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.entity.CdnDomainSources;
import com.kuocai.cdn.enumeration.domainmerage.CdnRoute;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.domain.operation.support.AbstractUnsupportedCdnPlatformService;
import com.kuocai.cdn.service.domain.operation.optional.ICdnDomainVerifyService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.DomainUtil;
import com.kuocai.cdn.util.KuocaiBaseUtil;
import com.kuocai.cdn.vo.CdnDomainSourcesVo;
import com.kuocai.cdn.vo.DomainHttpsSettingVo;
import com.kuocai.cdn.vo.DomainOriginSettingVo;
import com.kuocai.cdn.vo.EdgeOneSecurityPolicyVo;
import com.kuocai.cdn.vo.SettingAccessVo;
import com.kuocai.cdn.vo.SettingCacheVo;
import com.kuocai.cdn.vo.SettingHigherVo;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.teo.v20220901.TeoClient;
import com.tencentcloudapi.teo.v20220901.models.AccelerationDomain;
import com.tencentcloudapi.teo.v20220901.models.AdvancedFilter;
import com.tencentcloudapi.teo.v20220901.models.AclConfig;
import com.tencentcloudapi.teo.v20220901.models.AuthenticationParameters;
import com.tencentcloudapi.teo.v20220901.models.AscriptionInfo;
import com.tencentcloudapi.teo.v20220901.models.CreateAccelerationDomainRequest;
import com.tencentcloudapi.teo.v20220901.models.CreateAccelerationDomainResponse;
import com.tencentcloudapi.teo.v20220901.models.CreateOriginGroupRequest;
import com.tencentcloudapi.teo.v20220901.models.CreateOriginGroupResponse;
import com.tencentcloudapi.teo.v20220901.models.DeleteAccelerationDomainsRequest;
import com.tencentcloudapi.teo.v20220901.models.DeleteAccelerationDomainsResponse;
import com.tencentcloudapi.teo.v20220901.models.CustomRule;
import com.tencentcloudapi.teo.v20220901.models.CustomRules;
import com.tencentcloudapi.teo.v20220901.models.DescribeAccelerationDomainsRequest;
import com.tencentcloudapi.teo.v20220901.models.DescribeAccelerationDomainsResponse;
import com.tencentcloudapi.teo.v20220901.models.DescribeIdentificationsRequest;
import com.tencentcloudapi.teo.v20220901.models.DescribeIdentificationsResponse;
import com.tencentcloudapi.teo.v20220901.models.DescribeOriginGroupRequest;
import com.tencentcloudapi.teo.v20220901.models.DescribeOriginGroupResponse;
import com.tencentcloudapi.teo.v20220901.models.DescribeSecurityPolicyRequest;
import com.tencentcloudapi.teo.v20220901.models.DescribeSecurityPolicyResponse;
import com.tencentcloudapi.teo.v20220901.models.FileAscriptionInfo;
import com.tencentcloudapi.teo.v20220901.models.Filter;
import com.tencentcloudapi.teo.v20220901.models.Identification;
import com.tencentcloudapi.teo.v20220901.models.IdentifyZoneRequest;
import com.tencentcloudapi.teo.v20220901.models.IdentifyZoneResponse;
import com.tencentcloudapi.teo.v20220901.models.IPv6Parameters;
import com.tencentcloudapi.teo.v20220901.models.ModifyAccelerationDomainRequest;
import com.tencentcloudapi.teo.v20220901.models.ModifyAccelerationDomainResponse;
import com.tencentcloudapi.teo.v20220901.models.ModifyAccelerationDomainStatusesRequest;
import com.tencentcloudapi.teo.v20220901.models.ModifyAccelerationDomainStatusesResponse;
import com.tencentcloudapi.teo.v20220901.models.ModifyHostsCertificateRequest;
import com.tencentcloudapi.teo.v20220901.models.ModifyHostsCertificateResponse;
import com.tencentcloudapi.teo.v20220901.models.ModifyL7AccSettingRequest;
import com.tencentcloudapi.teo.v20220901.models.ModifyOriginGroupRequest;
import com.tencentcloudapi.teo.v20220901.models.ModifySecurityPolicyRequest;
import com.tencentcloudapi.teo.v20220901.models.ModifySecurityPolicyResponse;
import com.tencentcloudapi.teo.v20220901.models.OriginDetail;
import com.tencentcloudapi.teo.v20220901.models.OriginGroup;
import com.tencentcloudapi.teo.v20220901.models.OriginInfo;
import com.tencentcloudapi.teo.v20220901.models.OriginRecord;
import com.tencentcloudapi.teo.v20220901.models.SecurityAction;
import com.tencentcloudapi.teo.v20220901.models.SecurityPolicy;
import com.tencentcloudapi.teo.v20220901.models.ServerCertInfo;
import com.tencentcloudapi.teo.v20220901.models.RuleBranch;
import com.tencentcloudapi.teo.v20220901.models.RuleEngineAction;
import com.tencentcloudapi.teo.v20220901.models.RuleEngineItem;
import com.tencentcloudapi.teo.v20220901.models.SecurityConfig;
import com.tencentcloudapi.teo.v20220901.models.ZoneConfig;
import com.tencentcloudapi.teo.v20220901.models.ZoneConfigParameters;
import com.tencentcloudapi.teo.v20220901.models.CacheConfigParameters;
import com.tencentcloudapi.teo.v20220901.models.CacheConfigCustomTime;
import com.tencentcloudapi.teo.v20220901.models.CertificateInfo;
import com.tencentcloudapi.teo.v20220901.models.AdaptiveFrequencyControl;
import com.tencentcloudapi.teo.v20220901.models.AICrawlerDetection;
import com.tencentcloudapi.teo.v20220901.models.BandwidthAbuseDefense;
import com.tencentcloudapi.teo.v20220901.models.BotManagement;
import com.tencentcloudapi.teo.v20220901.models.BotManagementLite;
import com.tencentcloudapi.teo.v20220901.models.BotConfig;
import com.tencentcloudapi.teo.v20220901.models.CAPTCHAPageChallenge;
import com.tencentcloudapi.teo.v20220901.models.ChallengeActionParameters;
import com.tencentcloudapi.teo.v20220901.models.ClientFiltering;
import com.tencentcloudapi.teo.v20220901.models.CompressionParameters;
import com.tencentcloudapi.teo.v20220901.models.DescribeL7AccSettingRequest;
import com.tencentcloudapi.teo.v20220901.models.DescribeL7AccSettingResponse;
import com.tencentcloudapi.teo.v20220901.models.ExceptConfig;
import com.tencentcloudapi.teo.v20220901.models.ExceptionRule;
import com.tencentcloudapi.teo.v20220901.models.ExceptionRules;
import com.tencentcloudapi.teo.v20220901.models.FollowOrigin;
import com.tencentcloudapi.teo.v20220901.models.ForceRedirectHTTPSParameters;
import com.tencentcloudapi.teo.v20220901.models.HTTP2Parameters;
import com.tencentcloudapi.teo.v20220901.models.HttpDDoSProtection;
import com.tencentcloudapi.teo.v20220901.models.FrequentScanningProtection;
import com.tencentcloudapi.teo.v20220901.models.ManagedRuleAction;
import com.tencentcloudapi.teo.v20220901.models.ManagedRuleAutoUpdate;
import com.tencentcloudapi.teo.v20220901.models.ManagedRuleGroup;
import com.tencentcloudapi.teo.v20220901.models.ManagedRules;
import com.tencentcloudapi.teo.v20220901.models.MinimalRequestBodyTransferRate;
import com.tencentcloudapi.teo.v20220901.models.NoCache;
import com.tencentcloudapi.teo.v20220901.models.OCSPStaplingParameters;
import com.tencentcloudapi.teo.v20220901.models.RateLimitingRule;
import com.tencentcloudapi.teo.v20220901.models.RateLimitingRules;
import com.tencentcloudapi.teo.v20220901.models.RateLimitConfig;
import com.tencentcloudapi.teo.v20220901.models.RequestBodyTransferTimeout;
import com.tencentcloudapi.teo.v20220901.models.RequestFieldsForException;
import com.tencentcloudapi.teo.v20220901.models.SlowAttackDefense;
import com.tencentcloudapi.teo.v20220901.models.TLSConfigParameters;
import com.tencentcloudapi.teo.v20220901.models.UpstreamFollowRedirectParameters;
import com.tencentcloudapi.teo.v20220901.models.WafConfig;
import com.tencentcloudapi.teo.v20220901.models.Zone;
import com.tencentcloudapi.ssl.v20191205.models.UploadCertificateRequest;
import com.tencentcloudapi.ssl.v20191205.models.UploadCertificateResponse;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class TencentEdgeOneDomainServiceImpl extends AbstractUnsupportedCdnPlatformService implements ICdnDomainVerifyService {

    private static final Pattern IPV4_PATTERN = Pattern.compile("^(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}$");
    private static final String EO_RULE_REFERER = "kuocai_referer";
    private static final String EO_RULE_IP_ACL = "kuocai_ip_acl";
    private static final String EO_RULE_UA = "kuocai_user_agent";
    private static final String EO_RULE_RATE_LIMIT = "kuocai_rate_limit";
    private static final String EO_RULE_EXCEPTION = "kuocai_security_exception";
    private static final String EO_RULE_CACHE_PREFIX = "kuocai_cache_";
    private static final String EO_RULE_RESPONSE_HEADER_PREFIX = "kuocai_response_header_";
    private static final String EO_RULE_ORIGIN_FOLLOW_REDIRECT_PREFIX = "kuocai_origin_follow_redirect_";
    private static final String EO_RULE_URL_AUTH_PREFIX = "kuocai_url_auth_";
    private static final String EO_ACTION_CACHE = "Cache";
    private static final String EO_ACTION_MODIFY_RESPONSE_HEADER = "ModifyResponseHeader";
    private static final String EO_ACTION_UPSTREAM_FOLLOW_REDIRECT = "UpstreamFollowRedirect";
    private static final String EO_ACTION_AUTHENTICATION = "Authentication";
    private static final String EO_SECURITY_MODULE_MANAGED_RULES = "managed-rules";
    private static final String EO_SECURITY_MODULE_BOT_MANAGEMENT = "bot-management";
    private static final String EO_SECURITY_MODULE_BOT_MANAGEMENT_LITE = "bot-management-lite";
    private static final String EO_SECURITY_MODULE_HTTP_DDOS = "http-ddos-protection";
    private static final String EO_SECURITY_MODULE_RATE_LIMITING = "rate-limiting-rules";
    private static final String EO_SECURITY_MODULE_EXCEPTION_RULES = "exception-rules";
    private static final String EO_URL_AUTH_PARAM = "sign";
    private static final Pattern CONDITION_VALUE_PATTERN = Pattern.compile("'((?:\\\\'|[^'])*)'");
    private final Map<String, Object> domainCreateLocks = new ConcurrentHashMap<>();

    @Override
    protected String getPlatformName() {
        return "腾讯云 EdgeOne";
    }

    @Override
    public DomainVerifyRecordInfo createVerifyRecord(String domainName) throws BusinessException {
        return createVerifyRecord(domainName, null);
    }

    @Override
    public DomainVerifyRecordInfo createVerifyRecord(String domainName, String serviceArea) throws BusinessException {
        String rootDomain = TencentEdgeOneClient.getRootDomain(domainName);
        try {
            Zone zone = TencentEdgeOneClient.findZone(rootDomain);
            if (zone == null || Assert.isEmpty(zone.getZoneId())) {
                try {
                    TencentEdgeOneClient.createZone(rootDomain, serviceArea);
                } catch (BusinessException e) {
                    if (!isZoneAlreadyExistsError(e.getMessage())) {
                        throw e;
                    }
                }
            }
            Identification identification = describeIdentification(rootDomain);
            if (identification != null) {
                return buildVerifyRecordInfo(rootDomain, identification.getAscription(), identification.getFileAscription());
            }

            IdentifyZoneRequest request = new IdentifyZoneRequest();
            request.setZoneName(rootDomain);
            IdentifyZoneResponse response = TencentEdgeOneClient.getClient().IdentifyZone(request);
            return buildVerifyRecordInfo(rootDomain, response.getAscription(), response.getFileAscription());
        } catch (TencentCloudSDKException e) {
            throw new BusinessException("获取腾讯云 EdgeOne 根域名验证信息失败：" + TencentEdgeOneClient.formatTencentError(e));
        }
    }

    @Override
    public void verifyDomainRecord(String domainName, String verifyType) throws BusinessException {
        String rootDomain = TencentEdgeOneClient.getRootDomain(domainName);
        Identification identification = null;
        try {
            identification = describeIdentification(rootDomain);
            if (identification != null && "finished".equalsIgnoreCase(identification.getStatus())) {
                return;
            }
        } catch (TencentCloudSDKException e) {
            throw new BusinessException("查询腾讯云 EdgeOne 根域名验证状态失败：" + TencentEdgeOneClient.formatTencentError(e));
        }
        try {
            IdentifyZoneRequest request = new IdentifyZoneRequest();
            request.setZoneName(rootDomain);
            TencentEdgeOneClient.getClient().IdentifyZone(request);
        } catch (TencentCloudSDKException e) {
            String message = e.getMessage() == null ? "" : e.getMessage();
            if (message.contains("查询不到资源") || message.toLowerCase().contains("not found")) {
                if (identification == null) {
                    createVerifyRecord(rootDomain);
                    throw new BusinessException("腾讯云 EdgeOne 根域名站点正在创建或同步中，请稍等 1 分钟后再点击验证");
                }
            } else {
                throw new BusinessException("验证腾讯云 EdgeOne 根域名归属权失败：" + TencentEdgeOneClient.formatTencentError(e));
            }
        }
        try {
            identification = describeIdentification(rootDomain);
            if (identification != null && "finished".equalsIgnoreCase(identification.getStatus())) {
                return;
            }
        } catch (TencentCloudSDKException e) {
            throw new BusinessException("查询腾讯云 EdgeOne 根域名验证状态失败：" + TencentEdgeOneClient.formatTencentError(e));
        }
        Zone zone = TencentEdgeOneClient.findZone(rootDomain);
        if (!TencentEdgeOneClient.isVerifiedZone(zone)) {
            throw new BusinessException("腾讯云 EdgeOne 根域名归属权验证尚未通过，请检查 TXT 记录或文件验证后重试");
        }
    }

    private Identification describeIdentification(String rootDomain) throws TencentCloudSDKException, BusinessException {
        DescribeIdentificationsRequest request = new DescribeIdentificationsRequest();
        Filter filter = new Filter();
        filter.setName("zone-name");
        filter.setValues(new String[]{rootDomain});
        request.setFilters(new Filter[]{filter});
        request.setOffset(0L);
        request.setLimit(1L);
        DescribeIdentificationsResponse response = TencentEdgeOneClient.getClient().DescribeIdentifications(request);
        if (response.getIdentifications() == null || response.getIdentifications().length == 0) {
            return null;
        }
        return response.getIdentifications()[0];
    }

    private DomainVerifyRecordInfo buildVerifyRecordInfo(String rootDomain, AscriptionInfo dnsInfo, FileAscriptionInfo fileInfo) {
        String subDomain = dnsInfo == null ? "_edgeonereclaim" : dnsInfo.getSubdomain();
        String recordType = dnsInfo == null ? "TXT" : dnsInfo.getRecordType();
        String record = dnsInfo == null ? "" : dnsInfo.getRecordValue();
        String fileName = fileInfo == null ? "edgeone-verification.txt" : fileInfo.getIdentifyPath();
        String fileContent = fileInfo == null ? record : fileInfo.getIdentifyContent();
        String normalizedFileName = Assert.isEmpty(fileName) ? "" : fileName.replaceFirst("^/+", "");
        String fileUrl = Assert.isEmpty(normalizedFileName) ? "#" : "https://" + rootDomain + "/" + normalizedFileName;
        return DomainVerifyRecordInfo.builder()
                .domainName(rootDomain)
                .subDomain(subDomain)
                .recordType(recordType)
                .record(record)
                .fileVerifyDomains(new String[]{rootDomain})
                .fileVerifyUrl(fileUrl)
                .fileVerifyName(fileName)
                .content(fileContent)
                .build();
    }

    private boolean isZoneAlreadyExistsError(String message) {
        if (Assert.isEmpty(message)) {
            return false;
        }
        String lower = message.toLowerCase();
        return message.contains("已存在")
                || message.contains("重复")
                || lower.contains("already")
                || lower.contains("duplicate")
                || lower.contains("exists");
    }

    @Override
    public CdnDomain create(Long userId, String domainName, String businessType, String serviceArea, String originType, String ipOrDomain) throws BusinessException {
        return create(userId, domainName, businessType, serviceArea, originType, ipOrDomain, null, null, null, null, null);
    }

    @Override
    public CdnDomain create(Long userId, String domainName, String businessType, String serviceArea, String originType, String ipOrDomain,
                            String originProtocol, Integer httpPort, Integer httpsPort, String originHost, Integer originWeight) throws BusinessException {
        String normalizedDomain = normalize(domainName).toLowerCase();
        if (Assert.isEmpty(normalizedDomain)) {
            throw new BusinessException("加速域名不能为空");
        }
        Object createLock = domainCreateLocks.computeIfAbsent(normalizedDomain, key -> new Object());
        try {
            synchronized (createLock) {
                return createDomainLocked(userId, normalizedDomain, businessType, serviceArea, originType, ipOrDomain,
                        originProtocol, httpPort, httpsPort);
            }
        } finally {
            domainCreateLocks.remove(normalizedDomain, createLock);
        }
    }

    private CdnDomain createDomainLocked(Long userId, String domainName, String businessType, String serviceArea,
                                         String originType, String ipOrDomain, String originProtocol,
                                         Integer httpPort, Integer httpsPort) throws BusinessException {
        CdnDomain localDomain = findLocalEdgeOneDomain(domainName);
        if (localDomain != null && !userId.equals(localDomain.getUserId())) {
            throw new BusinessException("该加速域名已被其他用户添加，无法恢复到当前账号");
        }
        boolean localRecordExisted = localDomain != null;
        String zoneId = null;
        try {
            zoneId = resolveZoneIdForCreate(userId, domainName, serviceArea);
            TencentEdgeOneClient.ensureZoneBoundToConfiguredPlan(zoneId);
            localDomain = save(buildPendingCreateDomain(localDomain, userId, domainName, businessType, serviceArea, zoneId));

            if (localRecordExisted) {
                AccelerationDomain existingDomain = tryGetAccelerationDomain(zoneId, domainName);
                if (existingDomain != null) {
                    log.info("Recover existing EdgeOne domain {} from Tencent before duplicate create", domainName);
                    return completePendingCreate(localDomain, existingDomain, businessType, serviceArea, zoneId);
                }
            }

            CreateAccelerationDomainResponse response;
            try {
                response = createAccelerationDomain(domainName, originType, ipOrDomain, zoneId, originProtocol, httpPort, httpsPort);
            } catch (TencentCloudSDKException e) {
                if (!isResourceNotFound(e)) {
                    throw e;
                }
                TencentEdgeOneClient.invalidateZoneIdCache(domainName);
                zoneId = TencentEdgeOneClient.resolveZoneId(domainName, serviceArea);
                TencentEdgeOneClient.ensureZoneBoundToConfiguredPlan(zoneId);
                localDomain.setDomainId(zoneId);
                localDomain = save(localDomain);
                response = createAccelerationDomain(domainName, originType, ipOrDomain, zoneId, originProtocol, httpPort, httpsPort);
            }
            log.info("Create EdgeOne domain {} success: {}", domainName, CreateAccelerationDomainResponse.toJsonString(response));
            AccelerationDomain domain = tryGetAccelerationDomain(zoneId, domainName);
            CdnDomain savedDomain = completePendingCreate(localDomain, domain, businessType, serviceArea, zoneId);
            if (domain != null) {
                tryEnableDefaultOriginFollowRedirect(savedDomain);
            }
            return savedDomain;
        } catch (BusinessException e) {
            CdnDomain recovered = tryRecoverPendingCreate(localDomain, zoneId, domainName, businessType, serviceArea, originType, ipOrDomain, localRecordExisted);
            if (recovered != null) {
                return recovered;
            }
            markPendingCreateFailed(localDomain);
            throw e;
        } catch (TencentCloudSDKException e) {
            log.error("Create EdgeOne domain {} failed: {} - {}", domainName, e.getErrorCode(), e.getMessage());
            CdnDomain recovered = tryRecoverPendingCreate(localDomain, zoneId, domainName, businessType, serviceArea, originType, ipOrDomain, localRecordExisted);
            if (recovered != null) {
                return recovered;
            }
            markPendingCreateFailed(localDomain);
            if (isUnauthorized(e)) {
                throw new BusinessException("创建腾讯云 EdgeOne 域名失败：根域名已完成归属权验证并已写入 eo-user 授权标签，但当前 EdgeOne Secret 仍缺少 teo:CreateAccelerationDomain 创建加速域名权限，或腾讯云 CAM 策略的资源范围/条件未匹配。请在腾讯云 CAM 中给该密钥放行 EdgeOne 创建加速域名权限后重试。错误代码：" + e.getErrorCode() + "，" + TencentEdgeOneClient.formatTencentError(e));
            }
            throw new BusinessException("创建腾讯云 EdgeOne 域名失败：" + TencentEdgeOneClient.formatTencentError(e));
        } catch (Exception e) {
            log.error("Create EdgeOne domain {} failed", domainName, e);
            CdnDomain recovered = tryRecoverPendingCreate(localDomain, zoneId, domainName, businessType, serviceArea, originType, ipOrDomain, localRecordExisted);
            if (recovered != null) {
                return recovered;
            }
            markPendingCreateFailed(localDomain);
            throw new BusinessException("创建腾讯云 EdgeOne 域名失败：" + e.getMessage());
        }
    }

    private CdnDomain findLocalEdgeOneDomain(String domainName) {
        List<CdnDomain> domains = queryByWrapper(new QueryWrapper<CdnDomain>()
                .eq("route", CdnRoute.TENCENT_EDGEONE.getCode())
                .eq("domain_name", domainName)
                .last("LIMIT 1"));
        return domains == null || domains.isEmpty() ? null : domains.get(0);
    }

    private CdnDomain buildPendingCreateDomain(CdnDomain localDomain, Long userId, String domainName,
                                                String businessType, String serviceArea, String zoneId) {
        Date now = new Date();
        CdnDomain pending = localDomain == null ? new CdnDomain() : localDomain;
        pending.setUserId(userId);
        pending.setDomainName(domainName);
        pending.setBusinessType(businessType);
        pending.setServiceArea(serviceArea);
        pending.setDomainId(zoneId);
        pending.setDomainStatus("configuring");
        pending.setRoute(CdnRoute.TENCENT_EDGEONE.getCode());
        if (pending.getCreateTime() == null) {
            pending.setCreateTime(now);
        }
        pending.setUpdateTime(now);
        return pending;
    }

    private CdnDomain completePendingCreate(CdnDomain localDomain, AccelerationDomain upstreamDomain,
                                             String businessType, String serviceArea, String zoneId) {
        localDomain.setBusinessType(businessType);
        localDomain.setServiceArea(serviceArea);
        localDomain.setDomainId(zoneId);
        localDomain.setRoute(CdnRoute.TENCENT_EDGEONE.getCode());
        localDomain.setDomainStatus(convertDomainStatus(upstreamDomain == null ? "processing" : upstreamDomain.getDomainStatus()));
        if (upstreamDomain != null && Assert.notEmpty(upstreamDomain.getCname())) {
            localDomain.setCnameTencent(upstreamDomain.getCname());
        }
        localDomain.setUpdateTime(new Date());
        return save(localDomain);
    }

    private CdnDomain tryRecoverPendingCreate(CdnDomain localDomain, String zoneId, String domainName,
                                               String businessType, String serviceArea, String originType,
                                               String ipOrDomain, boolean localRecordExisted) {
        if (localDomain == null || Assert.isEmpty(zoneId)) {
            return null;
        }
        AccelerationDomain upstreamDomain = tryGetAccelerationDomain(zoneId, domainName);
        if (upstreamDomain == null) {
            return null;
        }
        if (!localRecordExisted && !isRequestedOriginMatched(upstreamDomain, originType, ipOrDomain)) {
            log.warn("Refuse to recover EdgeOne domain {} because upstream origin does not match current request", domainName);
            return null;
        }
        log.info("Recovered EdgeOne domain {} after create response failed or reported duplicate", domainName);
        return completePendingCreate(localDomain, upstreamDomain, businessType, serviceArea, zoneId);
    }

    private boolean isRequestedOriginMatched(AccelerationDomain upstreamDomain, String originType, String ipOrDomain) {
        if (upstreamDomain == null || upstreamDomain.getOriginDetail() == null) {
            return false;
        }
        OriginDetail origin = upstreamDomain.getOriginDetail();
        String requestedOrigin = firstOriginOrEmpty(ipOrDomain);
        String requestedType = convertOriginType(originType);
        return normalize(requestedOrigin).equalsIgnoreCase(normalize(origin.getOrigin()))
                && normalize(requestedType).equalsIgnoreCase(normalize(origin.getOriginType()));
    }

    private void markPendingCreateFailed(CdnDomain localDomain) {
        if (localDomain == null || localDomain.getId() == null) {
            return;
        }
        try {
            localDomain.setDomainStatus("configure_failed");
            localDomain.setUpdateTime(new Date());
            save(localDomain);
        } catch (Exception e) {
            log.error("Mark EdgeOne pending domain {} as failed error", localDomain.getDomainName(), e);
        }
    }

    private CreateAccelerationDomainResponse createAccelerationDomain(String domainName, String originType, String ipOrDomain, String zoneId,
                                                                      String originProtocol, Integer httpPort, Integer httpsPort) throws TencentCloudSDKException, BusinessException {
        CreateAccelerationDomainRequest request = new CreateAccelerationDomainRequest();
        request.setZoneId(zoneId);
        request.setDomainName(domainName);
        request.setOriginInfo(buildOriginInfo(originType, ipOrDomain, null));
        applyOriginProtocolAndPorts(request, originProtocol, httpPort, httpsPort);
        return TencentEdgeOneClient.getClient().CreateAccelerationDomain(request);
    }

    private String resolveZoneIdForCreate(Long userId, String domainName, String serviceArea) throws BusinessException {
        String storedZoneId = findReusableLocalZoneId(userId, domainName, serviceArea);
        if (Assert.notEmpty(storedZoneId)) {
            TencentEdgeOneClient.ensureZoneProjectTag(storedZoneId);
            return storedZoneId;
        }
        return TencentEdgeOneClient.resolveZoneId(domainName, serviceArea);
    }

    private String findReusableLocalZoneId(Long userId, String domainName, String serviceArea) throws BusinessException {
        String rootDomain = TencentEdgeOneClient.getRootDomain(domainName);
        List<CdnDomain> userDomains = queryByWrapper(new QueryWrapper<CdnDomain>()
                .eq("route", CdnRoute.TENCENT_EDGEONE.getCode())
                .eq("user_id", userId)
                .isNotNull("domain_id"));
        String matched = findZoneIdByRoot(userDomains, rootDomain, serviceArea);
        if (Assert.notEmpty(matched)) {
            return matched;
        }
        List<CdnDomain> allDomains = queryByWrapper(new QueryWrapper<CdnDomain>()
                .eq("route", CdnRoute.TENCENT_EDGEONE.getCode())
                .isNotNull("domain_id"));
        return findZoneIdByRoot(allDomains, rootDomain, serviceArea);
    }

    private String findZoneIdByRoot(List<CdnDomain> domains, String rootDomain, String serviceArea) {
        if (domains == null) {
            return null;
        }
        for (CdnDomain domain : domains) {
            if (domain == null || Assert.isEmpty(domain.getDomainName()) || Assert.isEmpty(domain.getDomainId())) {
                continue;
            }
            if (isDomainInZone(domain.getDomainName(), rootDomain) && isZoneAreaMatched(domain.getDomainId(), serviceArea)) {
                return domain.getDomainId();
            }
        }
        return null;
    }

    private boolean isZoneAreaMatched(String zoneId, String serviceArea) {
        if (Assert.isEmpty(serviceArea)) {
            return true;
        }
        try {
            Zone zone = TencentEdgeOneClient.findZoneById(zoneId);
            String zoneArea = toSystemServiceArea(zone == null ? null : zone.getArea());
            return Assert.isEmpty(zoneArea) || serviceArea.equals(zoneArea);
        } catch (Exception e) {
            log.warn("Skip reusing EdgeOne zone {} because area check failed: {}", zoneId, e.getMessage());
            return false;
        }
    }

    @Override
    public CdnDomain configDNS(CdnDomain cdnDomain) throws TencentCloudSDKException, BusinessException {
        if (Assert.isEmpty(cdnDomain.getCnameTencent())) {
            AccelerationDomain domain = getAccelerationDomain(cdnDomain.getDomainName());
            if (domain != null) {
                cdnDomain.setCnameTencent(domain.getCname());
            }
        }
        if (Assert.isEmpty(cdnDomain.getCnameTencent())) {
            throw new BusinessException("腾讯云 EdgeOne CNAME 为空，请稍后重试");
        }
        CreateRecordDTO createRecordDTO = new CreateRecordDTO();
        createRecordDTO.setDomain(TencentDns.LOCAL_DOMAIN_NAME)
                .setSubDomain(DomainUtil.convertSubDomain(cdnDomain.getDomainName()))
                .setValue(cdnDomain.getCnameTencent());
        CreateRecordResponse createRecordResponse = TencentApi.createRecord(createRecordDTO);
        if (Assert.isEmpty(createRecordResponse.getRecordId())) {
            throw new BusinessException("dns解析失败");
        }
        cdnDomain.setCname(createRecordDTO.getSubDomain() + "." + TencentDns.LOCAL_DOMAIN_NAME);
        cdnDomain.setTencentDnsId(createRecordResponse.getRecordId());
        return save(cdnDomain);
    }

    @Override
    public void disable(CdnDomain cdnDomain) throws BusinessException {
        changeStatus(cdnDomain, "offline");
    }

    @Override
    public void enable(CdnDomain cdnDomain) throws BusinessException {
        changeStatus(cdnDomain, "online");
    }

    @Override
    public void delete(CdnDomain cdnDomain) throws BusinessException {
        try {
            String zoneId = getZoneId(cdnDomain, true);
            if (Assert.isEmpty(zoneId)) {
                log.warn("Skip deleting EdgeOne domain {} from Tencent because its zone no longer exists", cdnDomain == null ? null : cdnDomain.getDomainName());
                return;
            }
            DeleteAccelerationDomainsRequest request = new DeleteAccelerationDomainsRequest();
            request.setZoneId(zoneId);
            request.setDomainNames(new String[]{cdnDomain.getDomainName()});
            request.setForce(true);
            DeleteAccelerationDomainsResponse response = TencentEdgeOneClient.getClient().DeleteAccelerationDomains(request);
            log.info("Delete EdgeOne domain {} success: {}", cdnDomain.getDomainName(), DeleteAccelerationDomainsResponse.toJsonString(response));
        } catch (BusinessException e) {
            throw e;
        } catch (TencentCloudSDKException e) {
            if (isResourceNotFound(e)) {
                log.warn("Skip deleting EdgeOne domain {} from Tencent because it no longer exists: {}", cdnDomain == null ? null : cdnDomain.getDomainName(), TencentEdgeOneClient.formatTencentError(e));
                return;
            }
            throw new BusinessException("删除腾讯云 EdgeOne 域名失败：" + TencentEdgeOneClient.formatTencentError(e));
        }
    }

    @Override
    public void saveSourceStationConfig(CdnDomain cdnDomain, CdnDomainSourcesVo config) throws BusinessException {
        CdnDomainSources main = config.getMain();
        if (main == null || Assert.isEmpty(main.getIpOrDomain())) {
            throw new BusinessException("主源站不能为空");
        }
        CdnDomainSources back = config.getBack();
        String backupOrigin = back == null ? null : back.getIpOrDomain();
        AccelerationDomain domain = getAccelerationDomain(cdnDomain.getDomainName());
        if (domain == null) {
            throw new BusinessException("获取腾讯云 EdgeOne 域名信息失败，域名不存在");
        }
        String originType = convertOriginType(main.getOriginType());
        String origin = firstOrigin(main.getIpOrDomain());
        String normalizedBackupOrigin = firstOriginOrEmpty(backupOrigin);
        if (isSameOriginConfig(domain.getOriginDetail(), originType, origin, normalizedBackupOrigin)) {
            log.info("Skip EdgeOne domain {} origin update because config is unchanged", cdnDomain.getDomainName());
            return;
        }
        if (isDomainBusy(domain.getDomainStatus())) {
            throw new BusinessException("腾讯云 EdgeOne 域名当前正在部署或变更中，请稍后再修改源站配置");
        }
        if (Assert.notEmpty(normalizedBackupOrigin)) {
            modifyOriginWithGroups(cdnDomain, main, back);
        } else {
            modifyOrigin(cdnDomain.getDomainName(), originType, origin, normalizedBackupOrigin);
        }
    }

    @Override
    public void saveOriginProtocol(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        ensureDomainReady(cdnDomain);
        try {
            ModifyAccelerationDomainRequest request = new ModifyAccelerationDomainRequest();
            request.setZoneId(getZoneId(cdnDomain));
            request.setDomainName(cdnDomain.getDomainName());
            request.setOriginProtocol(toEdgeOneOriginProtocol(domainOriginSettingVo.getOriginProtocol()));
            if (domainOriginSettingVo.getHttpPort() != null) {
                request.setHttpOriginPort(domainOriginSettingVo.getHttpPort().longValue());
            }
            if (domainOriginSettingVo.getHttpsPort() != null) {
                request.setHttpsOriginPort(domainOriginSettingVo.getHttpsPort().longValue());
            }
            TencentEdgeOneClient.getClient().ModifyAccelerationDomain(request);
        } catch (BusinessException e) {
            throw e;
        } catch (TencentCloudSDKException e) {
            if (isDomainStatusDenied(e)) {
                throw new BusinessException("腾讯云 EdgeOne 域名当前正在部署或变更中，请稍后再修改回源配置");
            }
            throw new BusinessException("修改腾讯云 EdgeOne 回源配置失败：" + TencentEdgeOneClient.formatTencentError(e));
        }
    }

    @Override
    public void httpsConfiguration(CdnDomain cdnDomain, DomainHttpsSettingVo config) throws BusinessException {
        if (config != null && config.getHttps() != null) {
            ensureDomainReady(cdnDomain);
            HttpPutBodyDTO https = config.getHttps();
            try {
                ModifyHostsCertificateRequest request = new ModifyHostsCertificateRequest();
                request.setZoneId(getZoneId(cdnDomain));
                request.setHosts(new String[]{cdnDomain.getDomainName()});
                if ("on".equals(normalizeSwitch(https.getHttps_status()))) {
                    String certificate = normalizePem(https.getCertificate_value());
                    String privateKey = normalizePem(https.getPrivate_key());
                    validateCertificateConfig(cdnDomain.getDomainName(), certificate, privateKey);

                    String certificateId = uploadCertificate(cdnDomain, https, certificate, privateKey);
                    ServerCertInfo serverCertInfo = new ServerCertInfo();
                    serverCertInfo.setCertId(certificateId);
                    serverCertInfo.setAlias(getCertificateAlias(cdnDomain, https));
                    request.setMode("sslcert");
                    request.setServerCertInfo(new ServerCertInfo[]{serverCertInfo});
                } else {
                    request.setMode("disable");
                }
                ModifyHostsCertificateResponse response = TencentEdgeOneClient.getClient().ModifyHostsCertificate(request);
                log.info("Modify EdgeOne domain {} HTTPS certificate success, requestId={}", cdnDomain.getDomainName(), response.getRequestId());
                return;
            } catch (BusinessException e) {
                throw e;
            } catch (TencentCloudSDKException e) {
                if (isDomainStatusDenied(e)) {
                    throw new BusinessException("EdgeOne domain is deploying or changing. Please try HTTPS certificate settings later.");
                }
                throw new BusinessException("Modify Tencent EdgeOne HTTPS certificate failed: " + TencentEdgeOneClient.formatTencentError(e));
            }
        }
        throw new BusinessException("腾讯云 EdgeOne 证书配置需要使用腾讯云 SSL 证书 ID，当前页面的 PEM 证书上传暂未对接");
    }

    @Override
    public void httpsConfigurationOther(CdnDomain cdnDomain, DomainHttpsSettingVo config) throws BusinessException {
        ensureDomainReady(cdnDomain);
        HttpPutBodyDTO https = config.getHttps();
        ZoneConfig zoneConfig = new ZoneConfig();
        if (Assert.notEmpty(https.getTls_version())) {
            TLSConfigParameters tls = new TLSConfigParameters();
            tls.setVersion(Arrays.stream(https.getTls_version().split(","))
                    .map(String::trim)
                    .filter(Assert::notEmpty)
                    .toArray(String[]::new));
            tls.setCipherSuite("loose-v2023");
            zoneConfig.setTLSConfig(tls);
        }
        if (Assert.notEmpty(https.getHttp2_status())) {
            HTTP2Parameters http2 = new HTTP2Parameters();
            http2.setSwitch(normalizeSwitch(https.getHttp2_status()));
            zoneConfig.setHTTP2(http2);
        }
        if (Assert.notEmpty(https.getOcsp_stapling_status())) {
            OCSPStaplingParameters ocsp = new OCSPStaplingParameters();
            ocsp.setSwitch(normalizeSwitch(https.getOcsp_stapling_status()));
            zoneConfig.setOCSPStapling(ocsp);
        }
        modifyL7AccSetting(cdnDomain, zoneConfig);
    }

    @Override
    public void forcedToJump(CdnDomain cdnDomain, DomainHttpsSettingVo config, String redirectCode) throws BusinessException {
        ensureDomainReady(cdnDomain);
        ForceRedirectHTTPSParameters forceRedirect = new ForceRedirectHTTPSParameters();
        boolean enabled = config.getForceRedirect() != null && "on".equals(config.getForceRedirect().getStatus());
        forceRedirect.setSwitch(enabled ? "on" : "off");
        forceRedirect.setRedirectStatusCode(Long.valueOf(enabled && Assert.notEmpty(redirectCode) ? redirectCode : "302"));
        ZoneConfig zoneConfig = new ZoneConfig();
        zoneConfig.setForceRedirectHTTPS(forceRedirect);
        modifyL7AccSetting(cdnDomain, zoneConfig);
    }

    @Override
    public void saveCacheRules(CdnDomain cdnDomain, SettingCacheVo config) throws BusinessException {
        List<CacheRuleDTO> rules = config == null ? Collections.emptyList() : config.getCacheRules();
        CacheRuleDTO globalRule = firstGlobalCacheRule(rules);
        if (globalRule != null) {
            CacheConfigParameters cache = buildGlobalCacheParameters(globalRule);
            CacheConfigParameters currentCache = getL7AccSetting(cdnDomain.getDomainName()).getCache();
            if (isSameGlobalCacheConfig(currentCache, globalRule)) {
                log.info("Skip EdgeOne global cache update because config is unchanged, domain={}", cdnDomain.getDomainName());
            } else {
                ZoneConfig zoneConfig = new ZoneConfig();
                zoneConfig.setCache(cache);
                modifyL7AccSetting(cdnDomain, zoneConfig);
            }
        }
        saveEdgeOneCacheRule(cdnDomain, nonGlobalCacheRules(rules));
    }

    @Override
    public void saveCacheFollowOriginStatusSwitch(CdnDomain cdnDomain, SettingCacheVo config) throws BusinessException {
        ensureDomainReady(cdnDomain);
        CacheConfigParameters cache = new CacheConfigParameters();
        String followOriginStatus = normalizeSwitch(config.getCacheFollowOriginStatus());
        cache.setFollowOrigin(buildFollowOrigin(followOriginStatus));
        CacheConfigCustomTime customTime = new CacheConfigCustomTime();
        customTime.setSwitch("on".equals(followOriginStatus) ? "off" : "on");
        customTime.setCacheTime(0L);
        cache.setCustomTime(customTime);
        NoCache noCache = new NoCache();
        noCache.setSwitch("off");
        cache.setNoCache(noCache);
        ZoneConfig zoneConfig = new ZoneConfig();
        zoneConfig.setCache(cache);
        modifyL7AccSetting(cdnDomain, zoneConfig);
    }

    @Override
    public void saveErrorCodeCache(CdnDomain cdnDomain, SettingCacheVo config) throws BusinessException {
        throw new BusinessException("腾讯云 EdgeOne 状态码缓存需要使用规则引擎，当前版本暂未对接");
    }

    @Override
    public void saveHotlinkPrevention(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        ensureDomainReady(cdnDomain);
        RefererDTO referer = config.getReferer();
        int type = referer == null || referer.getReferer_type() == null ? 0 : referer.getReferer_type();
        List<String> values = referer == null ? Collections.emptyList() : cleanValues(referer.getReferers());
        if (type != 0 && values.isEmpty()) {
            throw new BusinessException("Referer rules cannot be empty");
        }
        CustomRule rule = null;
        if (type != 0) {
            boolean includeEmpty = referer.getInclude_empty() != null && referer.getInclude_empty();
            String matched = buildHeaderLikeCondition("referer", values, true);
            String empty = "(not ${http.request.headers['referer']} exists or ${http.request.headers['referer']} in [''])";
            String condition = type == 2
                    ? (includeEmpty ? "not (" + matched + " or " + empty + ")" : "not (" + matched + ")")
                    : (includeEmpty ? "(" + matched + " or " + empty + ")" : matched);
            rule = buildDenyRule(EO_RULE_REFERER, condition, 10L);
        }
        saveKuocaiSecurityRule(cdnDomain, EO_RULE_REFERER, rule);
    }

    @Override
    public void ipv6(CdnDomain cdnDomain, Integer status) throws BusinessException {
        ensureDomainReady(cdnDomain);
        try {
            ModifyAccelerationDomainRequest request = buildModifyIpv6Request(
                    getZoneId(cdnDomain), cdnDomain.getDomainName(), status);
            ModifyAccelerationDomainResponse response = TencentEdgeOneClient.getClient().ModifyAccelerationDomain(request);
            log.info("Modify EdgeOne domain {} IPv6 status to {} success: {}",
                    cdnDomain.getDomainName(), request.getIPv6Status(),
                    ModifyAccelerationDomainResponse.toJsonString(response));
        } catch (BusinessException e) {
            throw e;
        } catch (TencentCloudSDKException e) {
            String error = TencentEdgeOneClient.formatTencentError(e);
            log.error("Modify EdgeOne domain {} IPv6 status failed: {}", cdnDomain.getDomainName(), error, e);
            throw new BusinessException("修改腾讯云 EdgeOne IPv6 配置失败：" + error);
        }
    }

    private ModifyAccelerationDomainRequest buildModifyIpv6Request(String zoneId, String domainName, Integer status)
            throws BusinessException {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("IPv6 状态参数错误，只能为开启或关闭");
        }
        ModifyAccelerationDomainRequest request = new ModifyAccelerationDomainRequest();
        request.setZoneId(zoneId);
        request.setDomainName(domainName);
        request.setIPv6Status(status == 1 ? "on" : "off");
        return request;
    }


    @Override
    public void saveIpBlackWhiteList(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        ensureDomainReady(cdnDomain);
        int type = config.getType() == null ? 0 : config.getType();
        List<String> values = cleanValues(config.getIps());
        if (type != 0 && values.isEmpty()) {
            throw new BusinessException("IP rules cannot be empty");
        }
        CustomRule rule = null;
        if (type != 0) {
            String matched = "${http.request.ip} in " + toConditionList(values);
            String condition = type == 2 ? "not (" + matched + ")" : matched;
            rule = buildBasicDenyRule(EO_RULE_IP_ACL, condition);
        }
        saveKuocaiSecurityRule(cdnDomain, EO_RULE_IP_ACL, rule);
    }

    @Override
    public void saveUserAgentFilter(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        ensureDomainReady(cdnDomain);
        UserAgentBlackAndWhiteListDTO ua = config.getUserAgentBlackAndWhiteListDTO();
        int type = ua == null || ua.getType() == null ? 0 : ua.getType();
        List<String> values = ua == null ? Collections.emptyList() : cleanValues(ua.getUa_list());
        if (type != 0 && values.isEmpty()) {
            throw new BusinessException("User-Agent rules cannot be empty");
        }
        CustomRule rule = null;
        if (type != 0) {
            String matched = buildHeaderLikeCondition("user-agent", values, false);
            String condition = type == 2 ? "not (" + matched + ")" : matched;
            rule = buildDenyRule(EO_RULE_UA, condition, 12L);
        }
        saveKuocaiSecurityRule(cdnDomain, EO_RULE_UA, rule);
    }

    @Override
    public void saveUrlAuth(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        ensureDomainReady(cdnDomain);
        saveEdgeOneUrlAuthRule(cdnDomain, config == null ? null : config.getUrlAuth());
    }

    @Override
    public EdgeOneSecurityPolicyVo getEdgeOneSecurityPolicy(CdnDomain cdnDomain) throws BusinessException {
        return buildEdgeOneSecurityPolicyVo(
                describeZoneDefaultSecurityPolicy(cdnDomain.getDomainName()),
                cdnDomain.getDomainName());
    }

    @Override
    public void saveEdgeOneSecurityPolicy(CdnDomain cdnDomain, EdgeOneSecurityPolicyVo config) throws BusinessException {
        ensureDomainReady(cdnDomain);
        String domainName = cdnDomain.getDomainName();
        String zoneId = getZoneId(cdnDomain);
        SecurityPolicy currentPolicy = describeZoneDefaultSecurityPolicy(domainName);
        if (currentPolicy == null) {
            currentPolicy = new SecurityPolicy();
        }

        int submittedModules = 0;
        if (shouldSubmitSecurityModule(config, EO_SECURITY_MODULE_MANAGED_RULES,
                shouldSubmitManagedRules(currentPolicy.getManagedRules(), config))) {
            SecurityPolicy update = new SecurityPolicy();
            update.setManagedRules(buildManagedRules(currentPolicy.getManagedRules(), config));
            submitSecurityPolicyModule(zoneId, domainName, EO_SECURITY_MODULE_MANAGED_RULES, update);
            submittedModules++;
        }
        if (shouldSubmitSecurityModule(config, EO_SECURITY_MODULE_BOT_MANAGEMENT,
                shouldSubmitBotManagement(currentPolicy.getBotManagement(), config))) {
            SecurityPolicy update = new SecurityPolicy();
            update.setBotManagement(buildBotManagement(config));
            submitSecurityPolicyModule(zoneId, domainName, EO_SECURITY_MODULE_BOT_MANAGEMENT, update);
            submittedModules++;
        }
        if (shouldSubmitSecurityModule(config, EO_SECURITY_MODULE_BOT_MANAGEMENT_LITE,
                shouldSubmitBotManagementLite(currentPolicy.getBotManagementLite(), config))) {
            SecurityPolicy update = new SecurityPolicy();
            update.setBotManagementLite(buildBotManagementLite(config));
            submitSecurityPolicyModule(zoneId, domainName, EO_SECURITY_MODULE_BOT_MANAGEMENT_LITE, update);
            submittedModules++;
        }
        if (shouldSubmitSecurityModule(config, EO_SECURITY_MODULE_HTTP_DDOS,
                shouldSubmitHttpDdosProtection(currentPolicy.getHttpDDoSProtection(), config))) {
            SecurityPolicy update = new SecurityPolicy();
            update.setHttpDDoSProtection(buildHttpDDoSProtection(currentPolicy.getHttpDDoSProtection(), config));
            submitSecurityPolicyModule(zoneId, domainName, EO_SECURITY_MODULE_HTTP_DDOS, update);
            submittedModules++;
        }
        if (shouldSubmitSecurityModule(config, EO_SECURITY_MODULE_RATE_LIMITING,
                shouldSubmitRateLimitingRules(currentPolicy.getRateLimitingRules(), config, domainName))) {
            SecurityPolicy update = new SecurityPolicy();
            update.setRateLimitingRules(buildRateLimitingRules(currentPolicy.getRateLimitingRules(), config, domainName));
            submitSecurityPolicyModule(zoneId, domainName, EO_SECURITY_MODULE_RATE_LIMITING, update);
            submittedModules++;
        }
        if (shouldSubmitSecurityModule(config, EO_SECURITY_MODULE_EXCEPTION_RULES,
                shouldSubmitExceptionRules(currentPolicy.getExceptionRules(), config))) {
            SecurityPolicy update = new SecurityPolicy();
            update.setExceptionRules(buildExceptionRules(currentPolicy.getExceptionRules(), config));
            submitSecurityPolicyModule(zoneId, domainName, EO_SECURITY_MODULE_EXCEPTION_RULES, update);
            submittedModules++;
        }

        if (submittedModules == 0) {
            log.info("Skip EdgeOne security policy update because no policy field changed, domain={}", domainName);
        }
    }

    private boolean shouldSubmitSecurityModule(EdgeOneSecurityPolicyVo config, String moduleName,
                                               boolean serverDetectedChange) {
        if (config == null || config.getChangedModules() == null) {
            return serverDetectedChange;
        }
        return config.getChangedModules().contains(moduleName);
    }

    @Override
    public void saveHttpHeader(CdnDomain cdnDomain, SettingHigherVo config) throws BusinessException {
        throw new BusinessException("腾讯云 EdgeOne 响应头配置需要使用规则引擎，当前版本暂未对接");
    }

    @Override
    public void saveCustomErrorPageConfiguration(CdnDomain cdnDomain, SettingHigherVo config) throws BusinessException {
        throw new BusinessException("腾讯云 EdgeOne 自定义错误页当前版本暂未对接");
    }

    @Override
    public void saveCompress(CdnDomain cdnDomain, SettingHigherVo config) throws BusinessException {
        ensureDomainReady(cdnDomain);
        CompressDTO compressDTO = config.getCompress();
        CompressionParameters compression = new CompressionParameters();
        compression.setSwitch(normalizeSwitch(compressDTO.getStatus()));
        if ("on".equals(compression.getSwitch()) && Assert.notEmpty(compressDTO.getType())) {
            compression.setAlgorithms(Arrays.stream(compressDTO.getType().split(","))
                    .map(String::trim)
                    .map(type -> "br".equals(type) ? "brotli" : type)
                    .filter(Assert::notEmpty)
                    .toArray(String[]::new));
        }
        ZoneConfig zoneConfig = new ZoneConfig();
        zoneConfig.setCompression(compression);
        modifyL7AccSetting(cdnDomain, zoneConfig);
    }

    @Override
    public DomainConfig getDomainConfig(String domainName) throws BusinessException {
        AccelerationDomain domain = getAccelerationDomain(domainName);
        if (domain == null) {
            throw new BusinessException("获取腾讯云 EdgeOne 域名信息失败，域名不存在");
        }
        ZoneConfig edgeOneConfig = getL7AccSettingOrEmpty(domainName);
        OriginDetail origin = domain.getOriginDetail();
        String originAddress = origin == null ? "" : origin.getOrigin();
        String backupOriginAddress = origin == null ? "" : origin.getBackupOrigin();
        String originType = inferSystemOriginType(origin == null ? null : origin.getOriginType(), originAddress);
        String backupOriginType = inferSystemOriginType(origin == null ? null : origin.getOriginType(), backupOriginAddress);
        DomainBasicInfo.SourceStationPrimaryInfo primaryInfo = DomainBasicInfo.SourceStationPrimaryInfo.builder()
                .sourceStationType(originType)
                .ipOrDomain(originAddress)
                .httpPort(domain.getHttpOriginPort() == null ? "80" : String.valueOf(domain.getHttpOriginPort()))
                .httpsPort(domain.getHttpsOriginPort() == null ? "443" : String.valueOf(domain.getHttpsOriginPort()))
                .sourceHost(origin != null && Assert.notEmpty(origin.getHostHeader()) ? origin.getHostHeader() : domainName)
                .build();
        DomainBasicInfo.SourceStationStandbyInfo standbyInfo = DomainBasicInfo.SourceStationStandbyInfo.builder()
                .sourceStationType(backupOriginType)
                .ipOrDomain(backupOriginAddress)
                .httpPort(domain.getHttpOriginPort() == null ? "80" : String.valueOf(domain.getHttpOriginPort()))
                .httpsPort(domain.getHttpsOriginPort() == null ? "443" : String.valueOf(domain.getHttpsOriginPort()))
                .sourceHost(origin != null && Assert.notEmpty(origin.getHostHeader()) ? origin.getHostHeader() : domainName)
                .build();
        DomainBasicInfo basicInfo = DomainBasicInfo.builder()
                .domainName(domain.getDomainName())
                .domainStatus(convertDomainStatus(domain.getDomainStatus()))
                .httpsStatus(isHttpsEnabled(domain) ? "1" : "0")
                .cname(domain.getCname())
                .businessType(getStoredBusinessType(domainName))
                .serviceArea(getStoredServiceArea(domainName))
                .isIpv6(resolveSystemIpv6Status(domain.getIPv6Status(), edgeOneConfig))
                .sourceStationPrimaryInfo(primaryInfo)
                .sourceStationStandbyInfo(standbyInfo)
                .build();
        DomainBackSourceInfo backSourceInfo = DomainBackSourceInfo.builder()
                .origin_protocol(toSystemOriginProtocol(domain.getOriginProtocol()))
                .port(domain.getHttpOriginPort() == null ? 80 : domain.getHttpOriginPort().intValue())
                .origin_receive_timeout("30")
                .origin_range_status("off")
                .slice_etag_status("off")
                .origin_request_url_rewrite(new ArrayList<>())
                .flexible_origin(new ArrayList<>())
                .origin_request_header(new ArrayList<>())
                .build();
        DomainHttpsInfo domainHttpsInfo = buildHttpsInfo(edgeOneConfig, domain);
        DomainCacheInfo domainCacheInfo = buildCacheInfo(edgeOneConfig);
        DomainVisitInfo domainVisitInfo = buildVisitInfo(domainName);
        DomainAdvancedInfo domainAdvancedInfo = buildAdvancedInfo(edgeOneConfig);
        return DomainConfig.builder()
                .domainBasicInfo(basicInfo)
                .domainBackSourceInfo(backSourceInfo)
                .domainHttpsInfo(domainHttpsInfo)
                .domainCacheInfo(domainCacheInfo)
                .domainVisitInfo(domainVisitInfo)
                .domainAdvancedInfo(domainAdvancedInfo)
                .build();
    }

    private String resolveSystemIpv6Status(String domainIpv6Status, ZoneConfig edgeOneConfig) {
        String normalizedStatus = normalize(domainIpv6Status).toLowerCase();
        if ("on".equals(normalizedStatus)) {
            return "1";
        }
        if ("off".equals(normalizedStatus)) {
            return "0";
        }
        IPv6Parameters siteIpv6 = edgeOneConfig == null ? null : edgeOneConfig.getIPv6();
        return siteIpv6 != null && "on".equals(normalizeSwitch(siteIpv6.getSwitch())) ? "1" : "0";
    }

    private void changeStatus(CdnDomain cdnDomain, String status) throws BusinessException {
        try {
            ModifyAccelerationDomainStatusesRequest request = new ModifyAccelerationDomainStatusesRequest();
            request.setZoneId(getZoneId(cdnDomain));
            request.setDomainNames(new String[]{cdnDomain.getDomainName()});
            request.setStatus(status);
            request.setForce(true);
            ModifyAccelerationDomainStatusesResponse response = TencentEdgeOneClient.getClient().ModifyAccelerationDomainStatuses(request);
            log.info("Change EdgeOne domain {} status to {} success: {}", cdnDomain.getDomainName(), status, ModifyAccelerationDomainStatusesResponse.toJsonString(response));
        } catch (BusinessException e) {
            throw e;
        } catch (TencentCloudSDKException e) {
            throw new BusinessException("修改腾讯云 EdgeOne 域名状态失败：" + TencentEdgeOneClient.formatTencentError(e));
        }
    }

    private void modifyL7AccSetting(CdnDomain cdnDomain, ZoneConfig zoneConfig) throws BusinessException {
        try {
            ModifyL7AccSettingRequest request = new ModifyL7AccSettingRequest();
            request.setZoneId(getZoneId(cdnDomain));
            request.setZoneConfig(zoneConfig);
            TencentEdgeOneClient.getClient().ModifyL7AccSetting(request);
        } catch (BusinessException e) {
            throw e;
        } catch (TencentCloudSDKException e) {
            if (isDomainStatusDenied(e)) {
                throw new BusinessException("腾讯云 EdgeOne 域名当前正在部署或变更中，请稍后再修改配置");
            }
            throw new BusinessException("修改腾讯云 EdgeOne 配置失败：" + TencentEdgeOneClient.formatTencentError(e));
        }
    }

    private SecurityPolicy describeSecurityPolicy(String domainName) throws BusinessException {
        try {
            DescribeSecurityPolicyRequest request = new DescribeSecurityPolicyRequest();
            request.setZoneId(TencentEdgeOneClient.resolveZoneId(domainName));
            request.setEntity("Host");
            request.setHost(domainName);
            DescribeSecurityPolicyResponse response = TencentEdgeOneClient.getClient().DescribeSecurityPolicy(request);
            return response.getSecurityPolicy();
        } catch (BusinessException e) {
            throw e;
        } catch (TencentCloudSDKException e) {
            throw new BusinessException("获取腾讯云 EdgeOne 安全策略失败：" + TencentEdgeOneClient.formatTencentError(e));
        }
    }

    private SecurityPolicy describeZoneDefaultSecurityPolicy(String domainName) throws BusinessException {
        try {
            DescribeSecurityPolicyRequest request = new DescribeSecurityPolicyRequest();
            request.setZoneId(TencentEdgeOneClient.resolveZoneId(domainName));
            request.setEntity("ZoneDefaultPolicy");
            DescribeSecurityPolicyResponse response = TencentEdgeOneClient.getClient().DescribeSecurityPolicy(request);
            return response.getSecurityPolicy();
        } catch (BusinessException e) {
            throw e;
        } catch (TencentCloudSDKException e) {
            throw new BusinessException("Get Tencent EdgeOne zone security policy failed: " + TencentEdgeOneClient.formatTencentError(e));
        }
    }

    private void saveKuocaiSecurityRule(CdnDomain cdnDomain, String ruleName, CustomRule replacement) throws BusinessException {
        try {
            SecurityPolicy policy = describeZoneDefaultSecurityPolicy(cdnDomain.getDomainName());
            if (policy == null) {
                policy = new SecurityPolicy();
            }
            List<CustomRule> rules = new ArrayList<>();
            CustomRule oldRule = null;
            CustomRules currentCustomRules = policy.getCustomRules();
            if (currentCustomRules != null && currentCustomRules.getRules() != null) {
                for (CustomRule rule : currentCustomRules.getRules()) {
                    if (rule == null) {
                        continue;
                    }
                    if (ruleName.equals(rule.getName())) {
                        oldRule = rule;
                        continue;
                    }
                    CustomRule copiedRule = copyCustomRuleForSubmit(rule);
                    if (copiedRule != null) {
                        rules.add(copiedRule);
                    }
                }
            }
            if (replacement != null) {
                if (oldRule != null && Assert.notEmpty(oldRule.getId())) {
                    replacement.setId(oldRule.getId());
                }
                rules.add(replacement);
            }
            CustomRules customRules = new CustomRules();
            customRules.setRules(rules.toArray(new CustomRule[0]));
            SecurityPolicy updatePolicy = new SecurityPolicy();
            updatePolicy.setCustomRules(customRules);

            ModifySecurityPolicyRequest request = buildModifyZoneDefaultSecurityPolicyRequest(getZoneId(cdnDomain), updatePolicy);
            log.info("Modify EdgeOne domain {} security rule {} request: {}",
                    cdnDomain.getDomainName(), ruleName, ModifySecurityPolicyRequest.toJsonString(request));
            ModifySecurityPolicyResponse response = TencentEdgeOneClient.getClient().ModifySecurityPolicy(request);
            log.info("Modify EdgeOne domain {} security rule {} success: {}", cdnDomain.getDomainName(), ruleName, ModifySecurityPolicyResponse.toJsonString(response));
        } catch (BusinessException e) {
            throw e;
        } catch (TencentCloudSDKException e) {
            throw new BusinessException("修改腾讯云 EdgeOne 安全策略失败：" + TencentEdgeOneClient.formatTencentError(e));
        }
    }

    private EdgeOneSecurityPolicyVo buildEdgeOneSecurityPolicyVo(SecurityPolicy policy, String domainName) {
        ManagedRules managedRules = policy == null ? null : policy.getManagedRules();
        BotManagement botManagement = policy == null ? null : policy.getBotManagement();
        BotManagementLite botLite = policy == null ? null : policy.getBotManagementLite();
        HttpDDoSProtection httpDdos = policy == null ? null : policy.getHttpDDoSProtection();
        RateLimitingRule rateRule = findKuocaiRateLimitingRule(policy == null ? null : policy.getRateLimitingRules());
        ExceptionRule exceptionRule = findKuocaiExceptionRule(policy == null ? null : policy.getExceptionRules());
        ManagedRuleAutoUpdate autoUpdate = managedRules == null ? null : managedRules.getAutoUpdate();
        SecurityAction rateAction = rateRule == null ? null : rateRule.getAction();
        ChallengeActionParameters challenge = rateAction == null ? null : rateAction.getChallengeActionParameters();
        String rateActionName = normalizeSecurityActionName(rateAction);
        return EdgeOneSecurityPolicyVo.builder()
                .managedRulesEnabled(managedRules == null ? "off" : normalizeSwitch(managedRules.getEnabled()))
                .managedRulesDetectionOnly(managedRules == null ? "off" : normalizeSwitch(managedRules.getDetectionOnly()))
                .managedRulesSemanticAnalysis(managedRules == null ? "off" : normalizeSwitch(managedRules.getSemanticAnalysis()))
                .managedRulesAutoUpdate(autoUpdate == null ? "on" : normalizeSwitch(autoUpdate.getAutoUpdateToLatestVersion()))
                .botManagementEnabled(botManagement == null ? "off" : normalizeSwitch(botManagement.getEnabled()))
                .captchaPageChallengeEnabled(botLite == null || botLite.getCAPTCHAPageChallenge() == null ? "off" : normalizeSwitch(botLite.getCAPTCHAPageChallenge().getEnabled()))
                .aiCrawlerDetectionEnabled(botLite == null || botLite.getAICrawlerDetection() == null ? "off" : normalizeSwitch(botLite.getAICrawlerDetection().getEnabled()))
                .aiCrawlerDetectionAction(botLite == null || botLite.getAICrawlerDetection() == null || botLite.getAICrawlerDetection().getAction() == null ? "Monitor" : normalizeSecurityActionName(botLite.getAICrawlerDetection().getAction()))
                .httpDdosAdaptiveFrequencyControlEnabled(httpDdos == null || httpDdos.getAdaptiveFrequencyControl() == null ? "off" : normalizeSwitch(httpDdos.getAdaptiveFrequencyControl().getEnabled()))
                .httpDdosAdaptiveFrequencyControlSensitivity(httpDdos == null || httpDdos.getAdaptiveFrequencyControl() == null ? "medium" : normalizeDefault(httpDdos.getAdaptiveFrequencyControl().getSensitivity(), "medium"))
                .httpDdosClientFilteringEnabled(httpDdos == null || httpDdos.getClientFiltering() == null ? "off" : normalizeSwitch(httpDdos.getClientFiltering().getEnabled()))
                .httpDdosBandwidthAbuseDefenseEnabled(httpDdos == null || httpDdos.getBandwidthAbuseDefense() == null ? "off" : normalizeSwitch(httpDdos.getBandwidthAbuseDefense().getEnabled()))
                .httpDdosSlowAttackDefenseEnabled(httpDdos == null || httpDdos.getSlowAttackDefense() == null ? "off" : normalizeSwitch(httpDdos.getSlowAttackDefense().getEnabled()))
                .rateLimitEnabled(rateRule == null ? "off" : normalizeSwitch(rateRule.getEnabled()))
                .rateLimitCondition(rateRule == null ? hostCondition(domainName) : normalize(rateRule.getCondition()))
                .rateLimitCountBy(rateRule == null || rateRule.getCountBy() == null ? "http.request.ip" : String.join(",", rateRule.getCountBy()))
                .rateLimitThreshold(rateRule == null || rateRule.getMaxRequestThreshold() == null ? 1000L : rateRule.getMaxRequestThreshold())
                .rateLimitPeriod(rateRule == null ? "1m" : normalizeDefault(rateRule.getCountingPeriod(), "1m"))
                .rateLimitMode(rateRule == null ? "Block" : normalizeDefault(rateRule.getMode(), "Block"))
                .rateLimitActionDuration(rateRule == null ? "10m" : normalizeDefault(rateRule.getActionDuration(), "10m"))
                .rateLimitAction(rateActionName)
                .rateLimitChallengeOption(challenge == null ? normalizeChallengeOption(rateAction) : normalizeDefault(challenge.getChallengeOption(), "ManagedChallenge"))
                .exceptionEnabled(exceptionRule == null ? "off" : normalizeSwitch(exceptionRule.getEnabled()))
                .exceptionCondition(exceptionRule == null ? "" : normalize(exceptionRule.getCondition()))
                .exceptionModules(exceptionRule == null ? "waf,rateLimiting,bot" : toLocalExceptionModules(exceptionRule.getWebSecurityModulesForException()))
                .build();
    }

    private ModifySecurityPolicyRequest buildModifySecurityPolicyRequest(String zoneId, String domainName, SecurityPolicy updatePolicy) {
        ModifySecurityPolicyRequest request = new ModifySecurityPolicyRequest();
        request.setZoneId(zoneId);
        request.setEntity("Host");
        request.setHost(domainName);
        request.setSecurityConfig(buildCompatibleSecurityConfig(updatePolicy));
        request.setSecurityPolicy(updatePolicy);
        return request;
    }

    private void submitSecurityPolicyModule(String zoneId, String domainName, String moduleName,
                                            SecurityPolicy updatePolicy) throws BusinessException {
        ModifySecurityPolicyRequest request = buildModifyZoneDefaultSecurityPolicyRequest(zoneId, updatePolicy);
        String payload = ModifySecurityPolicyRequest.toJsonString(request);
        int payloadBytes = payload.getBytes(StandardCharsets.UTF_8).length;
        log.info("Modify EdgeOne security policy, domain={}, module={}, payloadBytes={}",
                domainName, moduleName, payloadBytes);
        try {
            ModifySecurityPolicyResponse response = TencentEdgeOneClient.getClient().ModifySecurityPolicy(request);
            log.info("Modify EdgeOne security policy success, domain={}, module={}, requestId={}",
                    domainName, moduleName, response == null ? null : response.getRequestId());
        } catch (TencentCloudSDKException e) {
            throw new BusinessException("修改腾讯云 EdgeOne 安全防护策略失败（" + moduleName + "）："
                    + TencentEdgeOneClient.formatTencentError(e));
        }
    }

    private ModifySecurityPolicyRequest buildModifyZoneDefaultSecurityPolicyRequest(String zoneId, SecurityPolicy updatePolicy) {
        ModifySecurityPolicyRequest request = new ModifySecurityPolicyRequest();
        request.setZoneId(zoneId);
        request.setEntity("ZoneDefaultPolicy");
        request.setSecurityConfig(buildCompatibleSecurityConfig(updatePolicy));
        request.setSecurityPolicy(updatePolicy);
        return request;
    }

    private CustomRule copyCustomRuleForSubmit(CustomRule source) {
        if (source == null || "ManagedAccessRule".equalsIgnoreCase(normalize(source.getRuleType()))) {
            return null;
        }
        CustomRule rule = new CustomRule();
        rule.setId(source.getId());
        rule.setName(source.getName());
        rule.setCondition(source.getCondition());
        rule.setAction(copySecurityActionForSubmit(source.getAction()));
        rule.setEnabled(normalizeSwitch(source.getEnabled()));
        String ruleType = normalize(source.getRuleType());
        if ("BasicAccessRule".equalsIgnoreCase(ruleType)) {
            rule.setRuleType("BasicAccessRule");
        } else {
            rule.setRuleType("PreciseMatchRule");
            rule.setPriority(source.getPriority());
        }
        return rule;
    }

    private SecurityAction copySecurityActionForSubmit(SecurityAction source) {
        if (source == null || Assert.isEmpty(source.getName())) {
            return null;
        }
        SecurityAction action = new SecurityAction();
        action.setName(source.getName());
        switch (normalize(source.getName()).toLowerCase()) {
            case "redirect":
                action.setRedirectActionParameters(source.getRedirectActionParameters());
                break;
            case "allow":
                action.setAllowActionParameters(source.getAllowActionParameters());
                break;
            case "challenge":
            case "jschallenge":
            case "managedchallenge":
                action.setChallengeActionParameters(source.getChallengeActionParameters());
                break;
            case "blockip":
                action.setBlockIPActionParameters(source.getBlockIPActionParameters());
                break;
            case "returncustompage":
                action.setReturnCustomPageActionParameters(source.getReturnCustomPageActionParameters());
                break;
            default:
                // Deny and Monitor do not need any additional parameters. In particular,
                // the API rejects the ResponseCode returned by older Deny rule versions.
                break;
        }
        return action;
    }

    private SecurityConfig buildCompatibleSecurityConfig(SecurityPolicy updatePolicy) {
        SecurityConfig securityConfig = new SecurityConfig();
        if (updatePolicy != null && updatePolicy.getCustomRules() != null) {
            AclConfig aclConfig = new AclConfig();
            aclConfig.setSwitch("off");
            securityConfig.setAclConfig(aclConfig);
            return securityConfig;
        }
        if (updatePolicy != null && updatePolicy.getExceptionRules() != null) {
            ExceptConfig exceptConfig = new ExceptConfig();
            exceptConfig.setSwitch("off");
            securityConfig.setExceptConfig(exceptConfig);
            return securityConfig;
        }
        if (updatePolicy != null && (updatePolicy.getHttpDDoSProtection() != null
                || updatePolicy.getRateLimitingRules() != null)) {
            RateLimitConfig rateLimitConfig = new RateLimitConfig();
            rateLimitConfig.setSwitch("off");
            securityConfig.setRateLimitConfig(rateLimitConfig);
            return securityConfig;
        }
        if (updatePolicy != null && updatePolicy.getManagedRules() != null) {
            WafConfig wafConfig = new WafConfig();
            wafConfig.setSwitch("off");
            securityConfig.setWafConfig(wafConfig);
            return securityConfig;
        }
        if (updatePolicy != null && (updatePolicy.getBotManagement() != null
                || updatePolicy.getBotManagementLite() != null)) {
            BotConfig botConfig = new BotConfig();
            botConfig.setSwitch("off");
            securityConfig.setBotConfig(botConfig);
            return securityConfig;
        }
        throw new IllegalArgumentException("SecurityPolicy must contain at least one modifiable policy field");
    }

    private boolean shouldSubmitManagedRules(ManagedRules existing, EdgeOneSecurityPolicyVo config) {
        ManagedRuleAutoUpdate autoUpdate = existing == null ? null : existing.getAutoUpdate();
        String currentAutoUpdate = autoUpdate == null
                ? "on"
                : normalizeSwitch(autoUpdate.getAutoUpdateToLatestVersion());
        return !normalizeSwitch(existing == null ? null : existing.getEnabled())
                .equals(normalizeSwitch(config == null ? null : config.getManagedRulesEnabled()))
                || !normalizeSwitch(existing == null ? null : existing.getDetectionOnly())
                .equals(normalizeSwitch(config == null ? null : config.getManagedRulesDetectionOnly()))
                || !normalizeSwitch(existing == null ? null : existing.getSemanticAnalysis())
                .equals(normalizeSwitch(config == null ? null : config.getManagedRulesSemanticAnalysis()))
                || !currentAutoUpdate.equals(normalizeSwitch(config == null ? null : config.getManagedRulesAutoUpdate()));
    }

    private boolean shouldSubmitBotManagement(BotManagement existing, EdgeOneSecurityPolicyVo config) {
        return !normalizeSwitch(existing == null ? null : existing.getEnabled())
                .equals(normalizeSwitch(config == null ? null : config.getBotManagementEnabled()));
    }

    private boolean shouldSubmitBotManagementLite(BotManagementLite existing, EdgeOneSecurityPolicyVo config) {
        CAPTCHAPageChallenge captcha = existing == null ? null : existing.getCAPTCHAPageChallenge();
        AICrawlerDetection aiCrawler = existing == null ? null : existing.getAICrawlerDetection();
        String requestedAction = normalizeSecurityActionName(buildSecurityAction(
                config == null ? null : config.getAiCrawlerDetectionAction(), "ManagedChallenge"));
        return !normalizeSwitch(captcha == null ? null : captcha.getEnabled())
                .equals(normalizeSwitch(config == null ? null : config.getCaptchaPageChallengeEnabled()))
                || !normalizeSwitch(aiCrawler == null ? null : aiCrawler.getEnabled())
                .equals(normalizeSwitch(config == null ? null : config.getAiCrawlerDetectionEnabled()))
                || !normalizeSecurityActionName(aiCrawler == null ? null : aiCrawler.getAction()).equals(requestedAction);
    }

    private boolean shouldSubmitHttpDdosProtection(HttpDDoSProtection existing, EdgeOneSecurityPolicyVo config) {
        AdaptiveFrequencyControl adaptive = existing == null ? null : existing.getAdaptiveFrequencyControl();
        ClientFiltering clientFiltering = existing == null ? null : existing.getClientFiltering();
        BandwidthAbuseDefense bandwidth = existing == null ? null : existing.getBandwidthAbuseDefense();
        SlowAttackDefense slowAttack = existing == null ? null : existing.getSlowAttackDefense();
        return !normalizeSwitch(adaptive == null ? null : adaptive.getEnabled())
                .equals(normalizeSwitch(config == null ? null : config.getHttpDdosAdaptiveFrequencyControlEnabled()))
                || !normalizeDdosSensitivity(adaptive == null ? null : adaptive.getSensitivity())
                .equals(normalizeDdosSensitivity(config == null ? null : config.getHttpDdosAdaptiveFrequencyControlSensitivity()))
                || !normalizeSwitch(clientFiltering == null ? null : clientFiltering.getEnabled())
                .equals(normalizeSwitch(config == null ? null : config.getHttpDdosClientFilteringEnabled()))
                || !normalizeSwitch(bandwidth == null ? null : bandwidth.getEnabled())
                .equals(normalizeSwitch(config == null ? null : config.getHttpDdosBandwidthAbuseDefenseEnabled()))
                || !normalizeSwitch(slowAttack == null ? null : slowAttack.getEnabled())
                .equals(normalizeSwitch(config == null ? null : config.getHttpDdosSlowAttackDefenseEnabled()));
    }

    private boolean shouldSubmitRateLimitingRules(RateLimitingRules existing, EdgeOneSecurityPolicyVo config,
                                                  String domainName) {
        RateLimitingRule current = findKuocaiRateLimitingRule(existing);
        boolean requestedEnabled = "on".equals(normalizeSwitch(config == null ? null : config.getRateLimitEnabled()));
        if (!requestedEnabled) {
            return current != null;
        }
        if (current == null || !"on".equals(normalizeSwitch(current.getEnabled()))) {
            return true;
        }

        List<String> currentCountBy = current.getCountBy() == null
                ? Collections.singletonList("http.request.ip")
                : Arrays.asList(current.getCountBy());
        List<String> requestedCountBy = splitRuleValues(normalizeDefault(
                config.getRateLimitCountBy(), "http.request.ip"));
        String requestedAction = normalizeSecurityActionName(buildSecurityAction(
                config.getRateLimitAction(), config.getRateLimitChallengeOption()));
        SecurityAction currentAction = current.getAction();
        if (!normalizeDefault(current.getCondition(), hostCondition(domainName))
                .equals(normalizeDefault(config.getRateLimitCondition(), hostCondition(domainName)))
                || !normalizeRateLimitMode(current.getMode()).equals(normalizeRateLimitMode(config.getRateLimitMode()))
                || !sameRuleValues(currentCountBy, requestedCountBy)
                || !Long.valueOf(current.getMaxRequestThreshold() == null ? 1000L : current.getMaxRequestThreshold())
                .equals(config.getRateLimitThreshold() == null ? 1000L : config.getRateLimitThreshold())
                || !normalizeDefault(current.getCountingPeriod(), "1m")
                .equals(normalizeDefault(config.getRateLimitPeriod(), "1m"))
                || !normalizeDefault(current.getActionDuration(), "10m")
                .equals(normalizeDefault(config.getRateLimitActionDuration(), "10m"))
                || !normalizeSecurityActionName(currentAction).equals(requestedAction)) {
            return true;
        }
        if (!"Challenge".equals(requestedAction)) {
            return false;
        }
        ChallengeActionParameters currentChallenge = currentAction == null
                ? null
                : currentAction.getChallengeActionParameters();
        String currentOption = currentChallenge == null
                ? normalizeChallengeOption(currentAction)
                : normalizeDefault(currentChallenge.getChallengeOption(), "ManagedChallenge");
        String requestedOption = "JSChallenge".equals(normalize(config.getRateLimitChallengeOption()))
                ? "JSChallenge"
                : "ManagedChallenge";
        return !currentOption.equals(requestedOption);
    }

    private boolean shouldSubmitExceptionRules(ExceptionRules existing, EdgeOneSecurityPolicyVo config) {
        ExceptionRule current = findKuocaiExceptionRule(existing);
        boolean requestedEnabled = "on".equals(normalizeSwitch(config == null ? null : config.getExceptionEnabled()));
        if (!requestedEnabled) {
            return current != null;
        }
        if (current == null || !"on".equals(normalizeSwitch(current.getEnabled()))) {
            return true;
        }
        List<String> currentModules = current.getWebSecurityModulesForException() == null
                ? Collections.emptyList()
                : Arrays.asList(current.getWebSecurityModulesForException());
        return !normalize(current.getCondition()).equals(normalize(config.getExceptionCondition()))
                || !"WebSecurityModules".equals(normalize(current.getSkipScope()))
                || !sameRuleValues(currentModules, toEdgeOneExceptionModules(config.getExceptionModules()));
    }

    private ManagedRules buildManagedRules(ManagedRules existing, EdgeOneSecurityPolicyVo config) {
        ManagedRules managedRules = new ManagedRules();
        managedRules.setEnabled(normalizeSwitch(config == null ? null : config.getManagedRulesEnabled()));
        managedRules.setDetectionOnly(normalizeSwitch(config == null ? null : config.getManagedRulesDetectionOnly()));
        managedRules.setSemanticAnalysis(normalizeSwitch(config == null ? null : config.getManagedRulesSemanticAnalysis()));
        ManagedRuleAutoUpdate autoUpdate = new ManagedRuleAutoUpdate();
        autoUpdate.setAutoUpdateToLatestVersion("off".equals(normalizeSwitch(config == null ? null : config.getManagedRulesAutoUpdate())) ? "off" : "on");
        managedRules.setAutoUpdate(autoUpdate);

        if (existing != null && existing.getManagedRuleGroups() != null) {
            List<ManagedRuleGroup> groups = new ArrayList<>();
            for (ManagedRuleGroup source : existing.getManagedRuleGroups()) {
                ManagedRuleGroup group = copyManagedRuleGroupForSubmit(source);
                if (group != null) {
                    groups.add(group);
                }
            }
            managedRules.setManagedRuleGroups(groups.toArray(new ManagedRuleGroup[0]));
        }
        if (existing != null && existing.getFrequentScanningProtection() != null) {
            managedRules.setFrequentScanningProtection(
                    copyFrequentScanningProtectionForSubmit(existing.getFrequentScanningProtection()));
        }
        return managedRules;
    }

    private ManagedRuleGroup copyManagedRuleGroupForSubmit(ManagedRuleGroup source) {
        if (source == null || Assert.isEmpty(source.getGroupId())) {
            return null;
        }
        ManagedRuleGroup group = new ManagedRuleGroup();
        group.setGroupId(source.getGroupId());
        group.setSensitivityLevel(source.getSensitivityLevel());
        group.setAction(copySecurityActionForSubmit(source.getAction()));
        if (source.getRuleActions() != null) {
            List<ManagedRuleAction> actions = new ArrayList<>();
            for (ManagedRuleAction sourceAction : source.getRuleActions()) {
                if (sourceAction == null || Assert.isEmpty(sourceAction.getRuleId())) {
                    continue;
                }
                ManagedRuleAction action = new ManagedRuleAction();
                action.setRuleId(sourceAction.getRuleId());
                action.setAction(copySecurityActionForSubmit(sourceAction.getAction()));
                actions.add(action);
            }
            group.setRuleActions(actions.toArray(new ManagedRuleAction[0]));
        }
        return group;
    }

    private FrequentScanningProtection copyFrequentScanningProtectionForSubmit(
            FrequentScanningProtection source) {
        FrequentScanningProtection protection = new FrequentScanningProtection();
        protection.setEnabled(normalizeSwitch(source.getEnabled()));
        protection.setAction(copySecurityActionForSubmit(source.getAction()));
        protection.setCountBy(source.getCountBy());
        protection.setBlockThreshold(source.getBlockThreshold());
        protection.setCountingPeriod(source.getCountingPeriod());
        protection.setActionDuration(source.getActionDuration());
        return protection;
    }

    private BotManagement buildBotManagement(EdgeOneSecurityPolicyVo config) {
        BotManagement botManagement = new BotManagement();
        botManagement.setEnabled(normalizeSwitch(config == null ? null : config.getBotManagementEnabled()));
        return botManagement;
    }

    private BotManagementLite buildBotManagementLite(EdgeOneSecurityPolicyVo config) {
        BotManagementLite botLite = new BotManagementLite();
        CAPTCHAPageChallenge captcha = new CAPTCHAPageChallenge();
        captcha.setEnabled(normalizeSwitch(config == null ? null : config.getCaptchaPageChallengeEnabled()));
        botLite.setCAPTCHAPageChallenge(captcha);

        AICrawlerDetection aiCrawler = new AICrawlerDetection();
        aiCrawler.setEnabled(normalizeSwitch(config == null ? null : config.getAiCrawlerDetectionEnabled()));
        aiCrawler.setAction(buildSecurityAction(config == null ? null : config.getAiCrawlerDetectionAction(), "ManagedChallenge"));
        botLite.setAICrawlerDetection(aiCrawler);
        return botLite;
    }

    private HttpDDoSProtection buildHttpDDoSProtection(HttpDDoSProtection existing, EdgeOneSecurityPolicyVo config) {
        HttpDDoSProtection protection = new HttpDDoSProtection();
        AdaptiveFrequencyControl currentAdaptive = existing == null ? null : existing.getAdaptiveFrequencyControl();
        AdaptiveFrequencyControl adaptive = new AdaptiveFrequencyControl();
        adaptive.setEnabled(normalizeSwitch(config == null ? null : config.getHttpDdosAdaptiveFrequencyControlEnabled()));
        adaptive.setSensitivity(normalizeDdosSensitivity(config == null ? null : config.getHttpDdosAdaptiveFrequencyControlSensitivity()));
        adaptive.setAction(copySecurityActionOrDefault(currentAdaptive == null ? null : currentAdaptive.getAction(), "Deny"));
        protection.setAdaptiveFrequencyControl(adaptive);

        ClientFiltering currentClientFiltering = existing == null ? null : existing.getClientFiltering();
        ClientFiltering clientFiltering = new ClientFiltering();
        clientFiltering.setEnabled(normalizeSwitch(config == null ? null : config.getHttpDdosClientFilteringEnabled()));
        clientFiltering.setAction(copySecurityActionOrDefault(
                currentClientFiltering == null ? null : currentClientFiltering.getAction(), "Deny"));
        protection.setClientFiltering(clientFiltering);

        BandwidthAbuseDefense currentBandwidth = existing == null ? null : existing.getBandwidthAbuseDefense();
        BandwidthAbuseDefense bandwidth = new BandwidthAbuseDefense();
        bandwidth.setEnabled(normalizeSwitch(config == null ? null : config.getHttpDdosBandwidthAbuseDefenseEnabled()));
        bandwidth.setAction(copySecurityActionOrDefault(currentBandwidth == null ? null : currentBandwidth.getAction(), "Deny"));
        protection.setBandwidthAbuseDefense(bandwidth);

        SlowAttackDefense currentSlowAttack = existing == null ? null : existing.getSlowAttackDefense();
        SlowAttackDefense slowAttack = new SlowAttackDefense();
        slowAttack.setEnabled(normalizeSwitch(config == null ? null : config.getHttpDdosSlowAttackDefenseEnabled()));
        slowAttack.setAction(copySecurityActionOrDefault(currentSlowAttack == null ? null : currentSlowAttack.getAction(), "Deny"));
        if (currentSlowAttack != null && currentSlowAttack.getMinimalRequestBodyTransferRate() != null) {
            slowAttack.setMinimalRequestBodyTransferRate(copyMinimalRequestBodyTransferRate(
                    currentSlowAttack.getMinimalRequestBodyTransferRate()));
        }
        if (currentSlowAttack != null && currentSlowAttack.getRequestBodyTransferTimeout() != null) {
            slowAttack.setRequestBodyTransferTimeout(copyRequestBodyTransferTimeout(
                    currentSlowAttack.getRequestBodyTransferTimeout()));
        }
        protection.setSlowAttackDefense(slowAttack);
        return protection;
    }

    private SecurityAction copySecurityActionOrDefault(SecurityAction source, String defaultAction) {
        SecurityAction copied = copySecurityActionForSubmit(source);
        return copied == null ? buildSecurityAction(defaultAction, null) : copied;
    }

    private MinimalRequestBodyTransferRate copyMinimalRequestBodyTransferRate(
            MinimalRequestBodyTransferRate source) {
        MinimalRequestBodyTransferRate result = new MinimalRequestBodyTransferRate();
        result.setMinimalAvgTransferRateThreshold(source.getMinimalAvgTransferRateThreshold());
        result.setCountingPeriod(source.getCountingPeriod());
        result.setEnabled(normalizeSwitch(source.getEnabled()));
        return result;
    }

    private RequestBodyTransferTimeout copyRequestBodyTransferTimeout(RequestBodyTransferTimeout source) {
        RequestBodyTransferTimeout result = new RequestBodyTransferTimeout();
        result.setIdleTimeout(source.getIdleTimeout());
        result.setEnabled(normalizeSwitch(source.getEnabled()));
        return result;
    }

    private RateLimitingRules buildRateLimitingRules(RateLimitingRules existing, EdgeOneSecurityPolicyVo config, String domainName) throws BusinessException {
        List<RateLimitingRule> rules = new ArrayList<>();
        RateLimitingRule oldRule = null;
        if (existing != null && existing.getRules() != null) {
            for (RateLimitingRule rule : existing.getRules()) {
                if (rule == null) {
                    continue;
                }
                if (EO_RULE_RATE_LIMIT.equals(rule.getName())) {
                    oldRule = rule;
                    continue;
                }
                RateLimitingRule copiedRule = copyRateLimitingRuleForSubmit(rule);
                if (copiedRule != null) {
                    rules.add(copiedRule);
                }
            }
        }
        if (config != null && "on".equals(normalizeSwitch(config.getRateLimitEnabled()))) {
            Long threshold = config.getRateLimitThreshold() == null ? 1000L : config.getRateLimitThreshold();
            if (threshold < 1 || threshold > 100000) {
                throw new BusinessException("速率限制阈值必须在 1 - 100000 之间");
            }
            List<String> countBy = splitRuleValues(normalizeDefault(config.getRateLimitCountBy(), "http.request.ip"));
            if (countBy.isEmpty() || countBy.size() > 5) {
                throw new BusinessException("速率限制统计维度必须配置 1 - 5 个");
            }
            RateLimitingRule rule = new RateLimitingRule();
            if (oldRule != null && Assert.notEmpty(oldRule.getId())) {
                rule.setId(oldRule.getId());
            }
            rule.setName(EO_RULE_RATE_LIMIT);
            rule.setCondition(normalizeDefault(config.getRateLimitCondition(), hostCondition(domainName)));
            rule.setMode(normalizeRateLimitMode(config.getRateLimitMode()));
            rule.setCountBy(countBy.toArray(new String[0]));
            rule.setMaxRequestThreshold(threshold);
            rule.setCountingPeriod(normalizeDefault(config.getRateLimitPeriod(), "1m"));
            rule.setActionDuration(normalizeDefault(config.getRateLimitActionDuration(), "10m"));
            rule.setAction(buildSecurityAction(config.getRateLimitAction(), config.getRateLimitChallengeOption()));
            rule.setPriority(20L);
            rule.setEnabled("on");
            rules.add(rule);
        }
        RateLimitingRules result = new RateLimitingRules();
        result.setRules(rules.toArray(new RateLimitingRule[0]));
        return result;
    }

    private RateLimitingRule copyRateLimitingRuleForSubmit(RateLimitingRule source) {
        if (source == null) {
            return null;
        }
        RateLimitingRule rule = new RateLimitingRule();
        rule.setId(source.getId());
        rule.setName(source.getName());
        rule.setCondition(source.getCondition());
        rule.setMode(source.getMode());
        rule.setCountBy(source.getCountBy() == null ? null : source.getCountBy().clone());
        rule.setMaxRequestThreshold(source.getMaxRequestThreshold());
        rule.setCountingPeriod(source.getCountingPeriod());
        rule.setActionDuration(source.getActionDuration());
        rule.setAction(copySecurityActionForSubmit(source.getAction()));
        rule.setPriority(source.getPriority());
        rule.setEnabled(normalizeSwitch(source.getEnabled()));
        return rule;
    }

    private ExceptionRules buildExceptionRules(ExceptionRules existing, EdgeOneSecurityPolicyVo config) throws BusinessException {
        List<ExceptionRule> rules = new ArrayList<>();
        ExceptionRule oldRule = null;
        if (existing != null && existing.getRules() != null) {
            for (ExceptionRule rule : existing.getRules()) {
                if (rule == null) {
                    continue;
                }
                if (EO_RULE_EXCEPTION.equals(rule.getName())) {
                    oldRule = rule;
                    continue;
                }
                ExceptionRule copiedRule = copyExceptionRuleForSubmit(rule);
                if (copiedRule != null) {
                    rules.add(copiedRule);
                }
            }
        }
        if (config != null && "on".equals(normalizeSwitch(config.getExceptionEnabled()))) {
            String condition = normalize(config.getExceptionCondition());
            if (Assert.isEmpty(condition)) {
                throw new BusinessException("例外规则开启时必须填写匹配条件");
            }
            List<String> modules = toEdgeOneExceptionModules(config.getExceptionModules());
            if (modules.isEmpty()) {
                throw new BusinessException("例外规则至少选择一个跳过模块");
            }
            ExceptionRule rule = new ExceptionRule();
            if (oldRule != null && Assert.notEmpty(oldRule.getId())) {
                rule.setId(oldRule.getId());
            }
            rule.setName(EO_RULE_EXCEPTION);
            rule.setCondition(condition);
            rule.setSkipScope("WebSecurityModules");
            rule.setWebSecurityModulesForException(modules.toArray(new String[0]));
            rule.setEnabled("on");
            rules.add(rule);
        }
        ExceptionRules result = new ExceptionRules();
        result.setRules(rules.toArray(new ExceptionRule[0]));
        return result;
    }

    private ExceptionRule copyExceptionRuleForSubmit(ExceptionRule source) {
        if (source == null) {
            return null;
        }
        ExceptionRule rule = new ExceptionRule();
        rule.setId(source.getId());
        rule.setName(source.getName());
        rule.setCondition(source.getCondition());
        rule.setSkipScope(source.getSkipScope());
        rule.setSkipOption(source.getSkipOption());
        rule.setWebSecurityModulesForException(cloneArray(source.getWebSecurityModulesForException()));
        rule.setManagedRulesForException(cloneArray(source.getManagedRulesForException()));
        rule.setManagedRuleGroupsForException(cloneArray(source.getManagedRuleGroupsForException()));
        if (source.getRequestFieldsForException() != null) {
            RequestFieldsForException[] requestFields = new RequestFieldsForException[
                    source.getRequestFieldsForException().length];
            for (int i = 0; i < source.getRequestFieldsForException().length; i++) {
                RequestFieldsForException field = source.getRequestFieldsForException()[i];
                requestFields[i] = field == null ? null : new RequestFieldsForException(field);
            }
            rule.setRequestFieldsForException(requestFields);
        }
        rule.setEnabled(normalizeSwitch(source.getEnabled()));
        return rule;
    }

    private String[] cloneArray(String[] values) {
        return values == null ? null : values.clone();
    }

    private RateLimitingRule findKuocaiRateLimitingRule(RateLimitingRules rules) {
        if (rules == null || rules.getRules() == null) {
            return null;
        }
        for (RateLimitingRule rule : rules.getRules()) {
            if (rule != null && EO_RULE_RATE_LIMIT.equals(rule.getName())) {
                return rule;
            }
        }
        return null;
    }

    private ExceptionRule findKuocaiExceptionRule(ExceptionRules rules) {
        if (rules == null || rules.getRules() == null) {
            return null;
        }
        for (ExceptionRule rule : rules.getRules()) {
            if (rule != null && EO_RULE_EXCEPTION.equals(rule.getName())) {
                return rule;
            }
        }
        return null;
    }

    private SecurityAction buildSecurityAction(String actionName, String challengeOption) {
        String normalized = normalizeDefault(actionName, "Monitor");
        if (!"Deny".equals(normalized) && !"Challenge".equals(normalized) && !"Monitor".equals(normalized)) {
            normalized = "Monitor";
        }
        SecurityAction action = new SecurityAction();
        action.setName(normalized);
        if ("Challenge".equals(normalized)) {
            ChallengeActionParameters parameters = new ChallengeActionParameters();
            String option = normalizeDefault(challengeOption, "ManagedChallenge");
            parameters.setChallengeOption("JSChallenge".equals(option) ? "JSChallenge" : "ManagedChallenge");
            action.setChallengeActionParameters(parameters);
        }
        return action;
    }

    private String normalizeSecurityActionName(SecurityAction action) {
        if (action == null || Assert.isEmpty(action.getName())) {
            return "Monitor";
        }
        String name = normalize(action.getName());
        if ("ManagedChallenge".equals(name) || "JSChallenge".equals(name)) {
            return "Challenge";
        }
        if (!"Deny".equals(name) && !"Challenge".equals(name) && !"Monitor".equals(name)) {
            return "Monitor";
        }
        return name;
    }

    private String normalizeChallengeOption(SecurityAction action) {
        if (action == null || Assert.isEmpty(action.getName())) {
            return "ManagedChallenge";
        }
        return "JSChallenge".equals(action.getName()) ? "JSChallenge" : "ManagedChallenge";
    }

    private String normalizeRateLimitMode(String mode) {
        return "Throttle".equals(normalize(mode)) ? "Throttle" : "Block";
    }

    private String normalizeDdosSensitivity(String sensitivity) {
        String normalized = normalize(sensitivity).toLowerCase();
        if ("low".equals(normalized) || "high".equals(normalized)) {
            return normalized;
        }
        return "medium";
    }

    private String hostCondition(String domainName) {
        return "${http.request.host} in ['" + escapeConditionValue(domainName) + "']";
    }

    private String normalizeDefault(String value, String defaultValue) {
        String normalized = normalize(value);
        return Assert.isEmpty(normalized) ? defaultValue : normalized;
    }

    private List<String> splitRuleValues(String value) {
        if (Assert.isEmpty(value)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String item : value.split("[,;，；]")) {
            String normalized = normalize(item);
            if (Assert.notEmpty(normalized)) {
                result.add(normalized);
            }
        }
        return result;
    }

    private boolean sameRuleValues(List<String> first, List<String> second) {
        return normalizeRuleValues(first).equals(normalizeRuleValues(second));
    }

    private List<String> normalizeRuleValues(List<String> values) {
        List<String> normalized = new ArrayList<>();
        if (values != null) {
            for (String value : values) {
                String item = normalize(value);
                if (!Assert.isEmpty(item) && !normalized.contains(item)) {
                    normalized.add(item);
                }
            }
        }
        Collections.sort(normalized);
        return normalized;
    }

    private List<String> toEdgeOneExceptionModules(String modules) {
        List<String> result = new ArrayList<>();
        for (String module : splitRuleValues(normalizeDefault(modules, "waf,rateLimiting,bot"))) {
            String edgeModule;
            switch (module) {
                case "rateLimiting":
                    edgeModule = "websec-mod-rate-limiting";
                    break;
                case "customRules":
                    edgeModule = "websec-mod-custom-rules";
                    break;
                case "adaptiveControl":
                    edgeModule = "websec-mod-adaptive-control";
                    break;
                case "bot":
                    edgeModule = "websec-mod-bot";
                    break;
                case "managedRules":
                case "waf":
                default:
                    edgeModule = "websec-mod-managed-rules";
                    break;
            }
            if (!result.contains(edgeModule)) {
                result.add(edgeModule);
            }
        }
        return result;
    }

    private String toLocalExceptionModules(String[] modules) {
        if (modules == null || modules.length == 0) {
            return "waf,rateLimiting,bot";
        }
        List<String> result = new ArrayList<>();
        for (String module : modules) {
            String local;
            switch (normalize(module)) {
                case "websec-mod-rate-limiting":
                    local = "rateLimiting";
                    break;
                case "websec-mod-custom-rules":
                    local = "customRules";
                    break;
                case "websec-mod-adaptive-control":
                    local = "adaptiveControl";
                    break;
                case "websec-mod-bot":
                    local = "bot";
                    break;
                case "websec-mod-managed-rules":
                default:
                    local = "waf";
                    break;
            }
            if (!result.contains(local)) {
                result.add(local);
            }
        }
        return String.join(",", result);
    }

    private CustomRule buildDenyRule(String name, String condition, Long priority) {
        CustomRule rule = new CustomRule();
        rule.setName(name);
        rule.setCondition(condition);
        rule.setEnabled("on");
        rule.setRuleType("PreciseMatchRule");
        rule.setPriority(priority);
        SecurityAction action = new SecurityAction();
        action.setName("Deny");
        rule.setAction(action);
        return rule;
    }

    private CustomRule buildBasicDenyRule(String name, String condition) {
        CustomRule rule = new CustomRule();
        rule.setName(name);
        rule.setCondition(condition);
        rule.setEnabled("on");
        rule.setRuleType("BasicAccessRule");
        SecurityAction action = new SecurityAction();
        action.setName("Deny");
        rule.setAction(action);
        return rule;
    }

    private String buildHeaderLikeCondition(String headerName, List<String> values, boolean wrapWithoutWildcard) {
        List<String> exactValues = new ArrayList<>();
        List<String> containValues = new ArrayList<>();
        List<String> wildcardValues = new ArrayList<>();
        for (String value : values) {
            String item = normalize(value);
            if (item.contains("*") || item.contains("?")) {
                wildcardValues.add(item);
            } else if (wrapWithoutWildcard) {
                wildcardValues.add("*" + item + "*");
            } else {
                exactValues.add(item);
            }
        }
        List<String> expressions = new ArrayList<>();
        if (!exactValues.isEmpty()) {
            expressions.add("${http.request.headers['" + headerName + "']} in " + toConditionList(exactValues));
        }
        if (!wildcardValues.isEmpty()) {
            expressions.add("${http.request.headers['" + headerName + "']} like " + toConditionList(wildcardValues));
        }
        return "(" + String.join(" or ", expressions) + ")";
    }

    private String toConditionList(List<String> values) {
        List<String> escaped = new ArrayList<>();
        for (String value : values) {
            escaped.add("'" + escapeConditionValue(value) + "'");
        }
        return "[" + String.join(", ", escaped) + "]";
    }

    private String escapeConditionValue(String value) {
        return normalize(value).replace("\\", "\\\\").replace("'", "\\'");
    }

    private List<String> cleanValues(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> clean = new ArrayList<>();
        for (String value : values) {
            String normalized = normalize(value);
            if (Assert.notEmpty(normalized)) {
                clean.add(normalized);
            }
        }
        return clean;
    }

    private void applyKuocaiRuleToVisitInfo(CustomRule rule, DomainVisitInfo visitInfo) {
        if (rule == null || !"on".equalsIgnoreCase(normalize(rule.getEnabled()))) {
            return;
        }
        String name = rule.getName();
        String condition = normalize(rule.getCondition());
        if (EO_RULE_REFERER.equals(name)) {
            List<String> values = parseConditionValues(condition);
            boolean white = isWhitelistCondition(condition);
            visitInfo.setReferer(DomainVisitInfo.Referer.builder()
                    .type(white ? "white" : "black")
                    .referer_type(white ? 2 : 1)
                    .value(String.join("\n", values))
                    .include_empty(condition.contains("headers['referer']} in ['']"))
                    .build());
        } else if (EO_RULE_IP_ACL.equals(name)) {
            List<String> values = parseConditionValues(condition);
            boolean white = isWhitelistCondition(condition);
            visitInfo.setIp_filter(DomainVisitInfo.IpFilter.builder()
                    .type(white ? "white" : "black")
                    .value(String.join("\n", values))
                    .build());
        } else if (EO_RULE_UA.equals(name)) {
            List<String> values = parseConditionValues(condition);
            boolean white = isWhitelistCondition(condition);
            visitInfo.setUser_agent_filter(DomainVisitInfo.UserAgentFilter.builder()
                    .type(white ? "white" : "black")
                    .value(String.join("\n", values))
                    .ua_list(values)
                    .build());
        }
    }

    private boolean isWhitelistCondition(String condition) {
        return normalize(condition).startsWith("not (");
    }

    private List<String> parseConditionValues(String condition) {
        List<String> values = new ArrayList<>();
        Matcher matcher = CONDITION_VALUE_PATTERN.matcher(condition);
        while (matcher.find()) {
            String value = matcher.group(1).replace("\\'", "'").replace("\\\\", "\\");
            if (Assert.isEmpty(value) || "referer".equalsIgnoreCase(value) || "user-agent".equalsIgnoreCase(value)) {
                continue;
            }
            if (!values.contains(value)) {
                values.add(value);
            }
        }
        return values;
    }

    private ZoneConfig getL7AccSetting(String domainName) throws BusinessException {
        try {
            DescribeL7AccSettingRequest request = new DescribeL7AccSettingRequest();
            request.setZoneId(TencentEdgeOneClient.resolveZoneId(domainName));
            DescribeL7AccSettingResponse response = TencentEdgeOneClient.getClient().DescribeL7AccSetting(request);
            ZoneConfigParameters setting = response.getZoneSetting();
            return setting == null || setting.getZoneConfig() == null ? new ZoneConfig() : setting.getZoneConfig();
        } catch (BusinessException e) {
            throw e;
        } catch (TencentCloudSDKException e) {
            throw new BusinessException("获取腾讯云 EdgeOne 配置失败：" + TencentEdgeOneClient.formatTencentError(e));
        }
    }

    private ZoneConfig getL7AccSettingOrEmpty(String domainName) {
        try {
            return getL7AccSetting(domainName);
        } catch (BusinessException e) {
            log.warn("Get EdgeOne L7 setting failed for {}, use empty domain config: {}", domainName, e.getMessage());
            return new ZoneConfig();
        }
    }

    private void ensureDomainReady(CdnDomain cdnDomain) throws BusinessException {
        AccelerationDomain domain = getAccelerationDomain(cdnDomain.getDomainName());
        if (domain == null) {
            throw new BusinessException("获取腾讯云 EdgeOne 域名信息失败，域名不存在");
        }
        if (isDomainBusy(domain.getDomainStatus())) {
            throw new BusinessException("腾讯云 EdgeOne 域名当前正在部署或变更中，请稍后再修改配置");
        }
    }

    private String uploadCertificate(CdnDomain cdnDomain, HttpPutBodyDTO https, String certificate, String privateKey) throws BusinessException, TencentCloudSDKException {
        UploadCertificateRequest request = new UploadCertificateRequest();
        request.setCertificatePublicKey(certificate);
        request.setCertificatePrivateKey(privateKey);
        request.setCertificateType("SVR");
        request.setAlias(getCertificateAlias(cdnDomain, https));
        UploadCertificateResponse response = TencentEdgeOneClient.getSslClient().UploadCertificate(request);
        if (Assert.isEmpty(response.getCertificateId())) {
            throw new BusinessException("Tencent SSL did not return CertificateId.");
        }
        log.info("Upload EdgeOne domain {} certificate to Tencent SSL success, certificateId={}, requestId={}",
                cdnDomain.getDomainName(), response.getCertificateId(), response.getRequestId());
        return response.getCertificateId();
    }

    private String getCertificateAlias(CdnDomain cdnDomain, HttpPutBodyDTO https) {
        String alias = https == null ? null : https.getCertificate_name();
        if (Assert.isEmpty(alias)) {
            alias = "edgeone-" + cdnDomain.getDomainName();
        }
        return alias.trim();
    }

    private String normalizePem(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .replace("\\r\\n", "\n")
                .replace("\\n", "\n")
                .replace("\r\n", "\n")
                .replace('\r', '\n');
    }

    private void validateCertificateConfig(String domainName, String certificate, String privateKey) throws BusinessException {
        if (Assert.isEmpty(certificate)
                || !certificate.contains("-----BEGIN CERTIFICATE-----")
                || !certificate.contains("-----END CERTIFICATE-----")) {
            throw new BusinessException("Certificate content is invalid. Please use PEM certificate for " + domainName + ".");
        }
        if (Assert.isEmpty(privateKey)
                || (!privateKey.contains("-----BEGIN PRIVATE KEY-----") && !privateKey.contains("-----BEGIN RSA PRIVATE KEY-----"))
                || (!privateKey.contains("-----END PRIVATE KEY-----") && !privateKey.contains("-----END RSA PRIVATE KEY-----"))) {
            throw new BusinessException("Private key content is invalid. Please use PEM private key for " + domainName + ".");
        }
    }

    private String getStoredServiceArea(String domainName) {
        if (Assert.isEmpty(domainName)) {
            return "mainland_china";
        }
        CdnDomain local = getLocalEdgeOneDomain(domainName);
        if (local != null && Assert.notEmpty(local.getServiceArea())) {
            return local.getServiceArea();
        }
        try {
            Zone zone = TencentEdgeOneClient.findZone(domainName);
            String zoneArea = toSystemServiceArea(zone == null ? null : zone.getArea());
            if (Assert.notEmpty(zoneArea)) {
                return zoneArea;
            }
        } catch (Exception e) {
            log.warn("Get EdgeOne zone area failed for {}, fallback to local value: {}", domainName, e.getMessage());
        }
        return "mainland_china";
    }

    private CdnDomain getLocalEdgeOneDomain(String domainName) {
        if (Assert.isEmpty(domainName)) {
            return null;
        }
        List<CdnDomain> localDomains = queryByWrapper(new QueryWrapper<CdnDomain>()
                .eq("domain_name", domainName)
                .eq("route", CdnRoute.TENCENT_EDGEONE.getCode())
                .last("limit 1"));
        return localDomains == null || localDomains.isEmpty() ? null : localDomains.get(0);
    }

    private String toSystemServiceArea(String area) {
        String normalized = normalize(area).toLowerCase();
        if ("mainland".equals(normalized) || "mainland_china".equals(normalized)) {
            return "mainland_china";
        }
        if ("overseas".equals(normalized) || "outside_mainland_china".equals(normalized)) {
            return "outside_mainland_china";
        }
        if ("global".equals(normalized)) {
            return "global";
        }
        return null;
    }

    private String getStoredBusinessType(String domainName) {
        CdnDomain local = getLocalEdgeOneDomain(domainName);
        if (local != null && Assert.notEmpty(local.getBusinessType())) {
            return local.getBusinessType();
        }
        return "web";
    }

    private DomainHttpsInfo buildHttpsInfo(ZoneConfig config, AccelerationDomain domain) {
        CertificateInfo certificate = firstCertificate(domain);
        DomainHttpsInfo.HttpGetBody https = DomainHttpsInfo.HttpGetBody.builder()
                .https_status(certificate != null ? "on" : "off")
                .certificate_name(certificateDisplayName(certificate))
                .certificate_value("")
                .expire_time(0L)
                .certificate_source(0)
                .certificate_type(certificate == null ? "" : normalize(certificate.getType()))
                .http2_status(config.getHTTP2() == null ? "off" : normalizeSwitch(config.getHTTP2().getSwitch()))
                .tls_version(config.getTLSConfig() == null || config.getTLSConfig().getVersion() == null ? "TLSv1.2,TLSv1.3" : String.join(",", config.getTLSConfig().getVersion()))
                .ocsp_stapling_status(config.getOCSPStapling() == null ? "off" : normalizeSwitch(config.getOCSPStapling().getSwitch()))
                .certId(0)
                .build();
        DomainHttpsInfo.ForceRedirect forceRedirect = DomainHttpsInfo.ForceRedirect.builder()
                .status("off")
                .type("https")
                .redirect_code("302")
                .redirectType("off")
                .redirectCode(302)
                .build();
        ForceRedirectHTTPSParameters redirect = config.getForceRedirectHTTPS();
        if (redirect != null && "on".equals(normalizeSwitch(redirect.getSwitch()))) {
            int code = redirect.getRedirectStatusCode() == null ? 302 : redirect.getRedirectStatusCode().intValue();
            forceRedirect.setStatus("on");
            forceRedirect.setType("https");
            forceRedirect.setRedirect_code(String.valueOf(code));
            forceRedirect.setRedirectType("https");
            forceRedirect.setRedirectCode(code);
        }
        return DomainHttpsInfo.builder().https(https).force_redirect(forceRedirect).build();
    }

    private String certificateDisplayName(CertificateInfo certificate) {
        if (certificate == null) {
            return "";
        }
        String alias = normalize(certificate.getAlias());
        if (Assert.notEmpty(alias)) {
            return alias;
        }
        String certId = normalize(certificate.getCertId());
        return Assert.notEmpty(certId) ? certId : "";
    }

    private boolean isHttpsEnabled(AccelerationDomain domain) {
        return firstCertificate(domain) != null;
    }

    private CertificateInfo firstCertificate(AccelerationDomain domain) {
        if (domain == null || domain.getCertificate() == null || domain.getCertificate().getList() == null) {
            return null;
        }
        for (CertificateInfo cert : domain.getCertificate().getList()) {
            if (cert != null && Assert.notEmpty(cert.getCertId())) {
                return cert;
            }
        }
        return null;
    }

    private DomainCacheInfo buildCacheInfo(ZoneConfig config) {
        List<DomainCacheInfo.CacheRule> cacheRules = new ArrayList<>();
        CacheConfigParameters cache = config.getCache();
        DomainCacheInfo.CacheRule rule = DomainCacheInfo.CacheRule.builder()
                .match_type("all")
                .match_value("")
                .ttl(0)
                .ttl_unit("s")
                .priority(1)
                .follow_origin("off")
                .url_parameter_type("")
                .url_parameter_value("")
                .build();
        if (cache != null) {
            if (cache.getFollowOrigin() != null && "on".equals(normalizeSwitch(cache.getFollowOrigin().getSwitch()))) {
                rule.setFollow_origin("on");
            } else if (cache.getCustomTime() != null && cache.getCustomTime().getCacheTime() != null) {
                int seconds = cache.getCustomTime().getCacheTime().intValue();
                rule.setTtl(KuocaiBaseUtil.getUnitCacheTime(seconds));
                rule.setTtl_unit(KuocaiBaseUtil.getCacheTimeUnit(seconds));
            }
        }
        cacheRules.add(rule);
        return DomainCacheInfo.builder()
                .cache_rules(cacheRules)
                .error_code_cache(new ArrayList<>())
                .build();
    }

    private void saveEdgeOneCacheRule(CdnDomain cdnDomain, List<CacheRuleDTO> rules) throws BusinessException {
        String zoneId = getZoneId(cdnDomain);
        RuleEngineItem existingRule = findEdgeOneCacheRule(cdnDomain.getDomainName());
        try {
            if (rules.isEmpty()) {
                if (existingRule != null && Assert.notEmpty(existingRule.getRuleId())) {
                    DeleteL7AccRulesRequest request = new DeleteL7AccRulesRequest();
                    request.setZoneId(zoneId);
                    request.setRuleIds(new String[]{existingRule.getRuleId()});
                    TencentEdgeOneClient.getClient().DeleteL7AccRules(request);
                    log.info("Delete EdgeOne cache rule success, domain={}, ruleId={}",
                            cdnDomain.getDomainName(), existingRule.getRuleId());
                }
                return;
            }

            RuleEngineItem rule = buildCacheRuleEngineItem(cdnDomain.getDomainName(), rules);
            if (existingRule != null && Assert.notEmpty(existingRule.getRuleId())) {
                rule.setRuleId(existingRule.getRuleId());
                if (existingRule.getRulePriority() != null) {
                    rule.setRulePriority(existingRule.getRulePriority());
                }
                ModifyL7AccRuleRequest request = new ModifyL7AccRuleRequest();
                request.setZoneId(zoneId);
                request.setRule(rule);
                log.info("Modify EdgeOne cache rule request, domain={}, request={}",
                        cdnDomain.getDomainName(), ModifyL7AccRuleRequest.toJsonString(request));
                TencentEdgeOneClient.getClient().ModifyL7AccRule(request);
                log.info("Modify EdgeOne cache rule success, domain={}, ruleId={}",
                        cdnDomain.getDomainName(), existingRule.getRuleId());
            } else {
                CreateL7AccRulesRequest request = new CreateL7AccRulesRequest();
                request.setZoneId(zoneId);
                request.setRules(new RuleEngineItem[]{rule});
                log.info("Create EdgeOne cache rule request, domain={}, request={}",
                        cdnDomain.getDomainName(), CreateL7AccRulesRequest.toJsonString(request));
                CreateL7AccRulesResponse response = TencentEdgeOneClient.getClient().CreateL7AccRules(request);
                log.info("Create EdgeOne cache rule success, domain={}, response={}",
                        cdnDomain.getDomainName(), CreateL7AccRulesResponse.toJsonString(response));
            }
        } catch (TencentCloudSDKException e) {
            throw new BusinessException("修改腾讯云 EdgeOne 缓存规则失败：" + TencentEdgeOneClient.formatTencentError(e));
        }
    }

    private RuleEngineItem buildCacheRuleEngineItem(String domainName, List<CacheRuleDTO> rules) throws BusinessException {
        List<com.tencentcloudapi.teo.v20220901.models.RuleEngineSubRule> subRules = new ArrayList<>();
        for (CacheRuleDTO rule : rules) {
            RuleBranch branch = new RuleBranch();
            branch.setCondition(buildCacheRuleCondition(rule));
            RuleEngineAction action = new RuleEngineAction();
            action.setName(EO_ACTION_CACHE);
            action.setCacheParameters(buildRuleCacheParameters(rule));
            branch.setActions(new RuleEngineAction[]{action});
            com.tencentcloudapi.teo.v20220901.models.RuleEngineSubRule subRule = new com.tencentcloudapi.teo.v20220901.models.RuleEngineSubRule();
            subRule.setBranches(new RuleBranch[]{branch});
            subRule.setDescription(new String[]{JSON.toJSONString(normalizeEdgeOneCacheRuleForDescription(rule))});
            subRules.add(subRule);
        }
        RuleBranch hostBranch = new RuleBranch();
        hostBranch.setSubRules(subRules.toArray(new com.tencentcloudapi.teo.v20220901.models.RuleEngineSubRule[0]));
        RuleEngineItem item = new RuleEngineItem();
        item.setStatus("enable");
        hostBranch.setCondition("${http.request.host} in ['" + escapeConditionValue(domainName) + "']");
        item.setRuleName(edgeOneCacheRuleName(domainName));
        item.setDescription(new String[]{"Kuocai cache rules for " + domainName});
        item.setBranches(new RuleBranch[]{hostBranch});
        return item;
    }

    private String buildCacheRuleCondition(CacheRuleDTO rule) throws BusinessException {
        String matchType = normalize(rule.getMatch_type());
        List<String> values = splitRuleValues(rule.getMatch_value());
        if ("file_extension".equals(matchType)) {
            List<String> extensions = normalizeEdgeOneFileExtensions(rule.getMatch_value());
            if (extensions.isEmpty()) {
                throw new BusinessException("文件后缀缓存规则内容不能为空");
            }
            return "${http.request.file_extension} in " + toConditionList(extensions);
        }
        if ("catalog".equals(matchType)) {
            if (values.isEmpty()) {
                throw new BusinessException("目录路径缓存规则内容不能为空");
            }
            List<String> expressions = new ArrayList<>();
            for (String value : values) {
                String path = normalize(value);
                if (!path.startsWith("/")) {
                    path = "/" + path;
                }
                path = trimTrailingSlash(path);
                if ("/".equals(path)) {
                    expressions.add("${http.request.uri.path} like ['/*']");
                } else {
                    expressions.add("(${http.request.uri.path} in ['" + escapeConditionValue(path) + "'] or ${http.request.uri.path} like ['" + escapeConditionValue(path + "/*") + "'])");
                }
            }
            return "(" + String.join(" or ", expressions) + ")";
        }
        if ("full_path".equals(matchType)) {
            if (values.isEmpty()) {
                throw new BusinessException("全路径缓存规则内容不能为空");
            }
            List<String> exactValues = new ArrayList<>();
            List<String> wildcardValues = new ArrayList<>();
            for (String value : values) {
                if (value.contains("*") || value.contains("?")) {
                    wildcardValues.add(value);
                } else {
                    exactValues.add(value);
                }
            }
            List<String> expressions = new ArrayList<>();
            if (!exactValues.isEmpty()) {
                expressions.add("${http.request.uri.path} in " + toConditionList(exactValues));
            }
            if (!wildcardValues.isEmpty()) {
                expressions.add("${http.request.uri.path} like " + toConditionList(wildcardValues));
            }
            return expressions.size() == 1 ? expressions.get(0) : "(" + String.join(" or ", expressions) + ")";
        }
        throw new BusinessException("腾讯云 EdgeOne 暂不支持该缓存规则类型：" + matchType);
    }

    private CacheParameters buildRuleCacheParameters(CacheRuleDTO rule) {
        CacheParameters cache = new CacheParameters();
        if ("on".equals(rule.getFollow_origin())) {
            cache.setFollowOrigin(buildFollowOrigin("on"));
            return cache;
        }
        if (rule.getTtl() == null) {
            cache.setNoCache(buildNoCache("on"));
        } else {
            cache.setCustomTime(buildCustomTime("on", KuocaiBaseUtil.toSeconds(rule.getTtl(), rule.getTtl_unit())));
        }
        return cache;
    }

    private void saveEdgeOneUrlAuthRule(CdnDomain cdnDomain, UrlAuthDTO urlAuth) throws BusinessException {
        String zoneId = getZoneId(cdnDomain);
        RuleEngineItem existingRule = findEdgeOneUrlAuthRule(cdnDomain.getDomainName());
        boolean enabled = urlAuth != null && "on".equals(normalizeSwitch(urlAuth.getStatus()));
        try {
            if (!enabled) {
                if (existingRule != null && Assert.notEmpty(existingRule.getRuleId())) {
                    DeleteL7AccRulesRequest request = new DeleteL7AccRulesRequest();
                    request.setZoneId(zoneId);
                    request.setRuleIds(new String[]{existingRule.getRuleId()});
                    TencentEdgeOneClient.getClient().DeleteL7AccRules(request);
                    log.info("Delete EdgeOne URL auth rule success, domain={}, ruleId={}",
                            cdnDomain.getDomainName(), existingRule.getRuleId());
                }
                return;
            }

            RuleEngineItem rule = buildUrlAuthRule(cdnDomain.getDomainName(), urlAuth);
            if (existingRule != null && Assert.notEmpty(existingRule.getRuleId())) {
                rule.setRuleId(existingRule.getRuleId());
                if (existingRule.getRulePriority() != null) {
                    rule.setRulePriority(existingRule.getRulePriority());
                }
                ModifyL7AccRuleRequest request = new ModifyL7AccRuleRequest();
                request.setZoneId(zoneId);
                request.setRule(rule);
                log.info("Modify EdgeOne URL auth rule request, domain={}, request={}",
                        cdnDomain.getDomainName(), ModifyL7AccRuleRequest.toJsonString(request));
                TencentEdgeOneClient.getClient().ModifyL7AccRule(request);
                log.info("Modify EdgeOne URL auth rule success, domain={}, ruleId={}",
                        cdnDomain.getDomainName(), existingRule.getRuleId());
            } else {
                CreateL7AccRulesRequest request = new CreateL7AccRulesRequest();
                request.setZoneId(zoneId);
                request.setRules(new RuleEngineItem[]{rule});
                log.info("Create EdgeOne URL auth rule request, domain={}, request={}",
                        cdnDomain.getDomainName(), CreateL7AccRulesRequest.toJsonString(request));
                CreateL7AccRulesResponse response = TencentEdgeOneClient.getClient().CreateL7AccRules(request);
                log.info("Create EdgeOne URL auth rule success, domain={}, response={}",
                        cdnDomain.getDomainName(), CreateL7AccRulesResponse.toJsonString(response));
            }
        } catch (TencentCloudSDKException e) {
            throw new BusinessException("修改腾讯云 EdgeOne URL 鉴权失败：" + TencentEdgeOneClient.formatTencentError(e));
        }
    }

    private RuleEngineItem buildUrlAuthRule(String domainName, UrlAuthDTO urlAuth) throws BusinessException {
        AuthenticationParameters parameters = buildAuthenticationParameters(urlAuth);
        RuleEngineAction action = new RuleEngineAction();
        action.setName(EO_ACTION_AUTHENTICATION);
        action.setAuthenticationParameters(parameters);

        RuleBranch branch = new RuleBranch();
        branch.setCondition(hostCondition(domainName));
        branch.setActions(new RuleEngineAction[]{action});

        RuleEngineItem rule = new RuleEngineItem();
        rule.setStatus("enable");
        rule.setRuleName(edgeOneUrlAuthRuleName(domainName));
        rule.setDescription(new String[]{JSON.toJSONString(normalizeUrlAuthForDescription(urlAuth))});
        rule.setBranches(new RuleBranch[]{branch});
        return rule;
    }

    private AuthenticationParameters buildAuthenticationParameters(UrlAuthDTO urlAuth) throws BusinessException {
        String type = toEdgeOneUrlAuthType(urlAuth == null ? null : urlAuth.getType());
        String primaryKey = normalize(urlAuth == null ? null : urlAuth.getPrimary_key());
        String secondaryKey = normalize(urlAuth == null ? null : urlAuth.getSecondary_key());
        long timeout = urlAuth == null || urlAuth.getExpire_time() == null ? 0L : urlAuth.getExpire_time();
        if (Assert.isEmpty(primaryKey)) {
            throw new BusinessException("URL 鉴权主密钥不能为空");
        }
        if (primaryKey.length() < 6 || primaryKey.length() > 40) {
            throw new BusinessException("腾讯云 EdgeOne URL 鉴权主密钥必须为 6-40 位");
        }
        if (Assert.notEmpty(secondaryKey) && (secondaryKey.length() < 6 || secondaryKey.length() > 40)) {
            throw new BusinessException("腾讯云 EdgeOne URL 鉴权备密钥必须为 6-40 位");
        }
        if (timeout < 1 || timeout > 630720000L) {
            throw new BusinessException("腾讯云 EdgeOne URL 鉴权有效期必须在 1-630720000 秒之间");
        }

        AuthenticationParameters parameters = new AuthenticationParameters();
        parameters.setAuthType(type);
        parameters.setSecretKey(primaryKey);
        parameters.setBackupSecretKey(Assert.isEmpty(secondaryKey) ? null : secondaryKey);
        parameters.setTimeout(timeout);
        if ("TypeA".equals(type)) {
            parameters.setAuthParam(EO_URL_AUTH_PARAM);
        }
        return parameters;
    }

    private UrlAuthDTO normalizeUrlAuthForDescription(UrlAuthDTO urlAuth) throws BusinessException {
        return UrlAuthDTO.builder()
                .status("on")
                .type(toLocalUrlAuthType(toEdgeOneUrlAuthType(urlAuth == null ? null : urlAuth.getType())))
                .primary_key(normalize(urlAuth == null ? null : urlAuth.getPrimary_key()))
                .secondary_key(normalize(urlAuth == null ? null : urlAuth.getSecondary_key()))
                .expire_time(urlAuth == null || urlAuth.getExpire_time() == null ? 0L : urlAuth.getExpire_time())
                .build();
    }

    private String toEdgeOneUrlAuthType(String type) {
        String normalized = normalize(type).toLowerCase();
        if ("typeb".equals(normalized)) {
            return "TypeB";
        }
        return "TypeA";
    }

    private String toLocalUrlAuthType(String type) {
        return "TypeB".equals(normalize(type)) ? "typeB" : "typeA";
    }

    private void saveEdgeOneOriginFollowRedirectRule(CdnDomain cdnDomain, DomainOriginSettingVo config) throws BusinessException {
        String zoneId = getZoneId(cdnDomain);
        RuleEngineItem existingRule = findEdgeOneOriginFollowRedirectRule(cdnDomain.getDomainName());
        boolean enabled = config != null && "on".equals(normalizeSwitch(config.getStatus()));
        try {
            if (!enabled) {
                if (existingRule != null && Assert.notEmpty(existingRule.getRuleId())) {
                    DeleteL7AccRulesRequest request = new DeleteL7AccRulesRequest();
                    request.setZoneId(zoneId);
                    request.setRuleIds(new String[]{existingRule.getRuleId()});
                    TencentEdgeOneClient.getClient().DeleteL7AccRules(request);
                    log.info("Delete EdgeOne origin follow redirect rule success, domain={}, ruleId={}",
                            cdnDomain.getDomainName(), existingRule.getRuleId());
                }
                return;
            }

            RuleEngineItem rule = buildOriginFollowRedirectRule(cdnDomain.getDomainName(), resolveOriginFollowRedirectMaxTimes(config));
            if (existingRule != null && Assert.notEmpty(existingRule.getRuleId())) {
                rule.setRuleId(existingRule.getRuleId());
                if (existingRule.getRulePriority() != null) {
                    rule.setRulePriority(existingRule.getRulePriority());
                }
                ModifyL7AccRuleRequest request = new ModifyL7AccRuleRequest();
                request.setZoneId(zoneId);
                request.setRule(rule);
                TencentEdgeOneClient.getClient().ModifyL7AccRule(request);
                log.info("Modify EdgeOne origin follow redirect rule success, domain={}, ruleId={}",
                        cdnDomain.getDomainName(), existingRule.getRuleId());
            } else {
                CreateL7AccRulesRequest request = new CreateL7AccRulesRequest();
                request.setZoneId(zoneId);
                request.setRules(new RuleEngineItem[]{rule});
                CreateL7AccRulesResponse response = TencentEdgeOneClient.getClient().CreateL7AccRules(request);
                log.info("Create EdgeOne origin follow redirect rule success, domain={}, response={}",
                        cdnDomain.getDomainName(), CreateL7AccRulesResponse.toJsonString(response));
            }
        } catch (TencentCloudSDKException e) {
            throw new BusinessException("修改腾讯云 EdgeOne 回源跟随重定向失败：" + TencentEdgeOneClient.formatTencentError(e));
        }
    }

    private RuleEngineItem buildOriginFollowRedirectRule(String domainName, long maxTimes) {
        UpstreamFollowRedirectParameters parameters = new UpstreamFollowRedirectParameters();
        parameters.setSwitch("on");
        parameters.setMaxTimes(maxTimes);

        RuleEngineAction action = new RuleEngineAction();
        action.setName(EO_ACTION_UPSTREAM_FOLLOW_REDIRECT);
        action.setUpstreamFollowRedirectParameters(parameters);

        RuleBranch branch = new RuleBranch();
        branch.setCondition("${http.request.host} in ['" + escapeConditionValue(domainName) + "']");
        branch.setActions(new RuleEngineAction[]{action});

        RuleEngineItem rule = new RuleEngineItem();
        rule.setStatus("enable");
        rule.setRuleName(edgeOneOriginFollowRedirectRuleName(domainName));
        rule.setDescription(new String[]{"Kuocai origin follow redirect for " + domainName});
        rule.setBranches(new RuleBranch[]{branch});
        return rule;
    }

    private long resolveOriginFollowRedirectMaxTimes(DomainOriginSettingVo config) {
        int maxTimes = config == null || config.getMaxTimes() == null ? 3 : config.getMaxTimes();
        if (maxTimes < 1) {
            return 1L;
        }
        if (maxTimes > 5) {
            return 5L;
        }
        return maxTimes;
    }

    private String getOriginFollowRedirectStatus(String domainName) {
        try {
            RuleEngineItem rule = findEdgeOneOriginFollowRedirectRule(domainName);
            UpstreamFollowRedirectParameters parameters = getOriginFollowRedirectParameters(rule);
            return parameters != null && "on".equals(normalizeSwitch(parameters.getSwitch())) ? "on" : "off";
        } catch (Exception e) {
            log.warn("Get EdgeOne origin follow redirect status failed for {}, fallback off: {}", domainName, e.getMessage());
            return "off";
        }
    }

    private Integer getOriginFollowRedirectMaxTimes(String domainName) {
        try {
            RuleEngineItem rule = findEdgeOneOriginFollowRedirectRule(domainName);
            UpstreamFollowRedirectParameters parameters = getOriginFollowRedirectParameters(rule);
            if (parameters != null && parameters.getMaxTimes() != null) {
                return parameters.getMaxTimes().intValue();
            }
        } catch (Exception e) {
            log.warn("Get EdgeOne origin follow redirect max times failed for {}, fallback 3: {}", domainName, e.getMessage());
        }
        return 3;
    }

    private UpstreamFollowRedirectParameters getOriginFollowRedirectParameters(RuleEngineItem rule) {
        if (rule == null || rule.getBranches() == null || !"enable".equalsIgnoreCase(normalize(rule.getStatus()))) {
            return null;
        }
        for (RuleBranch branch : rule.getBranches()) {
            if (branch == null || branch.getActions() == null) {
                continue;
            }
            for (RuleEngineAction action : branch.getActions()) {
                if (action != null
                        && EO_ACTION_UPSTREAM_FOLLOW_REDIRECT.equals(action.getName())
                        && action.getUpstreamFollowRedirectParameters() != null) {
                    return action.getUpstreamFollowRedirectParameters();
                }
            }
        }
        return null;
    }

    private List<DomainCacheInfo.CacheRule> queryEdgeOneCacheRules(String domainName) {
        try {
            RuleEngineItem rule = findEdgeOneCacheRule(domainName);
            if (rule == null || rule.getBranches() == null) {
                return new ArrayList<>();
            }
            List<DomainCacheInfo.CacheRule> rules = new ArrayList<>();
            for (RuleBranch branch : rule.getBranches()) {
                if (branch == null || branch.getSubRules() == null) {
                    DomainCacheInfo.CacheRule cacheRule = parseCacheRuleBranch(branch, rules.size() + 1);
                    if (cacheRule != null) {
                        rules.add(cacheRule);
                    }
                    continue;
                }
                for (com.tencentcloudapi.teo.v20220901.models.RuleEngineSubRule subRule : branch.getSubRules()) {
                    if (subRule == null || subRule.getBranches() == null) {
                        continue;
                    }
                    DomainCacheInfo.CacheRule describedRule = parseCacheRuleDescription(subRule.getDescription(), rules.size() + 1);
                    if (describedRule != null) {
                        rules.add(describedRule);
                        continue;
                    }
                    for (RuleBranch subBranch : subRule.getBranches()) {
                        DomainCacheInfo.CacheRule cacheRule = parseCacheRuleBranch(subBranch, rules.size() + 1);
                        if (cacheRule != null) {
                            rules.add(cacheRule);
                        }
                    }
                }
            }
            return rules;
        } catch (Exception e) {
            log.warn("Get EdgeOne cache rules failed for {}, use global cache only: {}", domainName, e.getMessage());
            return new ArrayList<>();
        }
    }

    private DomainCacheInfo.CacheRule parseCacheRuleDescription(String[] descriptions, int priority) {
        if (descriptions == null || descriptions.length == 0 || Assert.isEmpty(descriptions[0])) {
            return null;
        }
        try {
            CacheRuleDTO rule = JSON.parseObject(descriptions[0], CacheRuleDTO.class);
            if (rule == null || Assert.isEmpty(rule.getMatch_type())) {
                return null;
            }
            return DomainCacheInfo.CacheRule.builder()
                    .match_type(rule.getMatch_type())
                    .match_value(normalizeEdgeOneCacheRuleMatchValue(rule))
                    .ttl(rule.getTtl())
                    .ttl_unit(Assert.isEmpty(rule.getTtl_unit()) ? "s" : rule.getTtl_unit())
                    .priority(rule.getPriority() == null ? priority : rule.getPriority())
                    .follow_origin("on".equals(rule.getFollow_origin()) ? "on" : "off")
                    .url_parameter_type(normalize(rule.getUrl_parameter_type()))
                    .url_parameter_value(normalize(rule.getUrl_parameter_value()))
                    .build();
        } catch (Exception e) {
            return null;
        }
    }

    private DomainCacheInfo.CacheRule parseCacheRuleBranch(RuleBranch branch, int priority) {
        if (branch == null || branch.getActions() == null) {
            return null;
        }
        CacheParameters cache = null;
        for (RuleEngineAction action : branch.getActions()) {
            if (action != null && EO_ACTION_CACHE.equals(action.getName()) && action.getCacheParameters() != null) {
                cache = action.getCacheParameters();
                break;
            }
        }
        if (cache == null) {
            return null;
        }
        String condition = normalize(branch.getCondition());
        String matchType = inferCacheRuleMatchType(condition);
        List<String> values = parseCacheRuleValues(matchType, condition);
        int seconds = getCacheSeconds(cache);
        return DomainCacheInfo.CacheRule.builder()
                .match_type(matchType)
                .match_value(String.join(",", values))
                .ttl(isNoCache(cache) ? null : KuocaiBaseUtil.getUnitCacheTime(seconds))
                .ttl_unit(isNoCache(cache) ? "s" : KuocaiBaseUtil.getCacheTimeUnit(seconds))
                .priority(priority)
                .follow_origin(isFollowOrigin(cache) ? "on" : "off")
                .url_parameter_type("")
                .url_parameter_value("")
                .build();
    }

    private String inferCacheRuleMatchType(String condition) {
        if (condition.contains("file_extension")) {
            return "file_extension";
        }
        if (condition.contains(" like ")) {
            List<String> values = parseConditionValues(condition);
            boolean allCatalog = !values.isEmpty() && values.stream().allMatch(value -> value.endsWith("*") && !value.substring(0, value.length() - 1).contains("*"));
            return allCatalog ? "catalog" : "full_path";
        }
        return "full_path";
    }

    private List<String> parseCacheRuleValues(String matchType, String condition) {
        List<String> values = parseConditionValues(condition);
        List<String> result = new ArrayList<>();
        for (String value : values) {
            String normalized = normalize(value);
            if (Assert.isEmpty(normalized)) {
                continue;
            }
            if ("file_extension".equals(matchType)) {
                String extension = normalized.replaceFirst("^\\.+", "");
                if (Assert.notEmpty(extension)) {
                    result.add(extension);
                }
            } else if ("catalog".equals(matchType)) {
                result.add(normalized.endsWith("*") ? normalized.substring(0, normalized.length() - 1) : normalized);
            } else {
                result.add(normalized);
            }
        }
        return result;
    }

    private RuleEngineItem findEdgeOneCacheRule(String domainName) throws BusinessException {
        return findEdgeOneL7Rule(domainName, edgeOneCacheRuleName(domainName));
    }

    private RuleEngineItem findEdgeOneOriginFollowRedirectRule(String domainName) throws BusinessException {
        return findEdgeOneL7Rule(domainName, edgeOneOriginFollowRedirectRuleName(domainName));
    }

    private RuleEngineItem findEdgeOneUrlAuthRule(String domainName) throws BusinessException {
        return findEdgeOneL7Rule(domainName, edgeOneUrlAuthRuleName(domainName));
    }

    private String edgeOneCacheRuleName(String domainName) {
        String normalized = normalize(domainName).replaceAll("[^A-Za-z0-9_-]", "_");
        return EO_RULE_CACHE_PREFIX + normalized;
    }

    private String edgeOneOriginFollowRedirectRuleName(String domainName) {
        String normalized = normalize(domainName).replaceAll("[^A-Za-z0-9_-]", "_");
        return EO_RULE_ORIGIN_FOLLOW_REDIRECT_PREFIX + normalized;
    }

    private String edgeOneUrlAuthRuleName(String domainName) {
        String normalized = normalize(domainName).replaceAll("[^A-Za-z0-9_-]", "_");
        return EO_RULE_URL_AUTH_PREFIX + normalized;
    }

    private List<CacheRuleDTO> nonGlobalCacheRules(List<CacheRuleDTO> rules) {
        if (rules == null || rules.isEmpty()) {
            return Collections.emptyList();
        }
        List<CacheRuleDTO> result = new ArrayList<>();
        for (CacheRuleDTO rule : rules) {
            if (rule == null || "all".equals(rule.getMatch_type()) || "global".equals(rule.getMatch_type())) {
                continue;
            }
            result.add(rule);
        }
        return result;
    }

    private List<String> splitRuleValues(String value) {
        if (Assert.isEmpty(value)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String item : value.split("[,;，；]")) {
            String normalized = normalize(item);
            if (Assert.notEmpty(normalized)) {
                result.add(normalized);
            }
        }
        return result;
    }

    private CacheRuleDTO normalizeEdgeOneCacheRuleForDescription(CacheRuleDTO rule) {
        if (rule == null || !"file_extension".equals(normalize(rule.getMatch_type()))) {
            return rule;
        }
        return CacheRuleDTO.builder()
                .match_type(rule.getMatch_type())
                .match_value(normalizeEdgeOneCacheRuleMatchValue(rule))
                .ttl(rule.getTtl())
                .ttl_unit(rule.getTtl_unit())
                .priority(rule.getPriority())
                .url_parameter_type(rule.getUrl_parameter_type())
                .url_parameter_value(rule.getUrl_parameter_value())
                .follow_origin(rule.getFollow_origin())
                .build();
    }

    private String normalizeEdgeOneCacheRuleMatchValue(CacheRuleDTO rule) {
        if (rule == null) {
            return "";
        }
        if (!"file_extension".equals(normalize(rule.getMatch_type()))) {
            return normalize(rule.getMatch_value());
        }
        return String.join(",", normalizeEdgeOneFileExtensions(rule.getMatch_value()));
    }

    private List<String> normalizeEdgeOneFileExtensions(String value) {
        List<String> result = new ArrayList<>();
        for (String item : splitRuleValues(value)) {
            String extension = normalize(item).replaceFirst("^\\.+", "");
            if (Assert.notEmpty(extension)) {
                result.add(extension);
            }
        }
        return result;
    }

    private String regexQuotePath(String path) {
        return Pattern.quote(path);
    }

    private String wildcardToRegex(String value) {
        StringBuilder regex = new StringBuilder("^");
        for (char c : normalize(value).toCharArray()) {
            if (c == '*') {
                regex.append(".*");
            } else if (c == '?') {
                regex.append('.');
            } else {
                regex.append(Pattern.quote(String.valueOf(c)));
            }
        }
        regex.append('$');
        return regex.toString();
    }

    private boolean isFollowOrigin(CacheParameters cache) {
        return cache.getFollowOrigin() != null && "on".equals(normalizeSwitch(cache.getFollowOrigin().getSwitch()));
    }

    private boolean isNoCache(CacheParameters cache) {
        return cache.getNoCache() != null && "on".equals(normalizeSwitch(cache.getNoCache().getSwitch()));
    }

    private int getCacheSeconds(CacheParameters cache) {
        if (cache.getCustomTime() == null || cache.getCustomTime().getCacheTime() == null) {
            return 0;
        }
        return cache.getCustomTime().getCacheTime().intValue();
    }

    private DomainVisitInfo.UrlAuth queryEdgeOneUrlAuth(String domainName) {
        try {
            RuleEngineItem rule = findEdgeOneUrlAuthRule(domainName);
            if (rule == null || rule.getBranches() == null || !"enable".equalsIgnoreCase(normalize(rule.getStatus()))) {
                return DomainVisitInfo.UrlAuth.builder().status("off").type("").primary_key("").secondary_key("").expire_time(0L).build();
            }
            DomainVisitInfo.UrlAuth described = parseUrlAuthDescription(rule.getDescription());
            if (described != null) {
                return described;
            }
            for (RuleBranch branch : rule.getBranches()) {
                if (branch == null || branch.getActions() == null) {
                    continue;
                }
                for (RuleEngineAction action : branch.getActions()) {
                    if (action == null || !EO_ACTION_AUTHENTICATION.equals(action.getName()) || action.getAuthenticationParameters() == null) {
                        continue;
                    }
                    AuthenticationParameters parameters = action.getAuthenticationParameters();
                    return DomainVisitInfo.UrlAuth.builder()
                            .status("on")
                            .type(toLocalUrlAuthType(parameters.getAuthType()))
                            .primary_key(normalize(parameters.getSecretKey()))
                            .secondary_key(normalize(parameters.getBackupSecretKey()))
                            .expire_time(parameters.getTimeout() == null ? 0L : parameters.getTimeout())
                            .build();
                }
            }
        } catch (Exception e) {
            log.warn("Get EdgeOne URL auth rule failed for {}, fallback off: {}", domainName, e.getMessage());
        }
        return DomainVisitInfo.UrlAuth.builder().status("off").type("").primary_key("").secondary_key("").expire_time(0L).build();
    }

    private DomainVisitInfo.UrlAuth parseUrlAuthDescription(String[] descriptions) {
        if (descriptions == null || descriptions.length == 0 || Assert.isEmpty(descriptions[0])) {
            return null;
        }
        try {
            UrlAuthDTO dto = JSON.parseObject(descriptions[0], UrlAuthDTO.class);
            if (dto == null || !"on".equals(normalizeSwitch(dto.getStatus()))) {
                return null;
            }
            return DomainVisitInfo.UrlAuth.builder()
                    .status("on")
                    .type(toLocalUrlAuthType(toEdgeOneUrlAuthType(dto.getType())))
                    .primary_key(normalize(dto.getPrimary_key()))
                    .secondary_key(normalize(dto.getSecondary_key()))
                    .expire_time(dto.getExpire_time() == null ? 0L : dto.getExpire_time())
                    .build();
        } catch (Exception e) {
            return null;
        }
    }

    private DomainVisitInfo buildVisitInfo(String domainName) {
        DomainVisitInfo visitInfo = DomainVisitInfo.builder()
                .referer(DomainVisitInfo.Referer.builder().type("off").referer_type(0).value("").include_empty(false).build())
                .ip_filter(DomainVisitInfo.IpFilter.builder().type("off").value("").build())
                .user_agent_filter(DomainVisitInfo.UserAgentFilter.builder().type("off").value("").ua_list(Collections.emptyList()).build())
                .url_auth(DomainVisitInfo.UrlAuth.builder().status("off").type("").primary_key("").secondary_key("").expire_time(0L).build())
                .build();
        try {
            SecurityPolicy policy = describeSecurityPolicy(domainName);
            CustomRules customRules = policy == null ? null : policy.getCustomRules();
            if (customRules != null && customRules.getRules() != null) {
                for (CustomRule rule : customRules.getRules()) {
                    applyKuocaiRuleToVisitInfo(rule, visitInfo);
                }
            }
            visitInfo.setUrl_auth(queryEdgeOneUrlAuth(domainName));
            visitInfo.setEdgeone_security_policy(buildEdgeOneSecurityPolicyVo(policy, domainName));
        } catch (Exception e) {
            log.warn("Get EdgeOne security policy failed for {}, use empty visit config: {}", domainName, e.getMessage());
        }
        return visitInfo;
    }

    private DomainAdvancedInfo buildAdvancedInfo(ZoneConfig config) {
        DomainAdvancedInfo.Compress compress = DomainAdvancedInfo.Compress.builder()
                .status("off")
                .type("")
                .file_type("")
                .build();
        CompressionParameters compression = config.getCompression();
        if (compression != null && "on".equals(normalizeSwitch(compression.getSwitch()))) {
            compress.setStatus("on");
            compress.setType(String.join(",", emptyIfNull(compression.getAlgorithms())).replace("brotli", "br"));
        }
        return DomainAdvancedInfo.builder()
                .http_response_header(new ArrayList<>())
                .error_code_redirect_rules(new ArrayList<>())
                .error_pages(new ArrayList<>())
                .compress(compress)
                .build();
    }

    private void modifyOrigin(String domainName, String originType, String origin, String backupOrigin) throws BusinessException {
        try {
            ModifyAccelerationDomainRequest request = new ModifyAccelerationDomainRequest();
            request.setZoneId(TencentEdgeOneClient.resolveZoneId(domainName));
            request.setDomainName(domainName);
            request.setOriginInfo(buildOriginInfo(originType, origin, backupOrigin));
            ModifyAccelerationDomainResponse response = TencentEdgeOneClient.getClient().ModifyAccelerationDomain(request);
            log.info("Modify EdgeOne domain {} origin success: {}", domainName, ModifyAccelerationDomainResponse.toJsonString(response));
        } catch (BusinessException e) {
            throw e;
        } catch (TencentCloudSDKException e) {
            throw new BusinessException("修改腾讯云 EdgeOne 源站失败：" + TencentEdgeOneClient.formatTencentError(e));
        }
    }

    private void modifyOriginWithGroups(CdnDomain cdnDomain, CdnDomainSources main, CdnDomainSources back) throws BusinessException {
        try {
            String zoneId = getZoneId(cdnDomain);
            String mainGroupId = ensureOriginGroup(zoneId, originGroupName(cdnDomain.getDomainName(), "main"), main);
            String backGroupId = ensureOriginGroup(zoneId, originGroupName(cdnDomain.getDomainName(), "backup"), back);
            ModifyAccelerationDomainRequest request = new ModifyAccelerationDomainRequest();
            request.setZoneId(zoneId);
            request.setDomainName(cdnDomain.getDomainName());
            OriginInfo originInfo = new OriginInfo();
            originInfo.setOriginType("ORIGIN_GROUP");
            originInfo.setOrigin(mainGroupId);
            originInfo.setBackupOrigin(backGroupId);
            request.setOriginInfo(originInfo);
            ModifyAccelerationDomainResponse response = TencentEdgeOneClient.getClient().ModifyAccelerationDomain(request);
            log.info("Modify EdgeOne domain {} origin group success: {}", cdnDomain.getDomainName(), ModifyAccelerationDomainResponse.toJsonString(response));
        } catch (BusinessException e) {
            throw e;
        } catch (TencentCloudSDKException e) {
            throw new BusinessException("修改腾讯云 EdgeOne 备源站失败：" + TencentEdgeOneClient.formatTencentError(e));
        }
    }

    private String ensureOriginGroup(String zoneId, String groupName, CdnDomainSources source) throws BusinessException, TencentCloudSDKException {
        OriginRecord[] records = buildOriginRecords(source);
        String hostHeader = Assert.notEmpty(source.getHostName()) ? source.getHostName().trim() : null;
        OriginGroup existing = findOriginGroup(zoneId, groupName);
        if (existing != null && Assert.notEmpty(existing.getGroupId())) {
            ModifyOriginGroupRequest request = new ModifyOriginGroupRequest();
            request.setZoneId(zoneId);
            request.setGroupId(existing.getGroupId());
            request.setName(groupName);
            request.setType("GENERAL");
            request.setRecords(records);
            if (Assert.notEmpty(hostHeader)) {
                request.setHostHeader(hostHeader);
            }
            TencentEdgeOneClient.getClient().ModifyOriginGroup(request);
            return existing.getGroupId();
        }
        CreateOriginGroupRequest request = new CreateOriginGroupRequest();
        request.setZoneId(zoneId);
        request.setName(groupName);
        request.setType("GENERAL");
        request.setRecords(records);
        if (Assert.notEmpty(hostHeader)) {
            request.setHostHeader(hostHeader);
        }
        CreateOriginGroupResponse response = TencentEdgeOneClient.getClient().CreateOriginGroup(request);
        if (Assert.isEmpty(response.getOriginGroupId())) {
            throw new BusinessException("腾讯云 EdgeOne 创建源站组未返回 OriginGroupId");
        }
        return response.getOriginGroupId();
    }

    private OriginGroup findOriginGroup(String zoneId, String groupName) throws BusinessException, TencentCloudSDKException {
        long offset = 0L;
        while (true) {
            DescribeOriginGroupRequest request = new DescribeOriginGroupRequest();
            request.setZoneId(zoneId);
            request.setOffset(offset);
            request.setLimit(100L);
            DescribeOriginGroupResponse response = TencentEdgeOneClient.getClient().DescribeOriginGroup(request);
            OriginGroup[] groups = response.getOriginGroups();
            if (groups == null || groups.length == 0) {
                return null;
            }
            for (OriginGroup group : groups) {
                if (group != null && groupName.equals(group.getName())) {
                    return group;
                }
            }
            if (groups.length < 100) {
                return null;
            }
            offset += 100L;
        }
    }

    private OriginRecord[] buildOriginRecords(CdnDomainSources source) throws BusinessException {
        List<String> origins = splitOrigins(source == null ? null : source.getIpOrDomain());
        OriginRecord[] records = new OriginRecord[origins.size()];
        for (int i = 0; i < origins.size(); i++) {
            OriginRecord record = new OriginRecord();
            record.setRecord(origins.get(i));
            record.setType(convertOriginType(source.getOriginType()));
            record.setWeight(100L);
            records[i] = record;
        }
        return records;
    }

    private List<String> splitOrigins(String value) throws BusinessException {
        if (Assert.isEmpty(value)) {
            throw new BusinessException("源站不能为空");
        }
        List<String> origins = new ArrayList<>();
        for (String item : value.split(";")) {
            if (Assert.notEmpty(item)) {
                origins.add(item.trim());
            }
        }
        if (origins.isEmpty()) {
            throw new BusinessException("源站不能为空");
        }
        return origins;
    }

    private String originGroupName(String domainName, String suffix) {
        String base = normalize(domainName).replaceAll("[^A-Za-z0-9_-]", "_");
        String name = "kuocai_" + base + "_" + suffix;
        return name.length() > 200 ? name.substring(0, 200) : name;
    }

    private OriginInfo buildOriginInfo(String originType, String origin, String backupOrigin) throws BusinessException {
        OriginInfo originInfo = new OriginInfo();
        originInfo.setOriginType(convertOriginType(originType));
        originInfo.setOrigin(firstOrigin(origin));
        if (Assert.notEmpty(backupOrigin)) {
            originInfo.setBackupOrigin(firstOrigin(backupOrigin));
        }
        return originInfo;
    }

    private void applyOriginProtocolAndPorts(CreateAccelerationDomainRequest request, String originProtocol, Integer httpPort, Integer httpsPort) {
        request.setOriginProtocol(toEdgeOneOriginProtocol(originProtocol));
        if (httpPort != null) {
            request.setHttpOriginPort(httpPort.longValue());
        }
        if (httpsPort != null) {
            request.setHttpsOriginPort(httpsPort.longValue());
        }
    }

    private AccelerationDomain getAccelerationDomain(String domainName) throws BusinessException {
        try {
            return getAccelerationDomain(TencentEdgeOneClient.resolveZoneId(domainName), domainName);
        } catch (BusinessException e) {
            throw e;
        } catch (TencentCloudSDKException e) {
            throw new BusinessException("获取腾讯云 EdgeOne 域名信息失败：" + TencentEdgeOneClient.formatTencentError(e));
        }
    }

    private AccelerationDomain getAccelerationDomain(String zoneId, String domainName) throws TencentCloudSDKException, BusinessException {
        DescribeAccelerationDomainsRequest request = new DescribeAccelerationDomainsRequest();
        request.setZoneId(zoneId);
        request.setLimit(1L);
        request.setFilters(new AdvancedFilter[]{filter("domain-name", domainName)});
        DescribeAccelerationDomainsResponse response = TencentEdgeOneClient.getClient().DescribeAccelerationDomains(request);
        if (response.getAccelerationDomains() == null || response.getAccelerationDomains().length == 0) {
            return null;
        }
        return response.getAccelerationDomains()[0];
    }

    private AccelerationDomain tryGetAccelerationDomain(String zoneId, String domainName) {
        if (Assert.isEmpty(zoneId) || Assert.isEmpty(domainName)) {
            return null;
        }
        try {
            return getAccelerationDomain(zoneId, domainName);
        } catch (Exception e) {
            log.warn("Query EdgeOne domain {} for create recovery failed: {}", domainName, e.getMessage());
            return null;
        }
    }

    private AdvancedFilter filter(String name, String value) {
        AdvancedFilter filter = new AdvancedFilter();
        filter.setName(name);
        filter.setValues(new String[]{value});
        filter.setFuzzy(false);
        return filter;
    }

    private String getZoneId(CdnDomain cdnDomain) throws BusinessException {
        return getZoneId(cdnDomain, false);
    }

    private String getZoneId(CdnDomain cdnDomain, boolean allowMissing) throws BusinessException {
        if (cdnDomain == null) {
            throw new BusinessException("域名信息不存在");
        }
        if (Assert.isEmpty(cdnDomain.getDomainName())) {
            throw new BusinessException("EdgeOne domain name is empty");
        }
        String storedZoneId = cdnDomain.getDomainId();
        if (Assert.notEmpty(storedZoneId)) {
            Zone storedZone = TencentEdgeOneClient.findZoneById(storedZoneId);
            if (storedZone != null && isDomainInZone(cdnDomain.getDomainName(), storedZone.getZoneName())) {
                return storedZoneId;
            }
            log.warn("EdgeOne stored zoneId {} does not match domain {}, resolving from Tencent again", storedZoneId, cdnDomain.getDomainName());
            TencentEdgeOneClient.invalidateZoneIdCache(cdnDomain.getDomainName());
        }
        Zone matchedZone = TencentEdgeOneClient.findZone(cdnDomain.getDomainName());
        if (matchedZone != null && Assert.notEmpty(matchedZone.getZoneId())) {
            if (!matchedZone.getZoneId().equals(storedZoneId)) {
                cdnDomain.setDomainId(matchedZone.getZoneId());
                save(cdnDomain);
                log.info("Updated EdgeOne domain {} zoneId from {} to {}", cdnDomain.getDomainName(), storedZoneId, matchedZone.getZoneId());
            }
            return matchedZone.getZoneId();
        }
        TencentEdgeOneClient.invalidateZoneIdCache(cdnDomain.getDomainName());
        if (allowMissing) {
            return null;
        }
        throw new BusinessException("Tencent EdgeOne zone not found for this domain. It may have been deleted in Tencent Cloud.");
    }

    private boolean isDomainInZone(String domainName, String zoneName) {
        if (Assert.isEmpty(domainName) || Assert.isEmpty(zoneName)) {
            return false;
        }
        String normalizedDomain = TencentEdgeOneClient.normalizeDomain(domainName);
        String normalizedZone = TencentEdgeOneClient.normalizeDomain(zoneName);
        return normalizedDomain.equals(normalizedZone) || normalizedDomain.endsWith("." + normalizedZone);
    }

    private String firstOrigin(String value) throws BusinessException {
        if (Assert.isEmpty(value)) {
            throw new BusinessException("源站不能为空");
        }
        for (String item : value.split(";")) {
            if (Assert.notEmpty(item)) {
                return item.trim();
            }
        }
        throw new BusinessException("源站不能为空");
    }

    private String firstOriginOrEmpty(String value) {
        if (Assert.isEmpty(value)) {
            return "";
        }
        for (String item : value.split(";")) {
            if (Assert.notEmpty(item)) {
                return item.trim();
            }
        }
        return "";
    }

    private boolean isSameOriginConfig(OriginDetail current, String originType, String origin, String backupOrigin) {
        if (current == null) {
            return false;
        }
        return normalize(current.getOriginType()).equals(normalize(originType))
                && normalize(current.getOrigin()).equals(normalize(origin))
                && normalize(current.getBackupOrigin()).equals(normalize(backupOrigin));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimTrailingSlash(String value) {
        String normalized = normalize(value);
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private boolean isDomainBusy(String status) {
        if (Assert.isEmpty(status)) {
            return true;
        }
        String normalized = status.trim().toLowerCase();
        return !isDomainConfigurableStatus(normalized);
    }

    private boolean isDomainConfigurableStatus(String status) {
        String normalized = normalize(status).toLowerCase();
        return "online".equals(normalized)
                || "offline".equals(normalized)
                || "active".equals(normalized)
                || "activated".equals(normalized)
                || "enable".equals(normalized)
                || "enabled".equals(normalized)
                || "normal".equals(normalized)
                || "success".equals(normalized)
                || "finished".equals(normalized);
    }

    private boolean isDomainStatusDenied(TencentCloudSDKException e) {
        String errorCode = e.getErrorCode() == null ? "" : e.getErrorCode().toLowerCase();
        String message = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        return errorCode.contains("status")
                || message.contains("status")
                || message.contains("deploy")
                || message.contains("current domain status")
                || message.contains("当前域名状态")
                || message.contains("部署中");
    }

    private CacheRuleDTO firstGlobalCacheRule(List<CacheRuleDTO> rules) {
        if (rules == null || rules.isEmpty()) {
            return null;
        }
        return rules.stream()
                .filter(rule -> rule != null && ("all".equals(rule.getMatch_type()) || "global".equals(rule.getMatch_type())))
                .findFirst()
                .orElse(null);
    }

    private CacheConfigParameters buildGlobalCacheParameters(CacheRuleDTO globalRule) {
        CacheConfigParameters cache = new CacheConfigParameters();
        if ("on".equals(globalRule.getFollow_origin())) {
            cache.setFollowOrigin(buildFollowOrigin("on"));
            CacheConfigCustomTime customTime = new CacheConfigCustomTime();
            customTime.setSwitch("off");
            cache.setCustomTime(customTime);
            NoCache noCache = new NoCache();
            noCache.setSwitch("off");
            cache.setNoCache(noCache);
        } else if (globalRule.getTtl() == null) {
            cache.setFollowOrigin(buildFollowOrigin("off"));
            CacheConfigCustomTime customTime = new CacheConfigCustomTime();
            customTime.setSwitch("off");
            cache.setCustomTime(customTime);
            NoCache noCache = new NoCache();
            noCache.setSwitch("on");
            cache.setNoCache(noCache);
        } else {
            cache.setFollowOrigin(buildFollowOrigin("off"));
            CacheConfigCustomTime customTime = new CacheConfigCustomTime();
            customTime.setSwitch("on");
            customTime.setCacheTime(KuocaiBaseUtil.toSeconds(globalRule.getTtl(), globalRule.getTtl_unit()));
            cache.setCustomTime(customTime);
            NoCache noCache = new NoCache();
            noCache.setSwitch("off");
            cache.setNoCache(noCache);
        }
        return cache;
    }

    private boolean isSameGlobalCacheConfig(CacheConfigParameters currentCache, CacheRuleDTO requestedRule) {
        if (currentCache == null || requestedRule == null) {
            return false;
        }
        if ("on".equals(requestedRule.getFollow_origin())) {
            return currentCache.getFollowOrigin() != null
                    && "on".equals(normalizeSwitch(currentCache.getFollowOrigin().getSwitch()));
        }
        if (requestedRule.getTtl() == null) {
            return currentCache.getNoCache() != null
                    && "on".equals(normalizeSwitch(currentCache.getNoCache().getSwitch()));
        }
        return currentCache.getCustomTime() != null
                && "on".equals(normalizeSwitch(currentCache.getCustomTime().getSwitch()))
                && currentCache.getCustomTime().getCacheTime() != null
                && currentCache.getCustomTime().getCacheTime().equals(KuocaiBaseUtil.toSeconds(requestedRule.getTtl(), requestedRule.getTtl_unit()));
    }

    private String normalizeSwitch(String value) {
        return "on".equalsIgnoreCase(normalize(value)) ? "on" : "off";
    }

    private FollowOrigin buildFollowOrigin(String status) {
        FollowOrigin followOrigin = new FollowOrigin();
        String normalizedStatus = normalizeSwitch(status);
        followOrigin.setSwitch(normalizedStatus);
        followOrigin.setDefaultCache(normalizedStatus);
        followOrigin.setDefaultCacheStrategy(normalizedStatus);
        followOrigin.setDefaultCacheTime(0L);
        return followOrigin;
    }

    private NoCache buildNoCache(String status) {
        NoCache noCache = new NoCache();
        noCache.setSwitch(normalizeSwitch(status));
        return noCache;
    }

    private CustomTime buildCustomTime(String status, Long cacheTime) {
        CustomTime customTime = new CustomTime();
        String normalizedStatus = normalizeSwitch(status);
        customTime.setSwitch(normalizedStatus);
        customTime.setIgnoreCacheControl("on".equals(normalizedStatus) ? "on" : "off");
        if (cacheTime != null) {
            customTime.setCacheTime(cacheTime);
        }
        return customTime;
    }

    private String[] emptyIfNull(String[] values) {
        return values == null ? new String[0] : values;
    }

    private String toEdgeOneOriginProtocol(String protocol) {
        String normalized = normalize(protocol).toLowerCase();
        if ("https".equals(normalized)) {
            return "HTTPS";
        }
        if ("follow".equals(normalized)) {
            return "FOLLOW";
        }
        return "HTTP";
    }

    private String toSystemOriginProtocol(String protocol) {
        String normalized = normalize(protocol).toLowerCase();
        if ("https".equals(normalized)) {
            return "https";
        }
        if ("follow".equals(normalized)) {
            return "follow";
        }
        return "http";
    }

    private String convertOriginType(String originType) {
        return "domain".equals(originType) ? "IP_DOMAIN" : "IP_DOMAIN";
    }

    private String toSystemOriginType(String originType) {
        return "IP_DOMAIN".equals(originType) ? "domain" : "domain";
    }

    private String inferSystemOriginType(String originType, String origin) {
        String first = normalize(firstOriginOrEmpty(origin));
        if (Assert.notEmpty(first) && isIpOrigin(first)) {
            return "ipaddr";
        }
        if ("IP".equalsIgnoreCase(originType) || "IP_DOMAIN".equalsIgnoreCase(originType) && Assert.notEmpty(first) && isIpOrigin(first)) {
            return "ipaddr";
        }
        return "domain";
    }

    private boolean isIpOrigin(String origin) {
        String value = normalize(origin);
        if (Assert.isEmpty(value)) {
            return false;
        }
        if (value.startsWith("[") && value.contains("]")) {
            value = value.substring(1, value.indexOf("]"));
        } else if (value.indexOf(':') > 0 && value.indexOf(':') == value.lastIndexOf(':') && !value.contains("://")) {
            value = value.substring(0, value.indexOf(':'));
        }
        return IPV4_PATTERN.matcher(value).matches() || value.contains(":");
    }

    private String convertDomainStatus(String status) {
        if (isDomainConfigurableStatus(status) && !"offline".equals(normalize(status).toLowerCase())) {
            return "online";
        }
        if ("offline".equals(normalize(status).toLowerCase())) {
            return "offline";
        }
        return "configuring";
    }

    private boolean isUnauthorized(TencentCloudSDKException e) {
        String errorCode = e.getErrorCode() == null ? "" : e.getErrorCode();
        String message = e.getMessage() == null ? "" : e.getMessage();
        return errorCode.toLowerCase().contains("unauthorized")
                || message.contains("操作无权限")
                || message.toLowerCase().contains("not authorized");
    }

    private boolean isResourceNotFound(TencentCloudSDKException e) {
        String errorCode = e.getErrorCode() == null ? "" : e.getErrorCode().toLowerCase();
        String message = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        return errorCode.contains("notfound")
                || errorCode.contains("not_found")
                || message.contains("查询不到资源")
                || message.contains("not found")
                || message.contains("resource not found");
    }
}
