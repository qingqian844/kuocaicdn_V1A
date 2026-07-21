package com.kuocai.cdn.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class ScdnIntegrationConfigurationValidator {
    private final ScdnIntegrationProperties properties;
    private final String jwtPublicKey;
    private final String jwtPrivateKey;

    public ScdnIntegrationConfigurationValidator(
            ScdnIntegrationProperties properties,
            @Value("${security.jwt.public-key:}") String jwtPublicKey,
            @Value("${security.jwt.private-key:}") String jwtPrivateKey) {
        this.properties = properties;
        this.jwtPublicKey = jwtPublicKey;
        this.jwtPrivateKey = jwtPrivateKey;
    }

    @PostConstruct
    public void validate() {
        if (!properties.isEnabled()) {
            return;
        }
        require(properties.getConsoleUrl() != null && properties.getConsoleUrl().startsWith("https://"),
                "SCDN_CONSOLE_URL must use HTTPS when SCDN integration is enabled");
        require(properties.getInternalToken() != null && properties.getInternalToken().length() >= 32,
                "SCDN_INTERNAL_TOKEN must contain at least 32 characters");
        require(properties.getSsoCodeTtlSeconds() > 0 && properties.getSsoCodeTtlSeconds() <= 60,
                "SCDN_SSO_CODE_TTL_SECONDS must be between 1 and 60");
        require(properties.getAccessTokenTtlSeconds() >= 60 && properties.getAccessTokenTtlSeconds() <= 900,
                "SCDN_ACCESS_TOKEN_TTL_SECONDS must be between 60 and 900");
        require(notBlank(jwtPublicKey) && notBlank(jwtPrivateKey),
                "JWT_PUBLIC_KEY and JWT_PRIVATE_KEY are required for SCDN SSO");
        if (properties.isMtlsRequired()) {
            require(notBlank(properties.getMtlsVerifiedHeader()) && notBlank(properties.getTrustedProxyAddresses()),
                    "SCDN mTLS proxy verification settings are required");
        }
        require(properties.isPublisherConfirms(),
                "SCDN RabbitMQ publisher confirms must remain enabled");
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void require(boolean valid, String message) {
        if (!valid) {
            throw new IllegalStateException(message);
        }
    }
}
