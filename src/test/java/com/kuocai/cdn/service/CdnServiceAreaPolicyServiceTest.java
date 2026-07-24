package com.kuocai.cdn.service;

import com.kuocai.cdn.config.SystemConfig;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.vo.WebsiteBaseConfigVo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CdnServiceAreaPolicyServiceTest {
    private final WebsiteBaseConfigVo originalConfig = SystemConfig.websiteBaseConfig;
    private final CdnServiceAreaPolicyService policy = new CdnServiceAreaPolicyService();

    @AfterEach
    void restoreConfig() {
        SystemConfig.websiteBaseConfig = originalConfig;
    }

    @Test
    void missingConfigurationAllowsOnlyMainlandForSupportedVendor() {
        SystemConfig.websiteBaseConfig = new WebsiteBaseConfigVo();

        assertTrue(policy.isAllowed("tencent_edgeone", CdnServiceAreaPolicyService.MAINLAND));
        assertFalse(policy.isAllowed("tencent_edgeone", CdnServiceAreaPolicyService.OVERSEAS));
        assertFalse(policy.isAllowed("tencent_edgeone", CdnServiceAreaPolicyService.GLOBAL));
        assertFalse(policy.isAllowed("unknown_vendor", CdnServiceAreaPolicyService.MAINLAND));
    }

    @Test
    void configuredAreasAreIsolatedByRoute() {
        SystemConfig.websiteBaseConfig = WebsiteBaseConfigVo.builder()
                .overseasEnabledRoutes(Collections.singletonList("tencent_edgeone"))
                .globalEnabledRoutes(Collections.singletonList("aliyun"))
                .build();

        assertTrue(policy.isAllowed("tencent_edgeone", CdnServiceAreaPolicyService.OVERSEAS));
        assertFalse(policy.isAllowed("aliyun", CdnServiceAreaPolicyService.OVERSEAS));
        assertTrue(policy.isAllowed("aliyun", CdnServiceAreaPolicyService.GLOBAL));
        assertFalse(policy.isAllowed("tencent_edgeone", CdnServiceAreaPolicyService.GLOBAL));
    }

    @Test
    void selfHostedProductRoutesKeepTheirFixedArea() {
        assertTrue(policy.isAllowed("self_hosted_mainland", CdnServiceAreaPolicyService.MAINLAND));
        assertFalse(policy.isAllowed("self_hosted_mainland", CdnServiceAreaPolicyService.OVERSEAS));
        assertTrue(policy.isAllowed("self_hosted_overseas", CdnServiceAreaPolicyService.OVERSEAS));
        assertFalse(policy.isAllowed("self_hosted_overseas", CdnServiceAreaPolicyService.GLOBAL));
        assertTrue(policy.isAllowed("self_hosted_global", CdnServiceAreaPolicyService.GLOBAL));
    }

    @Test
    void configuredRoutesAreSupportedAndDeduplicated() throws BusinessException {
        assertEquals(Arrays.asList("tencent_edgeone", "aliyun"),
                policy.normalizeConfiguredRoutes("tencent_edgeone, aliyun, tencent_edgeone"));
        assertThrows(BusinessException.class,
                () -> policy.normalizeConfiguredRoutes("multi_cdn"));
        assertThrows(BusinessException.class,
                () -> policy.normalizeConfiguredRoutes("unknown_vendor"));
    }

    @Test
    void configuredRouteTargetsAreValidatedAndDeduplicated() throws BusinessException {
        assertEquals(Collections.singletonList("route:tencent_edgeone"),
                policy.normalizeConfiguredTargets(
                        "route:tencent_edgeone, route:tencent_edgeone",
                        CdnServiceAreaPolicyService.OVERSEAS));
        assertThrows(BusinessException.class,
                () -> policy.normalizeConfiguredTargets(
                        "route:self_hosted_global",
                        CdnServiceAreaPolicyService.OVERSEAS));
    }

    @Test
    void vendorAccountTargetsAreRejectedInOpenSourceEdition() {
        assertThrows(BusinessException.class,
                () -> policy.normalizeConfiguredTargets("account:101"));
        assertThrows(BusinessException.class,
                () -> policy.normalizeConfiguredTargets("account:not-a-number"));
    }
}
