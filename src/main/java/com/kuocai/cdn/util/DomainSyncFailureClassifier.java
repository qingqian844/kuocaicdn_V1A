package com.kuocai.cdn.util;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class DomainSyncFailureClassifier {

    private DomainSyncFailureClassifier() {
    }

    public static boolean isTerminalMissingDomain(Throwable error) {
        String details = collectMessages(error).toLowerCase(Locale.ROOT);
        return details.contains("invalid domain")
                || details.contains("nosuchdomain")
                || details.contains("does not have this domain name")
                || details.contains("invaliddomain.notfound")
                || details.contains("domainnotfound")
                || details.contains("在金山云平台未找到域名")
                || details.contains("账号下无此域名")
                || details.contains("域名不存在")
                || details.contains("没有找到域名信息");
    }

    public static String failureReason(String route) {
        if ("kingsoft".equals(route)) {
            return "金山云中未找到该域名，请联系管理员处理";
        }
        if ("baidu".equals(route)) {
            return "百度云中未找到该域名或域名已失效，请联系管理员处理";
        }
        return "供应商中未找到该域名，请联系管理员处理";
    }

    private static String collectMessages(Throwable error) {
        StringBuilder messages = new StringBuilder();
        Set<Throwable> visited = new HashSet<>();
        Throwable current = error;
        while (current != null && visited.add(current)) {
            if (current.getMessage() != null) {
                messages.append('\n').append(current.getMessage());
            }
            current = current.getCause();
        }
        return messages.toString();
    }
}
