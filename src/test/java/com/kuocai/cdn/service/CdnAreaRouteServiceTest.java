package com.kuocai.cdn.service;

import com.kuocai.cdn.config.SystemConfig;
import com.kuocai.cdn.entity.CdnVendorAccount;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.license.LicenseService;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CdnAreaRouteServiceTest {

    private final WebsiteBaseConfigVo originalConfig = SystemConfig.websiteBaseConfig;

    @AfterEach
    void restoreConfig() {
        SystemConfig.websiteBaseConfig = originalConfig;
    }

    @Test
    void mainlandLoadBalanceKeepsSameRootDomainOnSameAccount() throws BusinessException {
        CdnAreaRouteService service = serviceWithAccounts();
        SystemConfig.websiteBaseConfig = WebsiteBaseConfigVo.builder()
                .mainlandEnabledTargets(Arrays.asList("account:101", "account:102"))
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
    void mainlandMultiCdnReturnsEveryConfiguredAccount() throws BusinessException {
        CdnAreaRouteService service = serviceWithAccounts();
        SystemConfig.websiteBaseConfig = WebsiteBaseConfigVo.builder()
                .mainlandEnabledTargets(Arrays.asList("account:101", "account:102"))
                .mainlandRouteMode(CdnAreaRouteService.MODE_MULTI_CDN)
                .build();

        ResolvedAreaRouteVo plan = service.resolve(9L, "aliyun", "cdn.example.com",
                CdnServiceAreaPolicyService.MAINLAND);

        assertTrue(plan.isMultiCdn());
        assertEquals(2, plan.getTargets().size());
        assertEquals("account:101", plan.getTargets().get(0).getTargetKey());
        assertEquals("account:102", plan.getTargets().get(1).getTargetKey());
    }

    @Test
    void areaGroupsAreResolvedIndependently() throws BusinessException {
        CdnAreaRouteService service = serviceWithAccounts();
        SystemConfig.websiteBaseConfig = WebsiteBaseConfigVo.builder()
                .mainlandEnabledTargets(Collections.singletonList("account:101"))
                .overseasEnabledTargets(Collections.singletonList("account:102"))
                .globalEnabledTargets(Collections.emptyList())
                .build();

        assertEquals("account:101", service.resolve(9L, "aliyun", "cdn.example.com",
                CdnServiceAreaPolicyService.MAINLAND).getPrimaryTarget().getTargetKey());
        assertEquals("account:102", service.resolve(9L, "aliyun", "cdn.example.com",
                CdnServiceAreaPolicyService.OVERSEAS).getPrimaryTarget().getTargetKey());
        assertThrows(BusinessException.class, () -> service.resolve(9L, "aliyun", "cdn.example.com",
                CdnServiceAreaPolicyService.GLOBAL));
    }

    @Test
    void mainlandWithoutGroupFallsBackToUserRoute() throws BusinessException {
        VendorAccountService accounts = mock(VendorAccountService.class);
        LicenseService license = mock(LicenseService.class);
        CdnVendorAccount aliyun = account(201L, "aliyun", "阿里默认账号");
        when(license.isVendorAuthorized("aliyun")).thenReturn(true);
        when(accounts.supportedVendorCodes()).thenReturn(Collections.singletonList("aliyun"));
        when(accounts.resolveAccount(9L, "aliyun")).thenReturn(aliyun);
        SystemConfig.websiteBaseConfig = new WebsiteBaseConfigVo();

        ResolvedAreaRouteVo plan = new CdnAreaRouteService(accounts, license)
                .resolve(9L, "aliyun", "cdn.example.com", CdnServiceAreaPolicyService.MAINLAND);

        assertEquals("account:201", plan.getPrimaryTarget().getTargetKey());
    }

    @Test
    void availabilityUsesConfiguredGroupBeforeUserFallbackRoute() {
        CdnAreaRouteService service;
        try {
            service = serviceWithAccounts();
        } catch (BusinessException e) {
            throw new AssertionError(e);
        }
        SystemConfig.websiteBaseConfig = WebsiteBaseConfigVo.builder()
                .mainlandEnabledTargets(Collections.singletonList("account:101"))
                .build();

        assertTrue(service.isAreaAvailable(9L, "unauthorized-route",
                CdnServiceAreaPolicyService.MAINLAND));
        assertFalse(service.isAreaAvailable(9L, "unauthorized-route",
                CdnServiceAreaPolicyService.OVERSEAS));
    }

    @Test
    void unavailableAccountDoesNotDisableOtherTargets() throws BusinessException {
        VendorAccountService accounts = mock(VendorAccountService.class);
        LicenseService license = mock(LicenseService.class);
        when(accounts.resolveAccountById(101L)).thenThrow(new BusinessException("账号已停用"));
        when(accounts.resolveAccountById(102L)).thenReturn(account(102L, "aliyun", "阿里主账号"));
        when(license.isVendorAuthorized("aliyun")).thenReturn(true);
        SystemConfig.websiteBaseConfig = WebsiteBaseConfigVo.builder()
                .mainlandEnabledTargets(Arrays.asList("account:101", "account:102"))
                .mainlandRouteMode(CdnAreaRouteService.MODE_MULTI_CDN)
                .build();

        ResolvedAreaRouteVo plan = new CdnAreaRouteService(accounts, license)
                .resolve(9L, "aliyun", "cdn.example.com", CdnServiceAreaPolicyService.MAINLAND);

        assertEquals(1, plan.getTargets().size());
        assertEquals("account:102", plan.getPrimaryTarget().getTargetKey());
    }

    private CdnAreaRouteService serviceWithAccounts() throws BusinessException {
        VendorAccountService accounts = mock(VendorAccountService.class);
        LicenseService license = mock(LicenseService.class);
        CdnVendorAccount first = account(101L, "tencent_edgeone", "EO 主账号");
        CdnVendorAccount second = account(102L, "aliyun", "阿里主账号");
        when(accounts.resolveAccountById(101L)).thenReturn(first);
        when(accounts.resolveAccountById(102L)).thenReturn(second);
        when(license.isVendorAuthorized("tencent_edgeone")).thenReturn(true);
        when(license.isVendorAuthorized("aliyun")).thenReturn(true);
        return new CdnAreaRouteService(accounts, license);
    }

    private CdnVendorAccount account(Long id, String vendorCode, String name) {
        return CdnVendorAccount.builder()
                .id(id)
                .vendorCode(vendorCode)
                .accountName(name)
                .status(VendorAccountService.STATUS_ENABLED)
                .build();
    }
}
