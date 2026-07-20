package com.kuocai.cdn.util;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EdgeOneFailureReasonFormatter {

    static final int MAX_REASON_LENGTH = 1000;

    private static final String ICP_REASON =
            "域名未完成 ICP 备案，请使用海外加速；如刚完成备案，请等待备案信息同步后重试。";
    private static final String ACCOUNT_OCCUPIED_REASON =
            "域名已被腾讯云 EdgeOne 其他账号或现有站点占用，请先从原账号/站点删除，或完成域名取回后重试。";
    private static final String UNAUTHORIZED_REASON =
            "当前腾讯云 EdgeOne 厂商账号没有创建加速域名权限，请检查 CAM 的 teo:CreateAccelerationDomain 权限、资源范围及条件。";
    private static final Pattern ERROR_CODE_PATTERN = Pattern.compile(
            "(?:错误代码\\s*[:：]\\s*)?([A-Z][A-Za-z0-9]*(?:\\.[A-Za-z0-9_-]+)+)(?=\\s*[:：])");
    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile(
            "(?i)RequestId\\s*[:：]\\s*([A-Za-z0-9-]+)");

    private EdgeOneFailureReasonFormatter() {
    }

    public static String format(String rawReason, String errorCode, String requestId, String serviceArea) {
        String reason = clean(rawReason);
        String code = clean(errorCode);
        String request = clean(requestId);
        if (code.isEmpty()) {
            code = extract(ERROR_CODE_PATTERN, reason);
        }
        if (request.isEmpty()) {
            request = extract(REQUEST_ID_PATTERN, reason);
        }
        String searchable = (code + " " + reason).toLowerCase(Locale.ROOT);

        if (isIcpFailure(searchable, reason)) {
            return withDiagnostics(ICP_REASON, code, request);
        }
        if (isAccountOccupied(searchable, reason)) {
            return withDiagnostics(ACCOUNT_OCCUPIED_REASON, code, request);
        }
        if (isUnauthorized(searchable, reason)) {
            return withDiagnostics(UNAUTHORIZED_REASON, code, request);
        }
        if (reason.isEmpty()) {
            return defaultReason(serviceArea);
        }

        String display = reason.contains("EdgeOne") || reason.startsWith("腾讯云")
                ? reason : "腾讯云 EdgeOne 配置失败：" + reason;
        return withDiagnostics(display, code, request);
    }

    public static String defaultReason(String serviceArea) {
        if ("outside_mainland_china".equals(serviceArea)) {
            return "腾讯云 EdgeOne 配置失败。请确认域名未绑定在其他 EdgeOne 账号，并重新提交以获取最新错误原因。";
        }
        return "腾讯云 EdgeOne 配置失败。请检查域名备案状态；未备案域名请改用海外加速。若已备案，请确认域名未绑定在其他 EdgeOne 账号。";
    }

    public static String stalePendingReason(String serviceArea) {
        String reason = "腾讯云 EdgeOne 创建未完成，上游长时间未查询到该域名。";
        if (!"outside_mainland_china".equals(serviceArea)) {
            reason += "请确认域名已完成 ICP 备案；";
        }
        return truncate(reason + "请同时确认域名未绑定在其他 EdgeOne 账号，然后重试。", MAX_REASON_LENGTH);
    }

    private static boolean isIcpFailure(String searchable, String original) {
        return searchable.contains("icp")
                || searchable.contains("noicp")
                || searchable.contains("beian")
                || searchable.contains("domainnotregistered")
                || searchable.contains("not filed")
                || searchable.contains("not registered")
                || original.contains("备案")
                || original.contains("未备案")
                || original.contains("没有备案")
                || original.contains("无备案")
                || original.contains("备案信息不存在")
                || original.contains("备案校验失败");
    }

    private static boolean isAccountOccupied(String searchable, String original) {
        return searchable.contains("resourceinuse")
                || searchable.contains("domainalreadyexists")
                || searchable.contains("domainnameisexist")
                || searchable.contains("already exists")
                || searchable.contains("already used")
                || searchable.contains("already bound")
                || searchable.contains("already added")
                || searchable.contains("used by other")
                || searchable.contains("owned by other")
                || searchable.contains("belongs to")
                || searchable.contains("other account")
                || searchable.contains("another account")
                || original.contains("其他账号")
                || original.contains("其它账号")
                || original.contains("已被接入")
                || original.contains("已接入")
                || original.contains("其他站点")
                || original.contains("其它站点")
                || original.contains("已被占用")
                || original.contains("已绑定")
                || original.contains("域名已存在");
    }

    private static boolean isUnauthorized(String searchable, String original) {
        return searchable.contains("unauthorizedoperation")
                || searchable.contains("camunauthorized")
                || searchable.contains("accessdenied")
                || searchable.contains("not authorized")
                || original.contains("没有权限")
                || original.contains("无权限");
    }

    private static String withDiagnostics(String reason, String errorCode, String requestId) {
        StringBuilder result = new StringBuilder(clean(reason));
        if (!errorCode.isEmpty() && !containsIgnoreCase(result.toString(), errorCode)) {
            result.append(" 错误代码：").append(errorCode).append('。');
        }
        if (!requestId.isEmpty() && !containsIgnoreCase(result.toString(), requestId)) {
            result.append(" RequestId：").append(requestId).append('。');
        }
        return truncate(result.toString(), MAX_REASON_LENGTH);
    }

    private static boolean containsIgnoreCase(String text, String value) {
        return text.toLowerCase(Locale.ROOT).contains(value.toLowerCase(Locale.ROOT));
    }

    private static String extract(Pattern pattern, String value) {
        Matcher matcher = pattern.matcher(value);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 1) + "…";
    }
}
