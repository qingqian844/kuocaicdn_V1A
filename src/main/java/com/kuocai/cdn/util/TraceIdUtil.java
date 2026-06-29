package com.kuocai.cdn.util;

import java.util.UUID;

public class TraceIdUtil {
    public static final String KEY = "requestId";
    public static final String HEADER_NAME = "X-Request-Id";

    public static String generateTraceId() {
        return UUID.randomUUID().toString();
    }
}
