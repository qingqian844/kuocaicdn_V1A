package com.kuocai.cdn.service;

import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.license.HostLicenseValidator;
import com.kuocai.cdn.license.LicenseService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SetupDomainServiceTest {

    @Test
    void acceptsAuthorizedDomainAndSubdomain() throws Exception {
        LicenseService licenseService = mock(LicenseService.class);
        when(licenseService.isHostAuthorized("cdn.example.com")).thenReturn(true);
        SetupDomainService service = new SetupDomainService(licenseService, new HostLicenseValidator(),
                mock(CaddyProvisioningService.class), "203.0.113.10");

        assertEquals("cdn.example.com", service.normalizeAndAuthorize("HTTPS://CDN.Example.com:443/path"));
    }

    @Test
    void rejectsInvalidDomainBeforeGeneratingCaddyConfiguration() {
        SetupDomainService service = new SetupDomainService(mock(LicenseService.class),
                new HostLicenseValidator(), mock(CaddyProvisioningService.class), "203.0.113.10");

        assertThrows(BusinessException.class,
                () -> service.normalizeAndAuthorize("cdn.example.com\nreverse_proxy evil:80"));
        assertThrows(BusinessException.class, () -> service.normalizeAndAuthorize("localhost"));
        assertThrows(BusinessException.class, () -> service.normalizeAndAuthorize("192.0.2.1"));
    }
}
