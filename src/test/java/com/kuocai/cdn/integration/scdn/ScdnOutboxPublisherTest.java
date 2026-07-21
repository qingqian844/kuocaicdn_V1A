package com.kuocai.cdn.integration.scdn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuocai.cdn.config.ScdnIntegrationProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScdnOutboxPublisherTest {

    @Test
    void publishesEventAndMarksItPublished() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ScdnRabbitEventSender sender = mock(ScdnRabbitEventSender.class);
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
        when(jdbc.queryForList(any(String.class))).thenReturn(Collections.singletonList(row));

        ScdnOutboxPublisher publisher = new ScdnOutboxPublisher(jdbc, sender, new ObjectMapper(), properties);
        publisher.publishPending();

        verify(sender).send(eq("events"), eq("scdn.order.created"), eq("event-12"), any(Map.class));
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(sql.capture(), eq(1), eq(12L));
        assertEquals(true, sql.getValue().contains("status='published'"));
    }
}

