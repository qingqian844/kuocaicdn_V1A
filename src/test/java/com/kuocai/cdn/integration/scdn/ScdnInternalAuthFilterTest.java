package com.kuocai.cdn.integration.scdn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuocai.cdn.config.ScdnIntegrationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScdnInternalAuthFilterTest {

    @Test
    void rejectsMissingInternalToken() throws Exception {
        ScdnIntegrationProperties properties = new ScdnIntegrationProperties();
        properties.setEnabled(true);
        properties.setMtlsRequired(false);
        properties.setInternalToken("test-internal-token");
        ScdnInternalAuthFilter filter = new ScdnInternalAuthFilter(properties, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/scdn/v1/users/1/eligibility");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
        assertEquals("UNAUTHORIZED", new ObjectMapper().readTree(response.getContentAsString()).get("code").asText());
    }

    @Test
    void acceptsMatchingInternalToken() throws Exception {
        ScdnIntegrationProperties properties = new ScdnIntegrationProperties();
        properties.setEnabled(true);
        properties.setInternalToken("test-internal-token");
        ScdnInternalAuthFilter filter = new ScdnInternalAuthFilter(properties, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/scdn/v1/users/1/eligibility");
        request.addHeader(ScdnInternalAuthFilter.TOKEN_HEADER, "test-internal-token");
        request.addHeader(properties.getMtlsVerifiedHeader(), "SUCCESS");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
    }

    @Test
    void rejectsTokenWhenClientCertificateWasNotVerified() throws Exception {
        ScdnIntegrationProperties properties = new ScdnIntegrationProperties();
        properties.setEnabled(true);
        properties.setInternalToken("test-internal-token");
        ScdnInternalAuthFilter filter = new ScdnInternalAuthFilter(properties, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/scdn/v1/users/1/eligibility");
        request.addHeader(ScdnInternalAuthFilter.TOKEN_HEADER, "test-internal-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
        assertEquals("MTLS_REQUIRED", new ObjectMapper().readTree(response.getContentAsString()).get("code").asText());
    }
}

