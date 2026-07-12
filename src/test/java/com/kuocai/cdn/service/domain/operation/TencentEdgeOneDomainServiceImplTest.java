package com.kuocai.cdn.service.domain.operation;

import com.kuocai.cdn.api.huawei.cdn.dto.CacheRuleDTO;
import com.kuocai.cdn.api.huawei.cdn.dto.UrlAuthDTO;
import com.tencentcloudapi.teo.v20220901.models.CacheConfigCustomTime;
import com.tencentcloudapi.teo.v20220901.models.CacheConfigParameters;
import com.tencentcloudapi.teo.v20220901.models.CustomRules;
import com.tencentcloudapi.teo.v20220901.models.ModifySecurityPolicyRequest;
import com.tencentcloudapi.teo.v20220901.models.NoCache;
import com.tencentcloudapi.teo.v20220901.models.RuleEngineItem;
import com.tencentcloudapi.teo.v20220901.models.SecurityPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TencentEdgeOneDomainServiceImplTest {

    private final TencentEdgeOneDomainServiceImpl service = new TencentEdgeOneDomainServiceImpl();

    @Test
    void edgeOneSecurityPolicyRequestIncludesSecurityConfig() {
        ModifySecurityPolicyRequest request = ReflectionTestUtils.invokeMethod(
                service,
                "buildModifySecurityPolicyRequest",
                "zone-test",
                "static.example.com",
                new SecurityPolicy()
        );

        assertNotNull(request);
        assertNotNull(request.getSecurityConfig());
        assertNotNull(request.getSecurityPolicy());
        assertTrue(ModifySecurityPolicyRequest.toJsonString(request).contains("\"SecurityConfig\""));
    }

    @Test
    void edgeOneCustomAccessRuleRequestIncludesSecurityConfig() {
        SecurityPolicy policy = new SecurityPolicy();
        policy.setCustomRules(new CustomRules());

        ModifySecurityPolicyRequest request = ReflectionTestUtils.invokeMethod(
                service,
                "buildModifySecurityPolicyRequest",
                "zone-test",
                "static.example.com",
                policy
        );

        assertNotNull(request);
        assertNotNull(request.getSecurityConfig());
        assertTrue(ModifySecurityPolicyRequest.toJsonString(request).contains("\"SecurityConfig\""));
    }

    @Test
    void edgeOneCacheRuleUsesFileExtensionListWithoutDotOrRegexMatches() {
        RuleEngineItem item = ReflectionTestUtils.invokeMethod(
                service,
                "buildCacheRuleEngineItem",
                "static.example.com",
                Arrays.asList(CacheRuleDTO.builder()
                        .match_type("file_extension")
                        .match_value("ipa,apk,zip")
                        .ttl(15)
                        .ttl_unit("d")
                        .follow_origin("off")
                        .build())
        );

        assertNotNull(item);
        String json = RuleEngineItem.toJsonString(item);
        assertTrue(json.contains("${http.request.file_extension} in ['ipa', 'apk', 'zip']"));
        assertTrue(json.contains("\"CustomTime\""));
        assertTrue(json.contains("\"CacheTime\":1296000"));
        assertFalse(json.contains("matches"));
        assertFalse(json.contains(".ipa"));
    }

    @Test
    void edgeOneCatalogCacheRuleUsesLikeExpression() {
        RuleEngineItem item = ReflectionTestUtils.invokeMethod(
                service,
                "buildCacheRuleEngineItem",
                "static.example.com",
                Arrays.asList(CacheRuleDTO.builder()
                        .match_type("catalog")
                        .match_value("/download")
                        .ttl(1)
                        .ttl_unit("h")
                        .follow_origin("off")
                        .build())
        );

        assertNotNull(item);
        String json = RuleEngineItem.toJsonString(item);
        assertTrue(json.contains("${http.request.uri.path} in"));
        assertTrue(json.contains("${http.request.uri.path} like"));
        assertTrue(json.contains("/download"));
        assertFalse(json.contains("matches"));
    }

    @Test
    void edgeOneActiveStatusIsConfigurable() {
        Boolean busy = ReflectionTestUtils.invokeMethod(service, "isDomainBusy", "active");

        assertNotNull(busy);
        assertFalse(busy);
    }

    @Test
    void unchangedGlobalCacheRuleCanBeSkipped() {
        CacheConfigParameters currentCache = new CacheConfigParameters();
        CacheConfigCustomTime customTime = new CacheConfigCustomTime();
        customTime.setSwitch("on");
        customTime.setCacheTime(30L * 24 * 60 * 60);
        currentCache.setCustomTime(customTime);
        NoCache noCache = new NoCache();
        noCache.setSwitch("off");
        currentCache.setNoCache(noCache);

        Boolean same = ReflectionTestUtils.invokeMethod(
                service,
                "isSameGlobalCacheConfig",
                currentCache,
                CacheRuleDTO.builder()
                        .match_type("all")
                        .ttl(30)
                        .ttl_unit("d")
                        .follow_origin("off")
                        .build()
        );

        assertNotNull(same);
        assertTrue(same);
    }

    @Test
    void edgeOneUrlAuthRuleUsesAuthenticationAction() {
        RuleEngineItem item = ReflectionTestUtils.invokeMethod(
                service,
                "buildUrlAuthRule",
                "static.example.com",
                UrlAuthDTO.builder()
                        .status("on")
                        .type("typeB")
                        .primary_key("abcdef123456")
                        .secondary_key("backup123456")
                        .expire_time(1800L)
                        .build()
        );

        assertNotNull(item);
        String json = RuleEngineItem.toJsonString(item);
        assertTrue(json.contains("\"Name\":\"Authentication\""));
        assertTrue(json.contains("\"AuthType\":\"TypeB\""));
        assertTrue(json.contains("\"SecretKey\":\"abcdef123456\""));
        assertTrue(json.contains("\"Timeout\":1800"));
    }
}
