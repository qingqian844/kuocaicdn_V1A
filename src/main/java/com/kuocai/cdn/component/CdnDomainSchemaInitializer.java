package com.kuocai.cdn.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
public class CdnDomainSchemaInitializer {

    private static final String LEGACY_EDGEONE_FAILURE_REASON =
            "腾讯云 EdgeOne 配置失败。请检查域名备案状态；未备案域名请改用海外加速。若已备案，请确认域名未绑定在其他 EdgeOne 账号。";
    private static final String LEGACY_EDGEONE_OVERSEAS_FAILURE_REASON =
            "腾讯云 EdgeOne 配置失败。请确认域名未绑定在其他 EdgeOne 账号，并重新提交以获取最新错误原因。";

    private final JdbcTemplate jdbcTemplate;

    public CdnDomainSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        execute("ALTER TABLE cdn_domain ADD COLUMN failure_reason VARCHAR(1000) NULL");
        execute("UPDATE cdn_domain SET failure_reason = CASE " +
                "WHEN service_area = 'outside_mainland_china' THEN '" + LEGACY_EDGEONE_OVERSEAS_FAILURE_REASON + "' " +
                "ELSE '" + LEGACY_EDGEONE_FAILURE_REASON + "' END " +
                "WHERE route = 'tencent_edgeone' AND domain_status = 'configure_failed' " +
                "AND (failure_reason IS NULL OR failure_reason = '')");
        execute("UPDATE cdn_domain SET failure_reason = '" + LEGACY_EDGEONE_OVERSEAS_FAILURE_REASON + "' " +
                "WHERE route = 'tencent_edgeone' AND domain_status = 'configure_failed' " +
                "AND service_area = 'outside_mainland_china' " +
                "AND failure_reason = '" + LEGACY_EDGEONE_FAILURE_REASON + "'");
    }

    private void execute(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            String message = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (!message.contains("duplicate column")) {
                log.warn("域名失败原因字段初始化失败：{}，原因：{}", sql, e.getMessage());
            }
        }
    }
}
