package com.kuocai.cdn.util;

import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

public class ThreadMdcUtils {
    private static void initTraceId(Map<String, String> context) {
        if (context == null) {
            MDC.clear();
        } else {
            MDC.setContextMap(context);
        }
        if (MDC.get(TraceIdUtil.KEY) == null) {
            String requestId = TraceIdUtil.generateTraceId();
            MDC.put(TraceIdUtil.KEY, requestId);
        }
    }

    public static Runnable wrapAsync(Runnable task, Map<String, String> context) {
        return () -> {
            initTraceId(context);
            try {
                task.run();
            } finally {
                MDC.clear();
            }
        };
    }

    public static <T> Callable<T> wrapAsync(Callable<T> task, Map<String, String> context) {
        return () -> {
            initTraceId(context);
            try {
                return task.call();
            } finally {
                MDC.clear();
            }
        };
    }

    public static <T> Supplier<T> wrapAsync(Supplier<T> supplier, Map<String, String> context) {
        // ?? 包装为Callable，然后再从Callable转换回Supplier
        return () -> {
            initTraceId(context);
            try {
                return supplier.get();
            } finally {
                MDC.clear();
            }
        };
    }
}
