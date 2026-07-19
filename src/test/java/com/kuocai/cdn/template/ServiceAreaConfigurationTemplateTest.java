package com.kuocai.cdn.template;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceAreaConfigurationTemplateTest {

    @Test
    void websiteSettingsContainsRouteAreaMatrix() throws IOException {
        String template = read("src/main/resources/templates/admin/settings/website-setting.html");

        assertTrue(template.contains("overseasEnabledRoute"));
        assertTrue(template.contains("globalEnabledRoute"));
        assertTrue(template.contains("加速区域线路"));
    }

    @Test
    void domainCreateUsesGlobalRouteAreaFlags() throws IOException {
        String template = read("src/main/resources/templates/admin/domain/domain-create.html");

        assertTrue(template.contains("${allowOverseas}"));
        assertTrue(template.contains("${allowGlobal}"));
        assertFalse(template.contains("${enableOverseas"));
        assertFalse(template.contains("${enableGlobal"));
    }

    @Test
    void userProductConfigurationNoLongerContainsLegacyAreaSwitches() throws IOException {
        String template = read("src/main/resources/templates/admin/product/user-price-list.html");

        assertFalse(template.contains("name=\"enableOverseas\""));
        assertFalse(template.contains("name=\"enableGlobal\""));
        assertFalse(template.contains("row.user.enableOverseas"));
        assertFalse(template.contains("row.user.enableGlobal"));
        String columns = between(template, "columns: [", "columnDefs:");
        String headers = between(template, "<thead class=\"thead-light\">", "</thead>");
        assertEquals(11, count(columns, Pattern.compile("\\bdata\\s*:")));
        assertEquals(11, count(headers, Pattern.compile("<th(?:\\s|>)")));
    }

    private String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }

    private String between(String value, String start, String end) {
        int from = value.indexOf(start);
        int to = value.indexOf(end, from + start.length());
        return value.substring(from, to);
    }

    private int count(String value, Pattern pattern) {
        int total = 0;
        Matcher matcher = pattern.matcher(value);
        while (matcher.find()) {
            total++;
        }
        return total;
    }
}
