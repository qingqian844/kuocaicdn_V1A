package com.kuocai.cdn.util;

import org.springframework.core.env.Environment;

public class RuntimeConfigUtils {

    private static volatile Environment environment;

    private RuntimeConfigUtils() {
    }

    public static void configure(Environment springEnvironment) {
        environment = springEnvironment;
    }

    public static String optional(String propertyName, String envName, String defaultValue) {
        String propertyValue = System.getProperty(propertyName);
        if (hasText(propertyValue)) {
            return propertyValue.trim();
        }
        Environment currentEnvironment = environment;
        if (currentEnvironment != null) {
            String environmentValue = currentEnvironment.getProperty(propertyName);
            if (hasText(environmentValue)) {
                return environmentValue.trim();
            }
        }
        String envValue = System.getenv(envName);
        if (hasText(envValue)) {
            return envValue.trim();
        }
        return defaultValue;
    }

    public static String required(String propertyName, String envName) {
        String value = optional(propertyName, envName, "");
        if (hasText(value)) {
            return value;
        }
        throw new IllegalStateException("Missing required runtime config: " + propertyName + " / " + envName);
    }

    public static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
