package com.kuocai.cdn.integration.scdn;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ScdnPlatformEventService {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public ScdnPlatformEventService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public boolean enqueue(String eventType, String aggregateType, String aggregateId, Object payload) {
        return enqueue(UUID.randomUUID().toString(), eventType, aggregateType, aggregateId, payload);
    }

    public boolean enqueue(String eventId, String eventType, String aggregateType, String aggregateId, Object payload) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO scdn_outbox_event " +
                            "(event_id,event_type,aggregate_type,aggregate_id,payload_json) VALUES (?,?,?,?,?)",
                    eventId, eventType, aggregateType, aggregateId, objectMapper.writeValueAsString(payload));
            return true;
        } catch (DuplicateKeyException ignored) {
            return false;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create SCDN outbox event", exception);
        }
    }
}
