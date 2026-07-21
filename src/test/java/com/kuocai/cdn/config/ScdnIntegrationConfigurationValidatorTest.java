package com.kuocai.cdn.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScdnIntegrationConfigurationValidatorTest {

    @Test
    void disabledIntegrationDoesNotRequireProductionSecrets() {
        assertDoesNotThrow(() -> new ScdnIntegrationConfigurationValidator(
                new ScdnIntegrationProperties(), "", "").validate());
    }

    @Test
    void enabledIntegrationRejectsInsecureConsoleUrl() {
        ScdnIntegrationProperties properties = productionProperties();
        properties.setConsoleUrl("http://scdn.example.com");

        assertThrows(IllegalStateException.class,
                () -> new ScdnIntegrationConfigurationValidator(properties, "public-key", "private-key").validate());
    }

    @Test
    void productionSettingsPassValidation() {
        assertDoesNotThrow(() -> new ScdnIntegrationConfigurationValidator(
                productionProperties(), "public-key", "private-key").validate());
    }

    private ScdnIntegrationProperties productionProperties() {
        ScdnIntegrationProperties properties = new ScdnIntegrationProperties();
        properties.setEnabled(true);
        properties.setConsoleUrl("https://scdn.example.com");
        properties.setInternalToken("0123456789abcdef0123456789abcdef");
        return properties;
    }
}
