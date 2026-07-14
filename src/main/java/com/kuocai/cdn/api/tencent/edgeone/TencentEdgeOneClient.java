package com.kuocai.cdn.api.tencent.edgeone;

import com.kuocai.cdn.config.SystemConfig;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.JedisUtil;
import com.kuocai.cdn.vo.TencentEdgeOneApiConfigVo;
import com.google.common.net.InternetDomainName;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.sts.v20180813.StsClient;
import com.tencentcloudapi.sts.v20180813.models.GetCallerIdentityRequest;
import com.tencentcloudapi.sts.v20180813.models.GetCallerIdentityResponse;
import com.tencentcloudapi.ssl.v20191205.SslClient;
import com.tencentcloudapi.tag.v20180813.TagClient;
import com.tencentcloudapi.tag.v20180813.models.TagResourcesRequest;
import com.tencentcloudapi.teo.v20220901.TeoClient;
import com.tencentcloudapi.teo.v20220901.models.BindZoneToPlanRequest;
import com.tencentcloudapi.teo.v20220901.models.CreateZoneRequest;
import com.tencentcloudapi.teo.v20220901.models.CreateZoneResponse;
import com.tencentcloudapi.teo.v20220901.models.DescribeIdentificationsRequest;
import com.tencentcloudapi.teo.v20220901.models.DescribeIdentificationsResponse;
import com.tencentcloudapi.teo.v20220901.models.DescribeZonesRequest;
import com.tencentcloudapi.teo.v20220901.models.DescribeZonesResponse;
import com.tencentcloudapi.teo.v20220901.models.Filter;
import com.tencentcloudapi.teo.v20220901.models.Identification;
import com.tencentcloudapi.teo.v20220901.models.Resource;
import com.tencentcloudapi.teo.v20220901.models.Tag;
import com.tencentcloudapi.teo.v20220901.models.Zone;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class TencentEdgeOneClient {

    private static final String ENDPOINT = "teo.tencentcloudapi.com";
    private static final String SSL_ENDPOINT = "ssl.tencentcloudapi.com";
    private static final String TAG_ENDPOINT = "tag.tencentcloudapi.com";
    private static final String STS_ENDPOINT = "sts.tencentcloudapi.com";
    private static final long PAGE_SIZE = 100L;
    private static final String CNAME_ACCESS_TYPE = "partial";
    private static final String DEFAULT_PROJECT_TAG_VALUE = "ljfcdn";
    private static final String ZONE_ID_CACHE_PREFIX = "TencentEdgeOne:ZoneId:";
    private static final String ZONE_PROJECT_TAG_CACHE_PREFIX = "TencentEdgeOne:ZoneProjectTag:";
    private static final int ZONE_PROJECT_TAG_CACHE_SECONDS = 300;
    private static final int ZONE_PROJECT_TAG_MAX_RETRIES = 3;
    private static volatile String cachedAccountSecretId;
    private static volatile String cachedAccountId;

    public static TeoClient getClient() throws BusinessException {
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint(ENDPOINT);
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);
        return new TeoClient(getCredential(), "", clientProfile);
    }

    public static SslClient getSslClient() throws BusinessException {
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint(SSL_ENDPOINT);
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);
        return new SslClient(getCredential(), "", clientProfile);
    }

    private static Credential getCredential() throws BusinessException {
        TencentEdgeOneApiConfigVo config = getConfig();
        return new Credential(config.getSecretId(), config.getSecretKey());
    }

    private static TagClient getTagClient() throws BusinessException {
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint(TAG_ENDPOINT);
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);
        return new TagClient(getCredential(), "", clientProfile);
    }

    private static StsClient getStsClient() throws BusinessException {
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint(STS_ENDPOINT);
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);
        return new StsClient(getCredential(), "ap-guangzhou", clientProfile);
    }

    public static String resolveZoneId(String domainName) throws BusinessException {
        if (Assert.isEmpty(domainName)) {
            throw new BusinessException("域名不能为空");
        }
        String rootDomain = getRootDomain(domainName);
        String zoneIdCacheKey = getZoneIdCacheKey(rootDomain);
        String cachedZoneId = JedisUtil.getStr(zoneIdCacheKey);
        if (Assert.notEmpty(cachedZoneId)) {
            Zone cachedZone = findZoneById(cachedZoneId);
            if (cachedZone == null || !isDomainInZone(rootDomain, cachedZone.getZoneName())) {
                JedisUtil.delKey(zoneIdCacheKey);
            } else if (!isVerifiedZone(cachedZone) && !isRootDomainVerified(rootDomain)) {
                throw new BusinessException("腾讯云 EdgeOne 根域名站点 " + rootDomain + " 尚未完成归属权验证，请先验证通过后再提交创建加速域名");
            } else {
                ensureZoneProjectTag(cachedZoneId);
                return cachedZoneId;
            }
        }
        Zone zone = findZone(rootDomain);
        if (zone != null && Assert.notEmpty(zone.getZoneId())) {
            if (!isVerifiedZone(zone) && !isRootDomainVerified(rootDomain)) {
                throw new BusinessException("腾讯云 EdgeOne 根域名站点 " + rootDomain + " 尚未完成归属权验证，请先验证通过后再提交创建加速域名");
            }
            cacheZoneId(rootDomain, zone.getZoneId());
            ensureZoneProjectTag(zone.getZoneId());
            return zone.getZoneId();
        }
        createZone(rootDomain);
        throw new BusinessException("已自动创建腾讯云 EdgeOne 根域名站点 " + rootDomain + "，请先完成域名归属权验证，验证通过后重新提交创建加速域名");
    }

    public static String resolveZoneId(String domainName, String serviceArea) throws BusinessException {
        if (Assert.isEmpty(domainName)) {
            throw new BusinessException("域名不能为空");
        }
        String rootDomain = getRootDomain(domainName);
        Zone zone = findZone(rootDomain);
        if (zone != null && Assert.notEmpty(zone.getZoneId())) {
            if (!isVerifiedZone(zone) && !isRootDomainVerified(rootDomain)) {
                throw new BusinessException("腾讯云 EdgeOne 根域名站点 " + rootDomain + " 尚未完成归属权验证，请先验证通过后再提交创建加速域名");
            }
            cacheZoneId(rootDomain, zone.getZoneId());
            ensureZoneProjectTag(zone.getZoneId());
            return zone.getZoneId();
        }
        createZone(rootDomain, serviceArea);
        throw new BusinessException("已自动创建腾讯云 EdgeOne 根域名站点 " + rootDomain + "，请先完成域名归属权验证，验证通过后重新提交创建加速域名");
    }

    public static String findZoneId(String domainName) throws BusinessException {
        Zone zone = findZone(domainName);
        return zone == null ? null : zone.getZoneId();
    }

    public static Zone findZone(String domainName) throws BusinessException {
        try {
            Zone matched = null;
            long offset = 0L;
            TeoClient client = getClient();
            while (true) {
                DescribeZonesRequest request = new DescribeZonesRequest();
                request.setOffset(offset);
                request.setLimit(PAGE_SIZE);
                DescribeZonesResponse response = client.DescribeZones(request);
                Zone[] zones = response.getZones();
                if (zones == null || zones.length == 0) {
                    break;
                }
                for (Zone zone : zones) {
                    if (zone == null || Assert.isEmpty(zone.getZoneId()) || Assert.isEmpty(zone.getZoneName())) {
                        continue;
                    }
                    if (isDomainInZone(domainName, zone.getZoneName())
                            && (matched == null || zone.getZoneName().length() > matched.getZoneName().length())) {
                        matched = zone;
                    }
                }
                if (zones.length < PAGE_SIZE) {
                    break;
                }
                offset += PAGE_SIZE;
            }
            if (matched != null) {
                cacheZoneId(getRootDomain(domainName), matched.getZoneId());
                return matched;
            }
        } catch (TencentCloudSDKException e) {
            throw new BusinessException("查询腾讯云 EdgeOne 站点失败：" + formatTencentError(e));
        }
        return null;
    }

    public static Zone findZoneById(String zoneId) throws BusinessException {
        if (Assert.isEmpty(zoneId)) {
            return null;
        }
        try {
            long offset = 0L;
            TeoClient client = getClient();
            while (true) {
                DescribeZonesRequest request = new DescribeZonesRequest();
                request.setOffset(offset);
                request.setLimit(PAGE_SIZE);
                DescribeZonesResponse response = client.DescribeZones(request);
                Zone[] zones = response.getZones();
                if (zones == null || zones.length == 0) {
                    break;
                }
                for (Zone zone : zones) {
                    if (zone != null && zoneId.equals(zone.getZoneId())) {
                        return zone;
                    }
                }
                if (zones.length < PAGE_SIZE) {
                    break;
                }
                offset += PAGE_SIZE;
            }
        } catch (TencentCloudSDKException e) {
            throw new BusinessException("查询腾讯云 EdgeOne 站点失败：" + formatTencentError(e));
        }
        return null;
    }

    public static String createZone(String rootDomain) throws BusinessException {
        return createZone(rootDomain, null);
    }

    public static String createZone(String rootDomain, String serviceArea) throws BusinessException {
        if (Assert.isEmpty(rootDomain)) {
            throw new BusinessException("根域名不能为空");
        }
        try {
            CreateZoneRequest request = new CreateZoneRequest();
            request.setZoneName(rootDomain);
            request.setType(CNAME_ACCESS_TYPE);
            request.setPlanId(getPlanId());
            String edgeOneArea = convertServiceArea(serviceArea);
            if (Assert.notEmpty(edgeOneArea)) {
                request.setArea(edgeOneArea);
            }
            request.setJumpStart(false);
            request.setAllowDuplicates(false);
            Tag projectTag = buildProjectTag();
            if (projectTag != null) {
                request.setTags(new Tag[]{projectTag});
            }
            CreateZoneResponse response = getClient().CreateZone(request);
            if (Assert.isEmpty(response.getZoneId())) {
                throw new BusinessException("腾讯云 EdgeOne 创建根域名站点未返回 ZoneId");
            }
            cacheZoneId(rootDomain, response.getZoneId());
            ensureZoneProjectTag(response.getZoneId());
            return response.getZoneId();
        } catch (BusinessException e) {
            throw e;
        } catch (TencentCloudSDKException e) {
            throw new BusinessException("创建腾讯云 EdgeOne 根域名站点失败：" + formatTencentError(e));
        }
    }

    public static void ensureZoneBoundToConfiguredPlan(String zoneId) throws BusinessException {
        if (Assert.isEmpty(zoneId)) {
            throw new BusinessException("腾讯云 EdgeOne ZoneId 不能为空，无法绑定套餐");
        }
        String planId = getPlanId();
        Zone zone = findZoneById(zoneId);
        if (isZoneBoundToPlan(zone, planId) || isZoneBoundToAnyPlan(zone)) {
            return;
        }
        try {
            BindZoneToPlanRequest request = new BindZoneToPlanRequest();
            request.setZoneId(zoneId);
            request.setPlanId(planId);
            getClient().BindZoneToPlan(request);
        } catch (TencentCloudSDKException e) {
            if (isAlreadyBoundPlanError(e)) {
                return;
            }
            throw new BusinessException("腾讯云 EdgeOne 根域名站点绑定套餐失败，请确认后台配置的 PlanId 属于当前账号且套餐可用：" + formatTencentError(e));
        }
    }

    private static boolean isZoneBoundToPlan(Zone zone, String planId) {
        if (zone == null || zone.getResources() == null) {
            return false;
        }
        for (Resource resource : zone.getResources()) {
            if (resource != null && planId.equals(resource.getPlanId())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isZoneBoundToAnyPlan(Zone zone) {
        if (zone == null || zone.getResources() == null) {
            return false;
        }
        for (Resource resource : zone.getResources()) {
            if (resource != null && Assert.notEmpty(resource.getPlanId())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAlreadyBoundPlanError(TencentCloudSDKException e) {
        String message = e.getMessage();
        if (Assert.isEmpty(message)) {
            return false;
        }
        String lower = message.toLowerCase();
        return lower.contains("already") || lower.contains("bound") || lower.contains("exist") || message.contains("已绑定");
    }

    private static String convertServiceArea(String serviceArea) {
        if ("mainland_china".equals(serviceArea)) {
            return "mainland";
        }
        if ("global".equals(serviceArea)) {
            return "global";
        }
        if ("outside_mainland_china".equals(serviceArea)) {
            return "overseas";
        }
        return null;
    }

    public static String getRootDomain(String domainName) throws BusinessException {
        String normalizedDomain = normalizeDomain(domainName);
        if (Assert.isEmpty(normalizedDomain)) {
            throw new BusinessException("域名不能为空");
        }
        try {
            InternetDomainName internetDomainName = InternetDomainName.from(normalizedDomain);
            if (internetDomainName.isUnderPublicSuffix()) {
                return internetDomainName.topPrivateDomain().toString();
            }
        } catch (Exception ignored) {
        }
        String[] parts = normalizedDomain.split("\\.");
        if (parts.length < 2) {
            throw new BusinessException("域名格式不正确");
        }
        return parts[parts.length - 2] + "." + parts[parts.length - 1];
    }

    public static List<String> listZoneIds() throws BusinessException {
        List<String> zoneIds = new ArrayList<>();
        try {
            long offset = 0L;
            TeoClient client = getClient();
            while (true) {
                DescribeZonesRequest request = new DescribeZonesRequest();
                request.setOffset(offset);
                request.setLimit(PAGE_SIZE);
                DescribeZonesResponse response = client.DescribeZones(request);
                Zone[] zones = response.getZones();
                if (zones == null || zones.length == 0) {
                    break;
                }
                for (Zone zone : zones) {
                    if (zone != null && Assert.notEmpty(zone.getZoneId())) {
                        zoneIds.add(zone.getZoneId());
                    }
                }
                if (zones.length < PAGE_SIZE) {
                    break;
                }
                offset += PAGE_SIZE;
            }
        } catch (TencentCloudSDKException e) {
            throw new BusinessException("查询腾讯云 EdgeOne 站点失败：" + formatTencentError(e));
        }
        return zoneIds;
    }

    private static boolean isDomainInZone(String domainName, String zoneName) {
        if (Assert.isEmpty(domainName) || Assert.isEmpty(zoneName)) {
            return false;
        }
        String normalizedDomain = normalizeDomain(domainName);
        String normalizedZone = normalizeDomain(zoneName);
        return normalizedDomain.equals(normalizedZone) || normalizedDomain.endsWith("." + normalizedZone);
    }

    public static String normalizeDomain(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        if (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.startsWith("*.")) {
            normalized = normalized.substring(2);
        }
        return normalized;
    }

    public static boolean isVerifiedZone(Zone zone) {
        if (zone == null) {
            return false;
        }
        String status = (zone.getStatus() == null ? "" : zone.getStatus()).toLowerCase();
        String activeStatus = (zone.getActiveStatus() == null ? "" : zone.getActiveStatus()).toLowerCase();
        String cnameStatus = (zone.getCnameStatus() == null ? "" : zone.getCnameStatus()).toLowerCase();
        return "active".equals(status)
                || "active".equals(activeStatus)
                || "online".equals(status)
                || "online".equals(activeStatus)
                || "success".equals(status)
                || "success".equals(activeStatus)
                || "finished".equals(cnameStatus);
    }

    public static boolean isRootDomainVerified(String rootDomain) throws BusinessException {
        try {
            DescribeIdentificationsRequest request = new DescribeIdentificationsRequest();
            Filter filter = new Filter();
            filter.setName("zone-name");
            filter.setValues(new String[]{rootDomain});
            request.setFilters(new Filter[]{filter});
            request.setOffset(0L);
            request.setLimit(1L);
            DescribeIdentificationsResponse response = getClient().DescribeIdentifications(request);
            Identification[] identifications = response.getIdentifications();
            if (identifications == null || identifications.length == 0 || identifications[0] == null) {
                return false;
            }
            return "finished".equalsIgnoreCase(identifications[0].getStatus());
        } catch (TencentCloudSDKException e) {
            throw new BusinessException("查询腾讯云 EdgeOne 根域名验证状态失败：" + formatTencentError(e));
        }
    }

    private static void cacheZoneId(String rootDomain, String zoneId) throws BusinessException {
        if (Assert.notEmpty(rootDomain) && Assert.notEmpty(zoneId)) {
            JedisUtil.setStr(getZoneIdCacheKey(rootDomain), zoneId);
        }
    }

    public static void invalidateZoneIdCache(String domainName) throws BusinessException {
        if (Assert.notEmpty(domainName)) {
            JedisUtil.delKey(getZoneIdCacheKey(getRootDomain(domainName)));
        }
    }

    private static String getZoneIdCacheKey(String rootDomain) throws BusinessException {
        return ZONE_ID_CACHE_PREFIX + getConfig().getSecretId().trim() + ":" + rootDomain;
    }

    private static Tag buildProjectTag() throws BusinessException {
        String projectName = getProjectTagValue();
        if (Assert.isEmpty(projectName)) {
            return null;
        }
        Tag tag = new Tag();
        tag.setTagKey("eo-user");
        tag.setTagValue(projectName.trim());
        return tag;
    }

    public static void ensureZoneProjectTag(String zoneId) throws BusinessException {
        com.tencentcloudapi.tag.v20180813.models.Tag tag = buildTagApiProjectTag();
        if (Assert.isEmpty(zoneId) || tag == null) {
            return;
        }
        String cacheKey = buildZoneProjectTagCacheKey(zoneId, tag.getTagValue());
        if (JedisUtil.exists(cacheKey)) {
            return;
        }
        for (int attempt = 1; attempt <= ZONE_PROJECT_TAG_MAX_RETRIES; attempt++) {
            try {
                TagResourcesRequest request = new TagResourcesRequest();
                request.setResourceList(new String[]{buildZoneResourceName(zoneId)});
                request.setTags(new com.tencentcloudapi.tag.v20180813.models.Tag[]{tag});
                getTagClient().TagResources(request);
                JedisUtil.setStr(cacheKey, "1", ZONE_PROJECT_TAG_CACHE_SECONDS);
                return;
            } catch (TencentCloudSDKException e) {
                if (!isResourceTagConcurrentCommit(e)) {
                    throw new BusinessException("腾讯云 EdgeOne 根站点打标签失败：" + formatTencentError(e));
                }
                if (attempt >= ZONE_PROJECT_TAG_MAX_RETRIES) {
                    log.warn("Skip duplicated EdgeOne zone tag commit after retries, zoneId={}, error={}",
                            zoneId, formatTencentError(e));
                    return;
                }
                try {
                    Thread.sleep(250L * attempt);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    log.warn("EdgeOne zone tag retry interrupted, continue domain operation, zoneId={}", zoneId);
                    return;
                }
            }
        }
    }

    private static String buildZoneProjectTagCacheKey(String zoneId, String tagValue) throws BusinessException {
        TencentEdgeOneApiConfigVo config = getConfig();
        return ZONE_PROJECT_TAG_CACHE_PREFIX
                + config.getSecretId().trim() + ":"
                + zoneId + ":"
                + Integer.toHexString((tagValue == null ? "" : tagValue).hashCode());
    }

    private static boolean isResourceTagConcurrentCommit(TencentCloudSDKException e) {
        return e != null && (isResourceTagConcurrentCommit(e.getMessage())
                || isResourceTagConcurrentCommit(e.getErrorCode()));
    }

    private static boolean isResourceTagConcurrentCommit(String value) {
        if (Assert.isEmpty(value)) {
            return false;
        }
        String normalized = value.toLowerCase();
        return normalized.contains("repeat commit")
                && (normalized.contains("resourcetag") || normalized.contains("resource tag"));
    }

    private static String buildZoneResourceName(String zoneId) throws BusinessException {
        String accountId = getAccountId();
        if (Assert.isEmpty(accountId)) {
            throw new BusinessException("获取腾讯云 EdgeOne 主账号 UIN 失败，无法给根站点打授权标签");
        }
        return "qcs::teo::uin/" + accountId + ":zone/" + zoneId;
    }

    private static String getAccountId() throws BusinessException {
        TencentEdgeOneApiConfigVo config = getConfig();
        String secretId = config.getSecretId();
        if (Assert.notEmpty(cachedAccountId) && secretId != null && secretId.equals(cachedAccountSecretId)) {
            return cachedAccountId;
        }
        synchronized (TencentEdgeOneClient.class) {
            if (Assert.notEmpty(cachedAccountId) && secretId != null && secretId.equals(cachedAccountSecretId)) {
                return cachedAccountId;
            }
            try {
                GetCallerIdentityResponse response = getStsClient().GetCallerIdentity(new GetCallerIdentityRequest());
                cachedAccountSecretId = secretId;
                cachedAccountId = response.getAccountId();
                return cachedAccountId;
            } catch (TencentCloudSDKException e) {
                throw new BusinessException("获取腾讯云 EdgeOne 主账号 UIN 失败：" + formatTencentError(e));
            }
        }
    }

    private static com.tencentcloudapi.tag.v20180813.models.Tag buildTagApiProjectTag() throws BusinessException {
        String projectName = getProjectTagValue();
        if (Assert.isEmpty(projectName)) {
            return null;
        }
        com.tencentcloudapi.tag.v20180813.models.Tag tag = new com.tencentcloudapi.tag.v20180813.models.Tag();
        tag.setTagKey("eo-user");
        tag.setTagValue(projectName.trim());
        return tag;
    }

    private static String getProjectTagValue() throws BusinessException {
        TencentEdgeOneApiConfigVo config = getConfig();
        if (Assert.notEmpty(config.getTagValue())) {
            return config.getTagValue().trim();
        }
        if (Assert.notEmpty(config.getProjectName())) {
            return config.getProjectName().trim();
        }
        return DEFAULT_PROJECT_TAG_VALUE;
    }

    private static String getPlanId() throws BusinessException {
        String planId = getConfig().getPlanId();
        if (Assert.isEmpty(planId)) {
            throw new BusinessException("请先在后台系统设置中配置腾讯云 EdgeOne 套餐ID（PlanId），根域名站点需要绑定套餐后才能创建加速域名");
        }
        return planId.trim();
    }

    private static TencentEdgeOneApiConfigVo getConfig() throws BusinessException {
        TencentEdgeOneApiConfigVo config = SystemConfig.tencentEdgeOneApiConfigVo;
        if (config == null || Assert.isEmpty(config.getSecretId()) || Assert.isEmpty(config.getSecretKey())) {
            throw new BusinessException("请先在后台系统设置中配置腾讯云 EdgeOne SecretId 和 SecretKey");
        }
        return config;
    }

    public static String formatTencentError(TencentCloudSDKException e) {
        String requestId = e.getRequestId();
        if (Assert.notEmpty(requestId)) {
            return e.getMessage() + "，RequestId：" + requestId;
        }
        return e.getMessage();
    }
}
