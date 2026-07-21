package com.kuocai.cdn.integration.scdn;

import com.kuocai.cdn.config.ScdnIntegrationProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class ScdnRabbitEventSender {
    private final RabbitTemplate rabbitTemplate;
    private final ScdnIntegrationProperties properties;

    public ScdnRabbitEventSender(RabbitTemplate rabbitTemplate, ScdnIntegrationProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    public void send(String exchange, String routingKey, String eventId, Map<String, Object> event) throws Exception {
        if (!properties.isPublisherConfirms()) {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            return;
        }
        CorrelationData correlation = new CorrelationData(eventId);
        rabbitTemplate.convertAndSend(exchange, routingKey, event, correlation);
        CorrelationData.Confirm confirm = correlation.getFuture()
                .get(properties.getPublisherConfirmTimeoutMs(), TimeUnit.MILLISECONDS);
        if (confirm == null || !confirm.isAck()) {
            throw new IllegalStateException("RabbitMQ rejected SCDN event: "
                    + (confirm == null ? "missing confirmation" : confirm.getReason()));
        }
    }
}
