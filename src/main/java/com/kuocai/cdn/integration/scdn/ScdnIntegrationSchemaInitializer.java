package com.kuocai.cdn.integration.scdn;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@ConditionalOnProperty(name = "scdn.integration.enabled", havingValue = "true")
public class ScdnIntegrationSchemaInitializer {
    private final JdbcTemplate jdbcTemplate;

    public ScdnIntegrationSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initialize() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS scdn_order_link (" +
                "id BIGINT NOT NULL AUTO_INCREMENT,external_order_id VARCHAR(96) NOT NULL," +
                "transaction_order_id BIGINT NOT NULL,user_id BIGINT NOT NULL,amount DECIMAL(18,6) NOT NULL," +
                "last_event_status VARCHAR(32) NULL," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP,update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "PRIMARY KEY (id),UNIQUE KEY uk_scdn_external_order (external_order_id)," +
                "UNIQUE KEY uk_scdn_platform_order (transaction_order_id),KEY idx_scdn_order_user (user_id,create_time)) " +
                "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS scdn_wallet_ledger (" +
                "id BIGINT NOT NULL AUTO_INCREMENT,business_reference VARCHAR(128) NOT NULL," +
                "original_business_reference VARCHAR(128) NULL,operation VARCHAR(16) NOT NULL," +
                "transaction_order_id BIGINT NOT NULL,user_id BIGINT NOT NULL,amount DECIMAL(18,6) NOT NULL," +
                "balance_after DECIMAL(18,6) NOT NULL,create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "PRIMARY KEY (id),UNIQUE KEY uk_scdn_wallet_reference (business_reference)," +
                "KEY idx_scdn_wallet_user (user_id,create_time),KEY idx_scdn_wallet_original (original_business_reference)) " +
                "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS scdn_idempotency_record (" +
                "id BIGINT NOT NULL AUTO_INCREMENT,idempotency_key VARCHAR(128) NOT NULL," +
                "operation_type VARCHAR(32) NOT NULL,request_hash VARCHAR(64) NOT NULL,response_json LONGTEXT NULL," +
                "status VARCHAR(16) NOT NULL DEFAULT 'processing',create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "PRIMARY KEY (id),UNIQUE KEY uk_scdn_idempotency_key (idempotency_key)) " +
                "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS scdn_outbox_event (" +
                "id BIGINT NOT NULL AUTO_INCREMENT,event_id VARCHAR(64) NOT NULL,event_type VARCHAR(64) NOT NULL," +
                "aggregate_type VARCHAR(32) NOT NULL,aggregate_id VARCHAR(128) NOT NULL,payload_json LONGTEXT NOT NULL," +
                "status VARCHAR(16) NOT NULL DEFAULT 'pending',attempt_count INT NOT NULL DEFAULT 0," +
                "available_time DATETIME DEFAULT CURRENT_TIMESTAMP,create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "published_time DATETIME NULL,PRIMARY KEY (id),UNIQUE KEY uk_scdn_outbox_event (event_id)," +
                "KEY idx_scdn_outbox_publish (status,available_time)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS scdn_user_state_snapshot (" +
                "user_id BIGINT NOT NULL,account_status VARCHAR(32) NOT NULL,real_name_verified TINYINT(1) NOT NULL," +
                "agent_user_id BIGINT NULL,banned TINYINT(1) NOT NULL,update_time DATETIME DEFAULT CURRENT_TIMESTAMP " +
                "ON UPDATE CURRENT_TIMESTAMP,PRIMARY KEY (user_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }
}

