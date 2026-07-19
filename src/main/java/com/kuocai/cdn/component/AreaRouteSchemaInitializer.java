package com.kuocai.cdn.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
public class AreaRouteSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    public AreaRouteSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        execute("CREATE TABLE IF NOT EXISTS cdn_domain_route_binding (" +
                "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "domain_id BIGINT NOT NULL," +
                "user_id BIGINT NOT NULL," +
                "domain_name VARCHAR(255) NOT NULL," +
                "service_area VARCHAR(64) NOT NULL," +
                "route VARCHAR(64) NOT NULL," +
                "target_key VARCHAR(128) NOT NULL," +
                "upstream_domain_id VARCHAR(255) DEFAULT NULL," +
                "upstream_cname VARCHAR(255) DEFAULT NULL," +
                "domain_snapshot_json LONGTEXT DEFAULT NULL," +
                "local_domain_id BIGINT DEFAULT NULL," +
                "dns_record_id BIGINT DEFAULT NULL," +
                "primary_binding TINYINT NOT NULL DEFAULT 0," +
                "status VARCHAR(32) NOT NULL DEFAULT 'active'," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "UNIQUE KEY uk_domain_route_target (domain_id, target_key)," +
                "KEY idx_route_binding_domain (domain_id, status)," +
                "KEY idx_route_binding_route (route)," +
                "KEY idx_route_binding_user (user_id, service_area)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    private void execute(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            log.warn("区域线路组表结构初始化失败：{}", e.getMessage());
        }
    }
}
