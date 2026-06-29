package com.kuocai.cdn.util;

import cn.hutool.core.util.RandomUtil;

public class DomainUtil {
    public static String convertSubDomain(String domainName) {
        StringBuilder subDomain = new StringBuilder();
        // 泛解析域名
        if (domainName.startsWith("*.")) {
            subDomain.append(RandomUtil.randomString(8)).append(".");
            subDomain.append(domainName.substring(2));
        } else {
            subDomain.append(domainName);
        }
        subDomain.append(".");
        subDomain.append(RandomUtil.randomString(8));
        return subDomain.toString().toLowerCase();
    }
}
