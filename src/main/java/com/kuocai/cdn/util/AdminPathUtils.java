package com.kuocai.cdn.util;

import com.kuocai.cdn.config.SystemConfig;
import com.kuocai.cdn.exception.BusinessException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class AdminPathUtils {
    public static final String DEFAULT_PATH = "kuocaiadmin";

    private static final Pattern PATH_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9_-]{2,31}$");
    private static final Set<String> RESERVED_PATHS = new HashSet<>(Arrays.asList(
            "api", "login", "logout", "health", "setup", "index", "user-login", "register",
            "register-email", "forget", "dashboard", "website-setting", "license-error",
            "license-issuer", "common", "front", "image", "alipay", "wechat", "sysconfig",
            "sysuser", "cdndomain", "purchasedflow", "vendoraccount"));

    private AdminPathUtils() {
    }

    public static String normalize(String value) throws BusinessException {
        String path = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (path.isEmpty()) {
            return DEFAULT_PATH;
        }
        if (!PATH_PATTERN.matcher(path).matches()) {
            throw new BusinessException("后台地址后缀必须为3-32位小写字母、数字、短横线或下划线");
        }
        if (!DEFAULT_PATH.equals(path) && RESERVED_PATHS.contains(path)) {
            throw new BusinessException("后台地址后缀与系统已有地址冲突，请更换");
        }
        return path;
    }

    public static String configuredPath() {
        String configured = SystemConfig.websiteBaseConfig == null
                ? null : SystemConfig.websiteBaseConfig.getAdminPath();
        try {
            return normalize(configured);
        } catch (BusinessException ignored) {
            return DEFAULT_PATH;
        }
    }

    public static boolean isConfiguredRequestPath(String requestUri) {
        if (requestUri == null) {
            return false;
        }
        String path = requestUri;
        int queryIndex = path.indexOf('?');
        if (queryIndex >= 0) {
            path = path.substring(0, queryIndex);
        }
        if (!path.startsWith("/") || path.indexOf('/', 1) >= 0) {
            return false;
        }
        return path.substring(1).equals(configuredPath());
    }
}
