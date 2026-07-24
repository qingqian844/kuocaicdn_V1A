package com.kuocai.cdn.controller.system;

import com.kuocai.cdn.service.CdnServiceAreaPolicyService;
import com.kuocai.cdn.vo.CdnServiceAreaOptionVo;
import com.kuocai.cdn.vo.WebsiteBaseConfigVo;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemSettingsPageControllerTest {

    @Test
    void areaOptionsUseVendorRoutesAndIncludeSelfHostedProducts() {
        SystemSettingsPageController controller = new SystemSettingsPageController();
        WebsiteBaseConfigVo config = WebsiteBaseConfigVo.builder()
                .overseasEnabledTargets(java.util.Arrays.asList(
                        CdnServiceAreaPolicyService.routeTarget("tencent_edgeone"),
                        CdnServiceAreaPolicyService.routeTarget("self_hosted_overseas")))
                .build();

        List<CdnServiceAreaOptionVo> options = ReflectionTestUtils.invokeMethod(
                controller, "buildServiceAreaOptions", config);

        CdnServiceAreaOptionVo edgeOne = options.stream()
                .filter(option -> "tencent_edgeone".equals(option.getRoute())).findFirst()
                .orElseThrow(() -> new AssertionError("EdgeOne route missing"));
        CdnServiceAreaOptionVo selfHosted = options.stream()
                .filter(option -> "self_hosted_overseas".equals(option.getRoute())).findFirst()
                .orElseThrow(() -> new AssertionError("self-hosted overseas route missing"));
        assertTrue(edgeOne.getOverseasEnabled());
        assertEquals("海外自建 CDN", selfHosted.getRouteName());
        assertEquals(CdnServiceAreaPolicyService.OVERSEAS, selfHosted.getFixedArea());
        assertTrue(selfHosted.getOverseasEnabled());
        assertTrue(selfHosted.getSelectable());
    }
}
