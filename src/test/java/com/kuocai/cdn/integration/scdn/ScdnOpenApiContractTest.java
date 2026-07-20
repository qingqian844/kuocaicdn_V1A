package com.kuocai.cdn.integration.scdn;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScdnOpenApiContractTest {

    @Test
    void contractPublishesEveryV1EndpointAndRequiredHeaders() throws IOException {
        Path contract = Paths.get("docs", "openapi", "scdn-platform-v1.yaml");
        try (InputStream input = Files.newInputStream(contract)) {
            Map<String, Object> document = new Yaml().load(input);
            assertEquals("3.0.3", document.get("openapi"));

            Map<String, Object> paths = map(document.get("paths"));
            assertEquals(6, paths.size());
            assertTrue(paths.containsKey("/internal/scdn/v1/sso/exchange"));
            assertTrue(paths.containsKey("/internal/scdn/v1/users/{userId}/eligibility"));
            assertTrue(paths.containsKey("/internal/scdn/v1/orders"));
            assertTrue(paths.containsKey("/internal/scdn/v1/orders/{externalOrderId}"));
            assertTrue(paths.containsKey("/internal/scdn/v1/wallet/debits"));
            assertTrue(paths.containsKey("/internal/scdn/v1/wallet/refunds"));

            Map<String, Object> components = map(document.get("components"));
            Map<String, Object> parameters = map(components.get("parameters"));
            assertEquals("Idempotency-Key", map(parameters.get("IdempotencyKey")).get("name"));
            Map<String, Object> schemes = map(components.get("securitySchemes"));
            assertEquals("X-Scdn-Internal-Token",
                    map(schemes.get("scdnInternalToken")).get("name"));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        assertNotNull(value);
        return (Map<String, Object>) value;
    }
}
