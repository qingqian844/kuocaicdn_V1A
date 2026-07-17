package com.kuocai.cdn.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@DependsOn("preloadComponent")
public class SelfHostedCdnSchemaInitializer {
    private final JdbcTemplate jdbcTemplate;

    public SelfHostedCdnSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        execute("CREATE TABLE IF NOT EXISTS self_hosted_node (" +
                "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "node_name VARCHAR(128) NOT NULL,host VARCHAR(255) NOT NULL,ssh_port INT NOT NULL DEFAULT 22," +
                "ssh_username VARCHAR(128) NOT NULL,ssh_password_cipher LONGTEXT NULL,ssh_host_key VARCHAR(512) NULL," +
                "agent_token_hash VARCHAR(128) NULL,region VARCHAR(64) NULL,weight INT NOT NULL DEFAULT 100," +
                "enabled TINYINT NOT NULL DEFAULT 1,status VARCHAR(32) NOT NULL DEFAULT 'pending'," +
                "last_heartbeat DATETIME NULL,agent_version VARCHAR(64) NULL,desired_config_version BIGINT NOT NULL DEFAULT 0," +
                "applied_config_version BIGINT NOT NULL DEFAULT 0,cpu_usage DECIMAL(6,2) NULL,memory_usage DECIMAL(6,2) NULL," +
                "disk_usage DECIMAL(6,2) NULL,rx_bytes BIGINT NOT NULL DEFAULT 0,tx_bytes BIGINT NOT NULL DEFAULT 0," +
                "cache_bytes BIGINT NOT NULL DEFAULT 0,last_error VARCHAR(1000) NULL,remark VARCHAR(512) NULL," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP,update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "UNIQUE KEY uk_self_hosted_node_endpoint (host,ssh_port),KEY idx_self_hosted_node_status (enabled,status,last_heartbeat)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        execute("CREATE TABLE IF NOT EXISTS self_hosted_node_group (" +
                "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,group_name VARCHAR(128) NOT NULL,cname_label VARCHAR(64) NOT NULL," +
                "coverage VARCHAR(32) NOT NULL DEFAULT 'legacy',dns_record_ids LONGTEXT NULL," +
                "is_default TINYINT NOT NULL DEFAULT 0,status VARCHAR(32) NOT NULL DEFAULT 'enabled',remark VARCHAR(512) NULL," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP,update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "UNIQUE KEY uk_self_hosted_group_name (group_name),UNIQUE KEY uk_self_hosted_group_cname (cname_label)," +
                "KEY idx_self_hosted_group_coverage (coverage,status,is_default)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        execute("CREATE TABLE IF NOT EXISTS self_hosted_group_node (" +
                "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,group_id BIGINT NOT NULL,node_id BIGINT NOT NULL," +
                "weight INT NOT NULL DEFAULT 100,priority INT NOT NULL DEFAULT 100," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP,update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "UNIQUE KEY uk_self_hosted_group_node (group_id,node_id),KEY idx_self_hosted_group_node_node (node_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        execute("CREATE TABLE IF NOT EXISTS self_hosted_domain_config (" +
                "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,cdn_domain_id BIGINT NOT NULL,node_group_id BIGINT NOT NULL," +
                "origin_type VARCHAR(32) NOT NULL,origin_address VARCHAR(2000) NOT NULL,origin_protocol VARCHAR(16) NOT NULL DEFAULT 'http'," +
                "http_port INT NOT NULL DEFAULT 80,https_port INT NOT NULL DEFAULT 443,origin_host VARCHAR(255) NULL," +
                "cache_config_json LONGTEXT NULL,https_enabled TINYINT NOT NULL DEFAULT 0,certificate_cipher LONGTEXT NULL," +
                "private_key_cipher LONGTEXT NULL,force_redirect VARCHAR(16) NOT NULL DEFAULT 'off'," +
                "desired_config_version BIGINT NOT NULL DEFAULT 1,status VARCHAR(32) NOT NULL DEFAULT 'enabled',last_error VARCHAR(1000) NULL," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP,update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "UNIQUE KEY uk_self_hosted_domain_config (cdn_domain_id),KEY idx_self_hosted_domain_group (node_group_id,status)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        execute("CREATE TABLE IF NOT EXISTS self_hosted_cache_job (" +
                "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,task_id VARCHAR(64) NOT NULL,user_id BIGINT NOT NULL," +
                "operation VARCHAR(32) NOT NULL,target_type VARCHAR(32) NOT NULL,targets_json LONGTEXT NOT NULL," +
                "status VARCHAR(32) NOT NULL DEFAULT 'pending',total_nodes INT NOT NULL DEFAULT 0,success_nodes INT NOT NULL DEFAULT 0," +
                "failed_nodes INT NOT NULL DEFAULT 0,create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "UNIQUE KEY uk_self_hosted_cache_task (task_id),KEY idx_self_hosted_cache_user (user_id,create_time)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        execute("CREATE TABLE IF NOT EXISTS self_hosted_cache_job_node (" +
                "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,job_id BIGINT NOT NULL,node_id BIGINT NOT NULL,targets_json LONGTEXT NOT NULL," +
                "status VARCHAR(32) NOT NULL DEFAULT 'pending',last_error VARCHAR(1000) NULL," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP,update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "UNIQUE KEY uk_self_hosted_cache_job_node (job_id,node_id),KEY idx_self_hosted_cache_node_status (node_id,status)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        addColumnIfAbsent("self_hosted_node_group", "dns_record_ids",
                "ALTER TABLE self_hosted_node_group ADD COLUMN dns_record_ids LONGTEXT NULL AFTER cname_label");
        addColumnIfAbsent("self_hosted_node_group", "coverage",
                "ALTER TABLE self_hosted_node_group ADD COLUMN coverage VARCHAR(32) NOT NULL DEFAULT 'legacy' AFTER cname_label");
        addIndexIfAbsent("self_hosted_node_group", "idx_self_hosted_group_coverage",
                "ALTER TABLE self_hosted_node_group ADD INDEX idx_self_hosted_group_coverage (coverage,status,is_default)");
        addColumnIfAbsent("self_hosted_cache_job_node", "targets_json",
                "ALTER TABLE self_hosted_cache_job_node ADD COLUMN targets_json LONGTEXT NULL AFTER node_id");
        execute("INSERT INTO self_hosted_node_group (group_name,cname_label,coverage,is_default,status) " +
                "SELECT '默认节点组','edge','legacy',1,'enabled' WHERE NOT EXISTS (SELECT 1 FROM self_hosted_node_group WHERE is_default=1)");
    }

    private void execute(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            log.warn("Self-hosted CDN schema initialization failed: {}", e.getMessage());
        }
    }

    private void addColumnIfAbsent(String tableName, String columnName, String sql) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM information_schema.columns " +
                            "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                    Integer.class, tableName, columnName);
            if (count == null || count == 0) {
                jdbcTemplate.execute(sql);
            }
        } catch (Exception e) {
            log.warn("Self-hosted CDN column initialization failed: {}.{}: {}",
                    tableName, columnName, e.getMessage());
        }
    }

    private void addIndexIfAbsent(String tableName, String indexName, String sql) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM information_schema.statistics " +
                            "WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?",
                    Integer.class, tableName, indexName);
            if (count == null || count == 0) {
                jdbcTemplate.execute(sql);
            }
        } catch (Exception e) {
            log.warn("Self-hosted CDN index initialization failed: {}.{}: {}",
                    tableName, indexName, e.getMessage());
        }
    }
}
