package com.kuocai.cdn.api.kingsoft.cdn.properties;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class KingsoftCdn {

    /**
     * 金山云Access Key
     */
    public static String AccessKey = "";

    /**
     * 金山云Secret Key
     */
    public static String SecretKey = "";

    /**
     * API端点地址
     */
    public static String Endpoint = "https://cdn.api.ksyun.com";

    /**
     * 区域
     */
    public static String Region = "cn-beijing-6";

    /**
     * 服务名称
     */
    public static String ServiceName = "cdn";

    public static void applyConfiguration(String accessKey, String secretKey, String endpoint, String region, String serviceName) {
        AccessKey = normalize(accessKey);
        SecretKey = normalize(secretKey);
        Endpoint = normalizeOrDefault(endpoint, Endpoint);
        Region = normalizeOrDefault(region, Region);
        ServiceName = normalizeOrDefault(serviceName, ServiceName);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeOrDefault(String value, String defaultValue) {
        String normalized = normalize(value);
        return normalized.isEmpty() ? defaultValue : normalized;
    }
    

}
