package com.kuocai.cdn.service;

import com.kuocai.cdn.config.SystemConfig;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.license.LicenseService;
import com.kuocai.cdn.vo.WebsiteBaseConfigVo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CdnServiceAreaPolicyServiceTest {
    private final WebsiteBaseConfigVo originalConfig = SystemConfig.websiteBaseConfig;

    @AfterEach
    void restoreConfig() {
        SystemConfig.websiteBaseConfig = originalConfig;
    }

    @Test
    void missingConfigurationAllowsOnlyMainlandForAuthorizedVendor() {
        LicenseService licenseService = authorizedLicense("tencent_edgeone");
        CdnServiceAreaPolicyService policy = new CdnServiceAreaPolicyService(licenseService);
        SystemConfig.websiteBaseConfig = new WebsiteBaseConfigVo();

        assertTrue(policy.isAllowed("tencent_edgeone", CdnServiceAreaPolicyService.MAINLAND));
        assertFalse(policy.isAllowed("tencent_edgeone", CdnServiceAreaPolicyService.OVERSEAS));
        assertFalse(policy.isAllowed("tencent_edgeone", CdnServiceAreaPolicyService.GLOBAL));
    }

    @Test
    void configuredAreasAreIsolatedByRoute() {
        LicenseService licenseService = mock(LicenseService.class);
        when(licenseService.isVendorAuthorized("tencent_edgeone")).thenReturn(true);
        when(licenseService.isVendorAuthorized("aliyun")).thenReturn(true);
        CdnServiceAreaPolicyService policy = new CdnServiceAreaPolicyService(licenseService);
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
        CdnServiceAreaPolicyService policy = new CdnServiceAreaPolicyService(mock(LicenseService.class));

        assertTrue(policy.isAllowed("self_hosted_mainland", CdnServiceAreaPolicyService.MAINLAND));
        assertFalse(policy.isAllowed("self_hosted_mainland", CdnServiceAreaPolicyService.OVERSEAS));
        assertTrue(policy.isAllowed("self_hosted_overseas", CdnServiceAreaPolicyService.OVERSEAS));
        assertFalse(policy.isAllowed("self_hosted_overseas", CdnServiceAreaPolicyService.GLOBAL));
        assertTrue(policy.isAllowed("self_hosted_global", CdnServiceAreaPolicyService.GLOBAL));
    }

    @Test
    void configuredRoutesAreAuthorizedAndDeduplicated() throws BusinessException {
        LicenseService licenseService = authorizedLicense("tencent_edgeone");
        CdnServiceAreaPolicyService policy = new CdnServiceAreaPolicyService(licenseService);

        assertEquals(Collections.singletonList("tencent_edgeone"),
                policy.normalizeConfiguredRoutes("tencent_edgeone, tencent_edgeone"));
        assertThrows(BusinessException.class,
                () -> policy.normalizeConfiguredRoutes("self_hosted_global"));
        assertThrows(BusinessException.class,
                () -> policy.normalizeConfiguredRoutes("aliyun"));
    }

    private LicenseService authorizedLicense(String... routes) {
        LicenseService licenseService = mock(LicenseService.class);
        Arrays.stream(routes).forEach(route -> when(licenseService.isVendorAuthorized(route)).thenReturn(true));
        return licenseService;
    }
}
