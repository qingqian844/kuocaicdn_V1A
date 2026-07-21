package com.kuocai.cdn.util;

import com.kuocai.cdn.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SelfHostedDomainValidatorTest {

    @Test
    void normalizesRegularAndWildcardDomains() throws Exception {
        assertEquals("cdn.example.com", SelfHostedDomainValidator.validateAndNormalize(" CDN.Example.COM "));
        assertEquals("*.example.com", SelfHostedDomainValidator.validateAndNormalize(" *.Example.COM "));
        assertEquals(".example.com", SelfHostedDomainValidator.toAgentServerName("*.example.com"));
        assertEquals("example.com", SelfHostedDomainValidator.defaultOriginHost("*.example.com"));
    }

    @Test
    void rejectsUnsafeOrMalformedDomains() {
        assertThrows(BusinessException.class,
                () -> SelfHostedDomainValidator.validateAndNormalize("https://cdn.example.com/path"));
        assertThrows(BusinessException.class,
                () -> SelfHostedDomainValidator.validateAndNormalize("cdn_example.com"));
        assertThrows(BusinessException.class,
                () -> SelfHostedDomainValidator.validateAndNormalize("foo.*.example.com"));
        assertThrows(BusinessException.class,
                () -> SelfHostedDomainValidator.validateAndNormalize("-cdn.example.com"));
    }
}
