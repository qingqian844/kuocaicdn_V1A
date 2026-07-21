package com.kuocai.cdn.integration.scdn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuocai.cdn.config.ScdnIntegrationProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class ScdnInternalAuthFilter extends OncePerRequestFilter {
    static final String TOKEN_HEADER = "X-Scdn-Internal-Token";
    static final String CLIENT_CERTIFICATE_ATTRIBUTE = "javax.servlet.request.X509Certificate";

    private final ScdnIntegrationProperties properties;
    private final ObjectMapper objectMapper;

    public ScdnInternalAuthFilter(ScdnIntegrationProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/scdn/v1/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!properties.isEnabled()) {
            writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "INTEGRATION_DISABLED", "SCDN integration is disabled");
            return;
        }
        if (properties.isMtlsRequired() && !hasVerifiedClientCertificate(request)) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "MTLS_REQUIRED", "A verified SCDN client certificate is required");
            return;
        }
        String expected = properties.getInternalToken();
        String actual = request.getHeader(TOKEN_HEADER);
        if (expected == null || expected.trim().isEmpty()) {
            writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "INTEGRATION_DISABLED", "SCDN internal authentication is not configured");
            return;
        }
        if (actual == null || !MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8))) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "UNAUTHORIZED", "Invalid SCDN internal credentials");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean hasVerifiedClientCertificate(HttpServletRequest request) {
        Object certificates = request.getAttribute(CLIENT_CERTIFICATE_ATTRIBUTE);
        if (certificates instanceof Object[] && ((Object[]) certificates).length > 0) {
            return true;
        }
        String header = properties.getMtlsVerifiedHeader();
        if (header == null || !"SUCCESS".equals(request.getHeader(header))) {
            return false;
        }
        String remoteAddress = request.getRemoteAddr();
        String trusted = properties.getTrustedProxyAddresses();
        if (remoteAddress == null || trusted == null) {
            return false;
        }
        for (String address : trusted.split(",")) {
            if (remoteAddress.equals(address.trim())) {
                return true;
            }
        }
        return false;
    }

    private void writeError(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ScdnContracts.Envelope.failure(code, message));
    }
}

