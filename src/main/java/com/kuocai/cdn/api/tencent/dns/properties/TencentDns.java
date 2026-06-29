package com.kuocai.cdn.api.tencent.dns.properties;

import com.kuocai.cdn.util.RuntimeConfigUtils;

/**
 * @author xiaobo
 * @date 2023/3/6
 */
public class TencentDns {

    private TencentDns() {
    }


    /**
     * 密匙ID
     */
    public static String SECRET_ID = RuntimeConfigUtils.optional("tencent.dns.secret-id", "TENCENT_DNS_SECRET_ID", "");

    /**
     * 密匙Key
     */
    public static String SECRET_KEY = RuntimeConfigUtils.optional("tencent.dns.secret-key", "TENCENT_DNS_SECRET_KEY", "");

    /**
     * 请求接口域名
     */
    public static final String END_POINT = "dnspod.tencentcloudapi.com";

    /**
     * API版本号
     */
    public static final String API_VERSION = "2021-03-23";

    /**
     * 记录类型——>因为我们都是根据cname进行配置的所以在createRecordVo的构造方法中默认赋值了
     */
    public static final String RECORD_TYPE = "CNAME";

    /**
     * 记录行 上同RECORD_TYPE
     */
    public static final String RECORD_LINE = "默认";

    /**
     * 腾讯域名
     */
    public static String LOCAL_DOMAIN_NAME = RuntimeConfigUtils.optional("tencent.dns.local-domain-name", "TENCENT_DNS_LOCAL_DOMAIN", "kuocaidns.com");

    public static void applyConfiguration(String secretId, String secretKey, String localDomainName) {
        if (RuntimeConfigUtils.hasText(secretId)) {
            SECRET_ID = secretId.trim();
        }
        if (RuntimeConfigUtils.hasText(secretKey)) {
            SECRET_KEY = secretKey.trim();
        }
        if (RuntimeConfigUtils.hasText(localDomainName)) {
            LOCAL_DOMAIN_NAME = localDomainName.trim();
        }
    }

    public static String requiredSecretId() {
        if (RuntimeConfigUtils.hasText(SECRET_ID)) {
            return SECRET_ID;
        }
        return RuntimeConfigUtils.required("tencent.dns.secret-id", "TENCENT_DNS_SECRET_ID");
    }

    public static String requiredSecretKey() {
        if (RuntimeConfigUtils.hasText(SECRET_KEY)) {
            return SECRET_KEY;
        }
        return RuntimeConfigUtils.required("tencent.dns.secret-key", "TENCENT_DNS_SECRET_KEY");
    }
}
