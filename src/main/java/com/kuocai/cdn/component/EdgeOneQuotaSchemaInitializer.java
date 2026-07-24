package com.kuocai.cdn.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
public class EdgeOneQuotaSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    public EdgeOneQuotaSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        execute("CREATE TABLE IF NOT EXISTS edgeone_root_domain_record (" +
                "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "user_id BIGINT NOT NULL," +
                "root_domain VARCHAR(255) NOT NULL," +
                "first_domain_name VARCHAR(255) DEFAULT NULL," +
                "cdn_domain_id BIGINT DEFAULT NULL," +
                "status VARCHAR(32) DEFAULT 'active'," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "UNIQUE KEY uk_edgeone_user_root (user_id, root_domain)," +
                "KEY idx_edgeone_root_user (user_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    private void execute(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            String message = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (!message.contains("duplicate column")) {
                log.warn("EdgeOne quota schema init skipped sql: {}, reason: {}", sql, e.getMessage());
            }
        }
    }
}
