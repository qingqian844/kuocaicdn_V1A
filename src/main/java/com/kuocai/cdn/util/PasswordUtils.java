package com.kuocai.cdn.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordUtils {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    public static String hash(String password) {
        return ENCODER.encode(password);
    }

    public static boolean matches(String password, String hash) {
        return ENCODER.matches(password, hash);
    }

    public static boolean isBcryptHash(String value) {
        return value != null && (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$"));
    }
}
