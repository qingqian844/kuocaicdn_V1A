package com.kuocai.cdn.util;

import com.google.common.net.InetAddresses;
import com.kuocai.cdn.exception.BusinessException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SelfHostedOriginValidator {
    private static final Pattern SEPARATOR_PATTERN = Pattern.compile("[,;，；]", Pattern.UNICODE_CASE);
    private static final Pattern HOST_LABEL_PATTERN = Pattern.compile(
            "^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$");
    private static final Pattern NUMERIC_DOTTED_PATTERN = Pattern.compile("^[0-9.]+$");

    private SelfHostedOriginValidator() {
    }

    public static String validateAndNormalize(String originType, String addresses) throws BusinessException {
        return validateAndNormalize(originType, addresses, "源站");
    }

    public static String validateAndNormalize(String originType, String addresses, String fieldName)
            throws BusinessException {
        String label = Assert.isEmpty(fieldName) ? "源站" : fieldName;
        String type = normalizeType(originType);
        if (Assert.isEmpty(addresses)) {
            throw new BusinessException(label + "地址不能为空");
        }
        String[] values = SEPARATOR_PATTERN.split(addresses.trim(), -1);
        List<String> normalized = new ArrayList<>();
        for (String value : values) {
            String address = value == null ? "" : value.trim();
            if (address.isEmpty()) {
                throw new BusinessException(label + "地址中存在空项，请删除多余的分隔符");
            }
            String effectiveType = type.isEmpty()
                    ? (InetAddresses.isInetAddress(stripIpv6Brackets(address)) ? "ipaddr" : "domain")
                    : type;
            if ("ipaddr".equals(effectiveType)) {
                if (!InetAddresses.isInetAddress(stripIpv6Brackets(address))) {
                    throw new BusinessException(label + " IP 地址格式不正确：" + address);
                }
                normalized.add(stripIpv6Brackets(address));
            } else if ("domain".equals(effectiveType)) {
                String hostname = normalizeHostname(address);
                if (!isValidHostname(hostname)) {
                    throw new BusinessException(label + "域名格式不正确：" + address);
                }
                normalized.add(hostname);
            } else {
                throw new BusinessException(label + "类型只支持 IP 源站或域名源站");
            }
        }
        return String.join(";", normalized);
    }

    private static String normalizeType(String originType) {
        if ("ip".equalsIgnoreCase(originType) || "ipaddr".equalsIgnoreCase(originType)) {
            return "ipaddr";
        }
        if ("domain".equalsIgnoreCase(originType)) {
            return "domain";
        }
        return originType == null ? "" : originType.trim().toLowerCase(Locale.ROOT);
    }

    private static String stripIpv6Brackets(String address) {
        if (address.length() > 2 && address.startsWith("[") && address.endsWith("]")) {
            return address.substring(1, address.length() - 1);
        }
        return address;
    }

    private static String normalizeHostname(String address) {
        String value = address.toLowerCase(Locale.ROOT);
        return value.endsWith(".") ? value.substring(0, value.length() - 1) : value;
    }

    private static boolean isValidHostname(String hostname) {
        if (hostname.isEmpty() || hostname.length() > 253 || InetAddresses.isInetAddress(hostname)
                || NUMERIC_DOTTED_PATTERN.matcher(hostname).matches()) {
            return false;
        }
        String[] labels = hostname.split("\\.", -1);
        for (String label : labels) {
            if (!HOST_LABEL_PATTERN.matcher(label).matches()) {
                return false;
            }
        }
        return true;
    }
}
