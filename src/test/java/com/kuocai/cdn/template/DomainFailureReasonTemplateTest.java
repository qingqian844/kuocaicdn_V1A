package com.kuocai.cdn.template;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainFailureReasonTemplateTest {

    @Test
    void userAndAdminDomainListsRenderEscapedFailureReasons() throws Exception {
        assertFailureReasonSupport(read("src/main/resources/templates/user/domain/domain-list.html"));
        assertFailureReasonSupport(read("src/main/resources/templates/admin/domain/domain-list.html"));
    }

    private void assertFailureReasonSupport(String template) {
        assertTrue(template.contains("row.failureReason"));
        assertTrue(template.contains("escapeDomainFailureText"));
        assertTrue(template.contains("未备案域名请使用海外加速"));
        assertTrue(template.contains("row.domainStatus !== 'configure_failed'"));
    }

    private String read(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
