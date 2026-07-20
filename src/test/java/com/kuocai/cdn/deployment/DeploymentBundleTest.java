package com.kuocai.cdn.deployment;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeploymentBundleTest {

    @Test
    void schemaTargetsTheDatabaseSelectedByInstaller() throws Exception {
        String sql = read("deploy/sql/KuocaiCDN-empty-install.sql");
        String installer = read("deploy/install.sh");

        assertFalse(sql.matches("(?s).*\\bCREATE\\s+DATABASE\\b.*"));
        assertFalse(sql.matches("(?s).*\\bUSE\\s+`?kuocai_cdn`?.*"));
        assertTrue(sql.contains("`failure_reason` varchar(1000)"));
        assertTrue(installer.contains("SCHEMA_IMPORT_MARKER"));
        assertTrue(installer.contains("created_by_installer"));
        assertTrue(installer.contains("历史数据库没有首次安装状态"));
    }

    @Test
    void composeUsesPinnedImagesAndKeepsApplicationPortInternal() throws Exception {
        String compose = read("deploy/docker-compose.yml");
        Map<String, Object> root = new Yaml().load(compose);
        Map<String, Map<String, Object>> services = (Map<String, Map<String, Object>>) root.get("services");

        assertFalse(compose.contains(":latest"));
        assertEquals("eclipse-temurin:8-jre-jammy", services.get("app").get("image"));
        assertEquals("caddy:2.8.4-alpine", services.get("caddy").get("image"));
        assertEquals("mysql:8.0.40", services.get("mysql").get("image"));
        assertEquals("minio/minio:RELEASE.2024-06-13T22-53-53Z", services.get("minio").get("image"));
        assertEquals(Collections.singletonList("8000"), services.get("app").get("expose"));
        assertNull(services.get("app").get("ports"));
        assertEquals(7, services.size());
    }

    private String read(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
