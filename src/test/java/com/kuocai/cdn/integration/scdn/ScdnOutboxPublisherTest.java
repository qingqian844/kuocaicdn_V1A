package com.kuocai.cdn.integration.scdn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuocai.cdn.config.ScdnIntegrationProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScdnOutboxPublisherTest {

    @Test
    void publishesEventAndMarksItPublished() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        ScdnIntegrationProperties properties = new ScdnIntegrationProperties();
        properties.setOutboxExchange("events");
        Map<String, Object> row = new HashMap<>();
        row.put("id", 12L);
        row.put("event_id", "event-12");
        row.put("event_type", "scdn.order.created");
        row.put("aggregate_type", "order");
        row.put("aggregate_id", "order-12");
        row.put("payload_json", "{\"externalOrderId\":\"order-12\"}");
        row.put("attempt_count", 0);
        when(jdbc.queryForList(any(String.class))).thenReturn(List.of(row));

        ScdnOutboxPublisher publisher = new ScdnOutboxPublisher(jdbc, rabbit, new ObjectMapper(), properties);
        publisher.publishPending();

        verify(rabbit).convertAndSend(eq("events"), eq("scdn.order.created"), any(Map.class));
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(sql.capture(), eq(1), eq(12L));
        assertEquals(true, sql.getValue().contains("status='published'"));
    }
}

