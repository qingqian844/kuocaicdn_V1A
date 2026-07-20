package com.kuocai.cdn.component;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class CdnDomainSchemaInitializerTest {

    @Test
    void legacyFailureBackfillDoesNotRequireIcpForOverseasDomains() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        CdnDomainSchemaInitializer initializer = new CdnDomainSchemaInitializer(jdbcTemplate);

        initializer.init();

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, times(3)).execute(sql.capture());
        List<String> statements = sql.getAllValues();
        assertTrue(statements.stream().anyMatch(value ->
                value.contains("service_area = 'outside_mainland_china'")
                        && value.contains("请确认域名未绑定在其他 EdgeOne 账号")
                        && value.contains("CASE")));
    }
}
