package com.kuocai.cdn.service;

import com.kuocai.cdn.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class SetupDomainServiceTest {

    @Test
    void acceptsValidDomain() throws Exception {
        SetupDomainService service = new SetupDomainService(
                mock(CaddyProvisioningService.class), "203.0.113.10");

        assertEquals("cdn.example.com", service.normalizeDomain("HTTPS://CDN.Example.com:443/path"));
    }

    @Test
    void rejectsInvalidDomainBeforeGeneratingCaddyConfiguration() {
        SetupDomainService service = new SetupDomainService(
                mock(CaddyProvisioningService.class), "203.0.113.10");

        assertThrows(BusinessException.class,
                () -> service.normalizeDomain("cdn.example.com\nreverse_proxy evil:80"));
        assertThrows(BusinessException.class, () -> service.normalizeDomain("localhost"));
        assertThrows(BusinessException.class, () -> service.normalizeDomain("192.0.2.1"));
    }
}
