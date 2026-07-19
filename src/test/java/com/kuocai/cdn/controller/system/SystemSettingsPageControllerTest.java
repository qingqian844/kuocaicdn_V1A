package com.kuocai.cdn.controller.system;

import com.kuocai.cdn.entity.CdnVendorAccount;
import com.kuocai.cdn.license.LicenseService;
import com.kuocai.cdn.license.LicenseVendorOption;
import com.kuocai.cdn.service.CdnServiceAreaPolicyService;
import com.kuocai.cdn.service.VendorAccountService;
import com.kuocai.cdn.vo.CdnServiceAreaOptionVo;
import com.kuocai.cdn.vo.WebsiteBaseConfigVo;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SystemSettingsPageControllerTest {

    @Test
    void areaOptionsSeparateVendorAccountsAndIncludeSelfHostedProducts() {
        SystemSettingsPageController controller = new SystemSettingsPageController();
        LicenseService licenseService = mock(LicenseService.class);
        VendorAccountService vendorAccountService = mock(VendorAccountService.class);
        when(licenseService.getAuthorizedVendorOptions()).thenReturn(Arrays.asList(
                new LicenseVendorOption("tencent_edgeone", "腾讯云 EdgeOne"),
                new LicenseVendorOption("self_hosted_overseas", "海外自建 CDN")));
        when(vendorAccountService.supportedVendorCodes())
                .thenReturn(Collections.singletonList("tencent_edgeone"));
        when(vendorAccountService.listByVendor("tencent_edgeone")).thenReturn(Arrays.asList(
                account(101L, "EO主账号", 1), account(102L, "EO备用账号", 0)));
        ReflectionTestUtils.setField(controller, "licenseService", licenseService);
        ReflectionTestUtils.setField(controller, "vendorAccountService", vendorAccountService);
        WebsiteBaseConfigVo config = WebsiteBaseConfigVo.builder()
                .overseasEnabledTargets(Collections.singletonList(
                        CdnServiceAreaPolicyService.accountTarget(101L)))
                .build();

        List<CdnServiceAreaOptionVo> options = ReflectionTestUtils.invokeMethod(
                controller, "buildServiceAreaOptions", config);

        assertEquals(3, options.size());
        assertEquals("EO主账号", options.get(0).getAccountName());
        assertTrue(options.get(0).getOverseasEnabled());
        assertFalse(options.get(1).getOverseasEnabled());
        assertEquals("海外自建 CDN", options.get(2).getRouteName());
        assertEquals(CdnServiceAreaPolicyService.OVERSEAS, options.get(2).getFixedArea());
        assertTrue(options.get(2).getOverseasEnabled());
        assertFalse(options.get(2).getSelectable());
    }

    private CdnVendorAccount account(Long id, String name, int isDefault) {
        return CdnVendorAccount.builder()
                .id(id)
                .vendorCode("tencent_edgeone")
                .accountName(name)
                .isDefault(isDefault)
                .status(VendorAccountService.STATUS_ENABLED)
                .build();
    }
}
