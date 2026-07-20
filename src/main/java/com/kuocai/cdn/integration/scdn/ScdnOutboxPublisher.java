package com.kuocai.cdn.integration.scdn;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuocai.cdn.config.ScdnIntegrationProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "scdn.integration.enabled", havingValue = "true")
public class ScdnOutboxPublisher {
    private final JdbcTemplate jdbcTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final ScdnIntegrationProperties properties;

    public ScdnOutboxPublisher(JdbcTemplate jdbcTemplate,
                               RabbitTemplate rabbitTemplate,
                               ObjectMapper objectMapper,
                               ScdnIntegrationProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${scdn.integration.outbox-publish-delay-ms:5000}")
    public synchronized void publishPending() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id,event_id,event_type,aggregate_type,aggregate_id,payload_json,attempt_count " +
                        "FROM scdn_outbox_event WHERE status='pending' AND available_time<=NOW() ORDER BY id LIMIT 100");
        for (Map<String, Object> row : rows) {
            publish(row);
        }
    }

    private void publish(Map<String, Object> row) {
        Long id = ((Number) row.get("id")).longValue();
        String eventType = row.get("event_type").toString();
        int attempts = ((Number) row.get("attempt_count")).intValue();
        try {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("eventId", row.get("event_id").toString());
            event.put("eventType", eventType);
            event.put("aggregateType", row.get("aggregate_type").toString());
            event.put("aggregateId", row.get("aggregate_id").toString());
            event.put("payload", objectMapper.readValue(
                    row.get("payload_json").toString(), new TypeReference<Map<String, Object>>() {}));
            rabbitTemplate.convertAndSend(properties.getOutboxExchange(), eventType, event);
            jdbcTemplate.update(
                    "UPDATE scdn_outbox_event SET status='published',published_time=NOW(),attempt_count=? WHERE id=? AND status='pending'",
                    attempts + 1, id);
        } catch (Exception e) {
            long delaySeconds = Math.min(300L, 1L << Math.min(8, attempts));
            jdbcTemplate.update(
                    "UPDATE scdn_outbox_event SET attempt_count=?,available_time=? WHERE id=? AND status='pending'",
                    attempts + 1, new Date(System.currentTimeMillis() + delaySeconds * 1000L), id);
        }
    }
}

