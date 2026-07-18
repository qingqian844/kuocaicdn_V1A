package com.kuocai.cdn.template;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SetupTemplateTest {

    @Test
    void setupTemplateContainsAllEightStagesAndApiActions() throws Exception {
        String html = new String(Files.readAllBytes(Paths.get(
                "src/main/resources/templates/admin/setup.html")), StandardCharsets.UTF_8);

        for (int step = 1; step <= 8; step++) {
            assertTrue(html.contains("data-step=\"" + step + "\""));
        }
        assertTrue(html.contains("/api/setup/domain/verify"));
        assertTrue(html.contains("/api/setup/domain/apply"));
        assertTrue(html.contains("/api/setup/module/${name}/test"));
        assertTrue(html.contains("/api/setup/complete"));
    }
}
