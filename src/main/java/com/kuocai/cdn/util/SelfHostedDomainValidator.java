package com.kuocai.cdn.util;

import com.kuocai.cdn.exception.BusinessException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Validates acceleration domain names before they are sent to self-hosted nodes.
 */
public final class SelfHostedDomainValidator {
    private static final Pattern LABEL_PATTERN =
            Pattern.compile("^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$");

    private SelfHostedDomainValidator() {
    }

    public static String validateAndNormalize(String domainName) throws BusinessException {
        if (domainName == null) {
            throw new BusinessException("自建 CDN 加速域名不能为空");
        }
        String normalized = domainName.trim().toLowerCase(Locale.ROOT);
        boolean wildcard = normalized.startsWith("*.");
        String host = wildcard ? normalized.substring(2) : normalized;
        if (host.isEmpty() || host.length() > 253 || host.startsWith(".") || host.endsWith(".")) {
            throw invalidDomain(domainName);
        }
        String[] labels = host.split("\\.", -1);
        if (labels.length < 2) {
            throw invalidDomain(domainName);
        }
        for (String label : labels) {
            if (!LABEL_PATTERN.matcher(label).matches()) {
                throw invalidDomain(domainName);
            }
        }
        return wildcard ? "*." + host : host;
    }

    /**
     * Nginx treats a leading dot as the safe equivalent of a wildcard server name.
     * This representation is also understood by already-installed legacy agents.
     */
    public static String toAgentServerName(String normalizedDomainName) {
        return normalizedDomainName.startsWith("*.")
                ? "." + normalizedDomainName.substring(2) : normalizedDomainName;
    }

    public static String defaultOriginHost(String normalizedDomainName) {
        return normalizedDomainName.startsWith("*.")
                ? normalizedDomainName.substring(2) : normalizedDomainName;
    }

    private static BusinessException invalidDomain(String domainName) {
        return new BusinessException("自建 CDN 加速域名格式不正确：" + String.valueOf(domainName));
    }
}
