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
import com.tencentcloudapi.teo.v20220901.models.CAPTCHAPageChallenge;
import com.tencentcloudapi.teo.v20220901.models.ChallengeActionParameters;
import com.tencentcloudapi.teo.v20220901.models.ClientFiltering;
import com.tencentcloudapi.teo.v20220901.models.CompressionParameters;
import com.tencentcloudapi.teo.v20220901.models.DescribeL7AccSettingRequest;
import com.tencentcloudapi.teo.v20220901.models.DescribeL7AccSettingResponse;
import com.tencentcloudapi.teo.v20220901.models.ExceptionRule;
import com.tencentcloudapi.teo.v20220901.models.ExceptionRules;
import com.tencentcloudapi.teo.v20220901.models.FollowOrigin;
import com.tencentcloudapi.teo.v20220901.models.ForceRedirectHTTPSParameters;
import com.tencentcloudapi.teo.v20220901.models.HTTP2Parameters;
import com.tencentcloudapi.teo.v20220901.models.HttpDDoSProtection;
import com.tencentcloudapi.teo.v20220901.models.ManagedRuleAutoUpdate;
import com.tencentcloudapi.teo.v20220901.models.ManagedRules;
import com.tencentcloudapi.teo.v20220901.models.NoCache;
import com.tencentcloudapi.teo.v20220901.models.OCSPStaplingParameters;
import com.tencentcloudapi.teo.v20220901.models.RateLimitingRule;
import com.tencentcloudapi.teo.v20220901.models.RateLimitingRules;
import com.tencentcloudapi.teo.v20220901.models.SlowAttackDefense;
import com.tencentcloudapi.teo.v20220901.models.TLSConfigParameters;
import com.tencentcloudapi.teo.v20220901.models.Zone;
import com.tencentcloudapi.ssl.v20191205.models.UploadCertificateRequest;
import com.tencentcloudapi.ssl.v20191205.models.UploadCertificateResponse;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
    private static final String EO_ACTION_CACHE = "Cache";
    private static final String EO_ACTION_MODIFY_RESPONSE_HEADER = "ModifyResponseHeader";
    private static final String EO_ACTION_UPSTREAM_FOLLOW_REDIRECT = "UpstreamFollowRedirect";
    private static final Pattern CONDITION_VALUE_PATTERN = Pattern.compile("'((?:\\\\'|[^'])*)'");

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
        try {
            String zoneId = resolveZoneIdForCreate(userId, domainName, serviceArea);
            TencentEdgeOneClient.ensureZoneBoundToConfiguredPlan(zoneId);
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
                response = createAccelerationDomain(domainName, originType, ipOrDomain, zoneId, originProtocol, httpPort, httpsPort);
            }
            log.info("Create EdgeOne domain {} success: {}", domainName, CreateAccelerationDomainResponse.toJsonString(response));
            AccelerationDomain domain = getAccelerationDomain(domainName);
            CdnDomain cdnDomain = CdnDomain.builder()
                    .userId(userId)
                    .domainName(domainName)
                    .businessType(businessType)
                    .serviceArea(serviceArea)
                    .domainId(zoneId)
                    .cnameTencent(domain == null ? null : domain.getCname())
                    .domainStatus(convertDomainStatus(domain == null ? "processing" : domain.getDomainStatus()))
                    .route(CdnRoute.TENCENT_EDGEONE.getCode())
                    .build();
            return save(cdnDomain);
        } catch (BusinessException e) {
            throw e;
        } catch (TencentCloudSDKException e) {
            log.error("Create EdgeOne domain {} failed: {} - {}", domainName, e.getErrorCode(), e.getMessage());
            if (isUnauthorized(e)) {
                throw new BusinessException("创建腾讯云 EdgeOne 域名失败：根域名已完成归属权验证并已写入 eo-user 授权标签，但当前 EdgeOne Secret 仍缺少 teo:CreateAccelerationDomain 创建加速域名权限，或腾讯云 CAM 策略的资源范围/条件未匹配。请在腾讯云 CAM 中给该密钥放行 EdgeOne 创建加速域名权限后重试。错误代码：" + e.getErrorCode() + "，" + TencentEdgeOneClient.formatTencentError(e));
            }
            throw new BusinessException("创建腾讯云 EdgeOne 域名失败：" + TencentEdgeOneClient.formatTencentError(e));
        } catch (Exception e) {
            log.error("Create EdgeOne domain {} failed", domainName, e);
            throw new BusinessException("创建腾讯云 EdgeOne 域名失败：" + e.getMessage());
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
        ensureDomainReady(cdnDomain);
        CacheRuleDTO globalRule = firstGlobalCacheRule(config.getCacheRules());
        if (globalRule == null) {
            throw new BusinessException("腾讯云 EdgeOne 当前先支持全站缓存规则，目录/后缀/完整路径规则后续单独对接");
        }
        CacheConfigParameters cache = new CacheConfigParameters();
        if ("on".equals(globalRule.getFollow_origin())) {
            cache.setFollowOrigin(buildFollowOrigin("on"));
            CacheConfigCustomTime customTime = new CacheConfigCustomTime();
            customTime.setSwitch("off");
            cache.setCustomTime(customTime);
            NoCache noCache = new NoCache();
            noCache.setSwitch("off");
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
        ZoneConfig zoneConfig = new ZoneConfig();
        zoneConfig.setCache(cache);
        modifyL7AccSetting(cdnDomain, zoneConfig);
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
        throw new BusinessException("腾讯云 EdgeOne 防盗链需要使用安全/规则策略，当前版本暂未对接");
    }

    @Override
    public void saveIpBlackWhiteList(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        throw new BusinessException("腾讯云 EdgeOne IP 黑白名单需要使用安全策略，当前版本暂未对接");
    }

    @Override
    public void saveUserAgentFilter(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        throw new BusinessException("腾讯云 EdgeOne User-Agent 黑白名单需要使用规则策略，当前版本暂未对接");
    }

    @Override
    public void saveUrlAuth(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        throw new BusinessException("腾讯云 EdgeOne URL 鉴权需要使用规则策略，当前版本暂未对接");
    }

    @Override
    public EdgeOneSecurityPolicyVo getEdgeOneSecurityPolicy(CdnDomain cdnDomain) throws BusinessException {
        return buildEdgeOneSecurityPolicyVo(describeSecurityPolicy(cdnDomain.getDomainName()), cdnDomain.getDomainName());
    }

    @Override
    public void saveEdgeOneSecurityPolicy(CdnDomain cdnDomain, EdgeOneSecurityPolicyVo config) throws BusinessException {
        ensureDomainReady(cdnDomain);
        try {
            SecurityPolicy currentPolicy = describeSecurityPolicy(cdnDomain.getDomainName());
            if (currentPolicy == null) {
                currentPolicy = new SecurityPolicy();
            }

            SecurityPolicy updatePolicy = new SecurityPolicy();
            updatePolicy.setManagedRules(buildManagedRules(currentPolicy.getManagedRules(), config));
            updatePolicy.setBotManagement(buildBotManagement(currentPolicy.getBotManagement(), config));
            updatePolicy.setBotManagementLite(buildBotManagementLite(currentPolicy.getBotManagementLite(), config));
            updatePolicy.setHttpDDoSProtection(buildHttpDDoSProtection(currentPolicy.getHttpDDoSProtection(), config));
            updatePolicy.setRateLimitingRules(buildRateLimitingRules(currentPolicy.getRateLimitingRules(), config, cdnDomain.getDomainName()));
            updatePolicy.setExceptionRules(buildExceptionRules(currentPolicy.getExceptionRules(), config));

            ModifySecurityPolicyRequest request = new ModifySecurityPolicyRequest();
            request.setZoneId(getZoneId(cdnDomain));
            request.setEntity("Host");
            request.setHost(cdnDomain.getDomainName());
            request.setSecurityPolicy(updatePolicy);
            ModifySecurityPolicyResponse response = TencentEdgeOneClient.getClient().ModifySecurityPolicy(request);
            log.info("Modify EdgeOne security policy success, domain={}, response={}",
                    cdnDomain.getDomainName(), ModifySecurityPolicyResponse.toJsonString(response));
        } catch (BusinessException e) {
            throw e;
        } catch (TencentCloudSDKException e) {
            throw new BusinessException("修改腾讯云 EdgeOne 安全防护策略失败：" + TencentEdgeOneClient.formatTencentError(e));
        }
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
                .isIpv6("0")
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
        ZoneConfig edgeOneConfig = getL7AccSetting(domainName);
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

    private void saveKuocaiSecurityRule(CdnDomain cdnDomain, String ruleName, CustomRule replacement) throws BusinessException {
        try {
            SecurityPolicy policy = describeSecurityPolicy(cdnDomain.getDomainName());
            if (policy == null) {
                policy = new SecurityPolicy();
            }
            CustomRules customRules = policy.getCustomRules();
            if (customRules == null) {
                customRules = new CustomRules();
            }
            List<CustomRule> rules = new ArrayList<>();
            if (customRules.getRules() != null) {
                for (CustomRule rule : customRules.getRules()) {
                    if (rule == null || ruleName.equals(rule.getName())) {
                        continue;
                    }
                    rules.add(rule);
                }
            }
            if (replacement != null) {
                rules.add(replacement);
            }
            customRules.setRules(rules.toArray(new CustomRule[0]));
            SecurityPolicy updatePolicy = new SecurityPolicy();
            updatePolicy.setCustomRules(customRules);

            ModifySecurityPolicyRequest request = new ModifySecurityPolicyRequest();
            request.setZoneId(getZoneId(cdnDomain));
            request.setEntity("Host");
            request.setHost(cdnDomain.getDomainName());
            request.setSecurityPolicy(updatePolicy);
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

    private ManagedRules buildManagedRules(ManagedRules existing, EdgeOneSecurityPolicyVo config) {
        ManagedRules managedRules = existing == null ? new ManagedRules() : existing;
        managedRules.setEnabled(normalizeSwitch(config == null ? null : config.getManagedRulesEnabled()));
        managedRules.setDetectionOnly(normalizeSwitch(config == null ? null : config.getManagedRulesDetectionOnly()));
        managedRules.setSemanticAnalysis(normalizeSwitch(config == null ? null : config.getManagedRulesSemanticAnalysis()));
        ManagedRuleAutoUpdate autoUpdate = managedRules.getAutoUpdate() == null ? new ManagedRuleAutoUpdate() : managedRules.getAutoUpdate();
        autoUpdate.setAutoUpdateToLatestVersion("off".equals(normalizeSwitch(config == null ? null : config.getManagedRulesAutoUpdate())) ? "off" : "on");
        managedRules.setAutoUpdate(autoUpdate);
        return managedRules;
    }

    private BotManagement buildBotManagement(BotManagement existing, EdgeOneSecurityPolicyVo config) {
        BotManagement botManagement = existing == null ? new BotManagement() : existing;
        botManagement.setEnabled(normalizeSwitch(config == null ? null : config.getBotManagementEnabled()));
        return botManagement;
    }

    private BotManagementLite buildBotManagementLite(BotManagementLite existing, EdgeOneSecurityPolicyVo config) {
        BotManagementLite botLite = existing == null ? new BotManagementLite() : existing;
        CAPTCHAPageChallenge captcha = botLite.getCAPTCHAPageChallenge() == null ? new CAPTCHAPageChallenge() : botLite.getCAPTCHAPageChallenge();
        captcha.setEnabled(normalizeSwitch(config == null ? null : config.getCaptchaPageChallengeEnabled()));
        botLite.setCAPTCHAPageChallenge(captcha);

        AICrawlerDetection aiCrawler = botLite.getAICrawlerDetection() == null ? new AICrawlerDetection() : botLite.getAICrawlerDetection();
        aiCrawler.setEnabled(normalizeSwitch(config == null ? null : config.getAiCrawlerDetectionEnabled()));
        aiCrawler.setAction(buildSecurityAction(config == null ? null : config.getAiCrawlerDetectionAction(), "ManagedChallenge"));
        botLite.setAICrawlerDetection(aiCrawler);
        return botLite;
    }

    private HttpDDoSProtection buildHttpDDoSProtection(HttpDDoSProtection existing, EdgeOneSecurityPolicyVo config) {
        HttpDDoSProtection protection = existing == null ? new HttpDDoSProtection() : existing;
        AdaptiveFrequencyControl adaptive = protection.getAdaptiveFrequencyControl() == null ? new AdaptiveFrequencyControl() : protection.getAdaptiveFrequencyControl();
        adaptive.setEnabled(normalizeSwitch(config == null ? null : config.getHttpDdosAdaptiveFrequencyControlEnabled()));
        adaptive.setSensitivity(normalizeDdosSensitivity(config == null ? null : config.getHttpDdosAdaptiveFrequencyControlSensitivity()));
        adaptive.setAction(buildSecurityAction("Deny", null));
        protection.setAdaptiveFrequencyControl(adaptive);

        ClientFiltering clientFiltering = protection.getClientFiltering() == null ? new ClientFiltering() : protection.getClientFiltering();
        clientFiltering.setEnabled(normalizeSwitch(config == null ? null : config.getHttpDdosClientFilteringEnabled()));
        clientFiltering.setAction(buildSecurityAction("Deny", null));
        protection.setClientFiltering(clientFiltering);

        BandwidthAbuseDefense bandwidth = protection.getBandwidthAbuseDefense() == null ? new BandwidthAbuseDefense() : protection.getBandwidthAbuseDefense();
        bandwidth.setEnabled(normalizeSwitch(config == null ? null : config.getHttpDdosBandwidthAbuseDefenseEnabled()));
        bandwidth.setAction(buildSecurityAction("Deny", null));
        protection.setBandwidthAbuseDefense(bandwidth);

        SlowAttackDefense slowAttack = protection.getSlowAttackDefense() == null ? new SlowAttackDefense() : protection.getSlowAttackDefense();
        slowAttack.setEnabled(normalizeSwitch(config == null ? null : config.getHttpDdosSlowAttackDefenseEnabled()));
        slowAttack.setAction(buildSecurityAction("Deny", null));
        protection.setSlowAttackDefense(slowAttack);
        return protection;
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
                rules.add(rule);
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
                rules.add(rule);
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
        rule.setRuleType("BasicAccessRule");
        rule.setPriority(priority);
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
                containValues.add(item);
            } else {
                exactValues.add(item);
            }
        }
        List<String> expressions = new ArrayList<>();
        if (!exactValues.isEmpty()) {
            expressions.add("${http.request.headers['" + headerName + "']} in " + toConditionList(exactValues));
        }
        if (!containValues.isEmpty()) {
            expressions.add("${http.request.headers['" + headerName + "']} contain " + toConditionList(containValues));
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
            DescribeAccelerationDomainsRequest request = new DescribeAccelerationDomainsRequest();
            request.setZoneId(TencentEdgeOneClient.resolveZoneId(domainName));
            request.setLimit(1L);
            request.setFilters(new AdvancedFilter[]{filter("domain-name", domainName)});
            DescribeAccelerationDomainsResponse response = TencentEdgeOneClient.getClient().DescribeAccelerationDomains(request);
            if (response.getAccelerationDomains() == null || response.getAccelerationDomains().length == 0) {
                return null;
            }
            return response.getAccelerationDomains()[0];
        } catch (BusinessException e) {
            throw e;
        } catch (TencentCloudSDKException e) {
            throw new BusinessException("获取腾讯云 EdgeOne 域名信息失败：" + TencentEdgeOneClient.formatTencentError(e));
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

    private boolean isDomainBusy(String status) {
        if (Assert.isEmpty(status)) {
            return true;
        }
        String normalized = status.trim().toLowerCase();
        return !"online".equals(normalized) && !"offline".equals(normalized);
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
        if ("online".equals(status)) {
            return "online";
        }
        if ("offline".equals(status)) {
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
