package com.kuocai.cdn.service;

import com.kuocai.cdn.config.SystemConfig;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.vo.ResolvedAreaRouteVo;
import com.kuocai.cdn.vo.WebsiteBaseConfigVo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CdnAreaRouteServiceTest {

    private final WebsiteBaseConfigVo originalConfig = SystemConfig.websiteBaseConfig;
    private final CdnAreaRouteService service = new CdnAreaRouteService();

    @AfterEach
    void restoreConfig() {
        SystemConfig.websiteBaseConfig = originalConfig;
    }

    @Test
    void mainlandLoadBalanceKeepsSameRootDomainOnSameRoute() throws BusinessException {
        SystemConfig.websiteBaseConfig = WebsiteBaseConfigVo.builder()
                .mainlandEnabledTargets(Arrays.asList("route:tencent_edgeone", "route:aliyun"))
                .mainlandRouteMode(CdnAreaRouteService.MODE_LOAD_BALANCE)
                .build();

        ResolvedAreaRouteVo first = service.resolve(9L, "aliyun", "a.example.com",
                CdnServiceAreaPolicyService.MAINLAND);
        ResolvedAreaRouteVo second = service.resolve(9L, "aliyun", "b.example.com",
                CdnServiceAreaPolicyService.MAINLAND);

        assertFalse(first.isMultiCdn());
        assertEquals(1, first.getTargets().size());
        assertEquals(first.getPrimaryTarget().getTargetKey(), second.getPrimaryTarget().getTargetKey());
    }

    @Test
    void mainlandMultiCdnReturnsEveryConfiguredRoute() throws BusinessException {
        SystemConfig.websiteBaseConfig = WebsiteBaseConfigVo.builder()
                .mainlandEnabledTargets(Arrays.asList("route:tencent_edgeone", "route:aliyun"))
                .mainlandRouteMode(CdnAreaRouteService.MODE_MULTI_CDN)
                .build();

        ResolvedAreaRouteVo plan = service.resolve(9L, "aliyun", "cdn.example.com",
                CdnServiceAreaPolicyService.MAINLAND);

        assertTrue(plan.isMultiCdn());
        assertEquals(2, plan.getTargets().size());
        assertEquals("route:tencent_edgeone", plan.getTargets().get(0).getTargetKey());
        assertEquals("route:aliyun", plan.getTargets().get(1).getTargetKey());
    }

    @Test
    void areaGroupsAreResolvedIndependently() throws BusinessException {
        SystemConfig.websiteBaseConfig = WebsiteBaseConfigVo.builder()
                .mainlandEnabledTargets(Collections.singletonList("route:tencent_edgeone"))
                .overseasEnabledTargets(Collections.singletonList("route:aliyun"))
                .globalEnabledTargets(Collections.emptyList())
                .build();

        assertEquals("route:tencent_edgeone", service.resolve(9L, "aliyun", "cdn.example.com",
                CdnServiceAreaPolicyService.MAINLAND).getPrimaryTarget().getTargetKey());
        assertEquals("route:aliyun", service.resolve(9L, "aliyun", "cdn.example.com",
                CdnServiceAreaPolicyService.OVERSEAS).getPrimaryTarget().getTargetKey());
        assertThrows(BusinessException.class, () -> service.resolve(9L, "aliyun", "cdn.example.com",
                CdnServiceAreaPolicyService.GLOBAL));
    }

    @Test
    void mainlandWithoutGroupFallsBackToUserRoute() throws BusinessException {
        SystemConfig.websiteBaseConfig = new WebsiteBaseConfigVo();

        ResolvedAreaRouteVo plan = service.resolve(9L, "aliyun", "cdn.example.com",
                CdnServiceAreaPolicyService.MAINLAND);

        assertEquals("route:aliyun", plan.getPrimaryTarget().getTargetKey());
    }

    @Test
    void availabilityUsesConfiguredGroupBeforeUserFallbackRoute() {
        SystemConfig.websiteBaseConfig = WebsiteBaseConfigVo.builder()
                .mainlandEnabledTargets(Collections.singletonList("route:tencent_edgeone"))
                .build();

        assertTrue(service.isAreaAvailable(9L, "unsupported-route",
                CdnServiceAreaPolicyService.MAINLAND));
        assertFalse(service.isAreaAvailable(9L, "unsupported-route",
                CdnServiceAreaPolicyService.OVERSEAS));
    }

    @Test
    void listsSelfHostedTargetsAcrossConfiguredAreas() {
        SystemConfig.websiteBaseConfig = WebsiteBaseConfigVo.builder()
                .overseasEnabledTargets(Collections.singletonList("route:self_hosted_overseas"))
                .globalEnabledTargets(Collections.singletonList("route:self_hosted_global"))
                .build();

        assertEquals(Arrays.asList("self_hosted_overseas", "self_hosted_global"),
                service.configuredSelfHostedRoutes());
    }
}
