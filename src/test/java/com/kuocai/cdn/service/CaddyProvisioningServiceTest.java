package com.kuocai.cdn.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CaddyProvisioningServiceTest {

    @Test
    void generatedConfigServesDomainAppAndMinioBucket() {
        CaddyProvisioningService service = new CaddyProvisioningService(
                "http://caddy:2019/", "app:8000", "http://minio:9000/", "uploads");

        String caddyfile = service.buildCaddyfile("cdn.example.com");

        assertTrue(caddyfile.contains("cdn.example.com {"));
        assertTrue(caddyfile.contains("handle /uploads/*"));
        assertTrue(caddyfile.contains("reverse_proxy http://minio:9000"));
        assertTrue(caddyfile.contains("reverse_proxy app:8000"));
        assertTrue(caddyfile.contains(":80 {"));
    }
}
