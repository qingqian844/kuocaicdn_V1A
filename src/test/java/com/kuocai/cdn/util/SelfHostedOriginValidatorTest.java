package com.kuocai.cdn.util;

import com.kuocai.cdn.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SelfHostedOriginValidatorTest {

    @Test
    void rejectsIncompleteAndOutOfRangeIpAddresses() {
        assertThrows(BusinessException.class,
                () -> SelfHostedOriginValidator.validateAndNormalize("ipaddr", "225.667.21"));
        assertThrows(BusinessException.class,
                () -> SelfHostedOriginValidator.validateAndNormalize("ipaddr", "999.1.1.1"));
        assertThrows(BusinessException.class,
                () -> SelfHostedOriginValidator.validateAndNormalize("ipaddr", "192.0.2.1;;192.0.2.2"));
    }

    @Test
    void acceptsAndNormalizesIpAddressLists() throws Exception {
        assertEquals("192.0.2.1;2001:db8::1",
                SelfHostedOriginValidator.validateAndNormalize(
                        "ipaddr", "192.0.2.1， [2001:db8::1]"));
        assertEquals("192.0.2.1",
                SelfHostedOriginValidator.validateAndNormalize(null, "192.0.2.1"));
    }

    @Test
    void acceptsValidHostnamesAndRejectsInvalidOnes() throws Exception {
        assertEquals("origin.example.com;backup.example.com",
                SelfHostedOriginValidator.validateAndNormalize(
                        "domain", "Origin.Example.com；backup.example.com."));
        assertThrows(BusinessException.class,
                () -> SelfHostedOriginValidator.validateAndNormalize("domain", "225.667.21"));
        assertThrows(BusinessException.class,
                () -> SelfHostedOriginValidator.validateAndNormalize("domain", "bad_host.example.com"));
    }
}
