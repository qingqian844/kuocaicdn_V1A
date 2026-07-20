package com.kuocai.cdn.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "scdn.integration")
public class ScdnIntegrationProperties {
    private boolean enabled;
    private String consoleUrl = "http://localhost:8090";
    private String internalToken;
    private int ssoCodeTtlSeconds = 60;
    private int accessTokenTtlSeconds = 900;
    private String outboxExchange = "kuocai.scdn.platform.events";
    private long outboxPublishDelayMs = 5000L;
}
