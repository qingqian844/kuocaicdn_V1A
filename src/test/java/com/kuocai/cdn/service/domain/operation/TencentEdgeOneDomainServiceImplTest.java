package com.kuocai.cdn.service.domain.operation;

import com.kuocai.cdn.api.huawei.cdn.dto.CacheRuleDTO;
import com.tencentcloudapi.teo.v20220901.models.ModifySecurityPolicyRequest;
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
}
