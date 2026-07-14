package com.kuocai.cdn.service.domain.operation;

import com.kuocai.cdn.api.huawei.cdn.dto.CacheRuleDTO;
import com.kuocai.cdn.api.huawei.cdn.dto.UrlAuthDTO;
import com.tencentcloudapi.teo.v20220901.models.CacheConfigCustomTime;
import com.tencentcloudapi.teo.v20220901.models.CacheConfigParameters;
import com.tencentcloudapi.teo.v20220901.models.CustomRule;
import com.tencentcloudapi.teo.v20220901.models.CustomRules;
import com.tencentcloudapi.teo.v20220901.models.DenyActionParameters;
import com.tencentcloudapi.teo.v20220901.models.ModifySecurityPolicyRequest;
import com.tencentcloudapi.teo.v20220901.models.NoCache;
import com.tencentcloudapi.teo.v20220901.models.RuleEngineItem;
import com.tencentcloudapi.teo.v20220901.models.SecurityAction;
import com.tencentcloudapi.teo.v20220901.models.SecurityPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TencentEdgeOneDomainServiceImplTest {

    private final TencentEdgeOneDomainServiceImpl service = new TencentEdgeOneDomainServiceImpl();

    @Test
    void edgeOneSecurityPolicyRequestIncludesSecurityConfig() {
        SecurityPolicy policy = new SecurityPolicy();
        CustomRules customRules = new CustomRules();
        customRules.setRules(new CustomRule[0]);
        policy.setCustomRules(customRules);

        ModifySecurityPolicyRequest request = ReflectionTestUtils.invokeMethod(
                service,
                "buildModifySecurityPolicyRequest",
                "zone-test",
                "static.example.com",
                policy
        );

        assertNotNull(request);
        assertNotNull(request.getSecurityConfig());
        assertNotNull(request.getSecurityPolicy());
        HashMap<String, String> parameters = new HashMap<>();
        request.toMap(parameters, "");
        assertEquals("off", parameters.get("SecurityConfig.AclConfig.Switch"));
    }

    @Test
    void edgeOneCustomAccessRuleRequestIncludesSecurityConfig() {
        SecurityPolicy policy = new SecurityPolicy();
        policy.setCustomRules(new CustomRules());

        ModifySecurityPolicyRequest request = ReflectionTestUtils.invokeMethod(
                service,
                "buildModifyZoneDefaultSecurityPolicyRequest",
                "zone-test",
                policy
        );

        assertNotNull(request);
        assertNotNull(request.getSecurityConfig());
        HashMap<String, String> parameters = new HashMap<>();
        request.toMap(parameters, "");
        assertEquals("off", parameters.get("SecurityConfig.AclConfig.Switch"));
        assertEquals("ZoneDefaultPolicy", parameters.get("Entity"));
    }

    @Test
    void edgeOneRefererRuleUsesPreciseMatchWithoutDenyParameters() {
        CustomRule rule = ReflectionTestUtils.invokeMethod(
                service,
                "buildDenyRule",
                "kuocai_referer",
                "${http.request.headers['referer']} like ['*example.com*']",
                10L
        );

        assertNotNull(rule);
        assertEquals("PreciseMatchRule", rule.getRuleType());
        assertEquals(10L, rule.getPriority());
        assertNotNull(rule.getAction());
        assertEquals("Deny", rule.getAction().getName());
        assertNull(rule.getAction().getDenyActionParameters());
    }

    @Test
    void edgeOneIpRuleUsesBasicAccessWithoutPriority() {
        CustomRule rule = ReflectionTestUtils.invokeMethod(
                service,
                "buildBasicDenyRule",
                "kuocai_ip_acl",
                "${http.request.ip} in ['192.0.2.10']"
        );

        assertNotNull(rule);
        assertEquals("BasicAccessRule", rule.getRuleType());
        assertNull(rule.getPriority());
        assertNull(rule.getAction().getDenyActionParameters());
    }

    @Test
    void outputOnlyManagedAccessRuleIsNotSubmittedAgain() {
        CustomRule managedRule = new CustomRule();
        managedRule.setRuleType("ManagedAccessRule");
        managedRule.setName("managed-rule");

        CustomRule copied = ReflectionTestUtils.invokeMethod(
                service,
                "copyCustomRuleForSubmit",
                managedRule
        );

        assertNull(copied);
    }

    @Test
    void copiedDenyRuleDropsLegacyResponseCode() {
        DenyActionParameters denyParameters = new DenyActionParameters();
        denyParameters.setResponseCode("403");
        SecurityAction action = new SecurityAction();
        action.setName("Deny");
        action.setDenyActionParameters(denyParameters);
        CustomRule existing = new CustomRule();
        existing.setName("existing-rule");
        existing.setCondition("${http.request.ip} in ['192.0.2.20']");
        existing.setEnabled("on");
        existing.setRuleType("PreciseMatchRule");
        existing.setPriority(20L);
        existing.setAction(action);

        CustomRule copied = ReflectionTestUtils.invokeMethod(
                service,
                "copyCustomRuleForSubmit",
                existing
        );

        assertNotNull(copied);
        assertNotNull(copied.getAction());
        assertEquals("Deny", copied.getAction().getName());
        assertNull(copied.getAction().getDenyActionParameters());
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
