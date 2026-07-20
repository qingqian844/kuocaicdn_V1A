package com.kuocai.cdn.integration.scdn;

import com.kuocai.cdn.config.ScdnIntegrationProperties;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "scdn.integration.enabled", havingValue = "true")
public class ScdnRabbitConfig {

    @Bean
    TopicExchange scdnPlatformEventExchange(ScdnIntegrationProperties properties) {
        return new TopicExchange(properties.getOutboxExchange(), true, false);
    }
}

