package com.kuocai.cdn.integration.scdn;

import com.kuocai.cdn.config.ScdnIntegrationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class ScdnRabbitEventSenderTest {

    @Test
    void waitsForPositivePublisherConfirmation() throws Exception {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        ScdnIntegrationProperties properties = new ScdnIntegrationProperties();
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().set(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).convertAndSend(eq("events"), eq("scdn.order.paid"), any(Map.class), any(CorrelationData.class));

        new ScdnRabbitEventSender(rabbit, properties).send(
                "events", "scdn.order.paid", "event-1", Collections.singletonMap("eventId", "event-1"));
    }

    @Test
    void rejectsNegativePublisherConfirmation() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        ScdnIntegrationProperties properties = new ScdnIntegrationProperties();
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().set(new CorrelationData.Confirm(false, "exchange rejected message"));
            return null;
        }).when(rabbit).convertAndSend(eq("events"), eq("scdn.order.paid"), any(Map.class), any(CorrelationData.class));

        assertThrows(IllegalStateException.class, () -> new ScdnRabbitEventSender(rabbit, properties).send(
                "events", "scdn.order.paid", "event-2", Collections.singletonMap("eventId", "event-2")));
    }
}
