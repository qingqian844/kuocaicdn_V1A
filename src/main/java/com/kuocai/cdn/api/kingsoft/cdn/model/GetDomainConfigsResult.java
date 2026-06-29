package com.kuocai.cdn.api.kingsoft.cdn.model;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class GetDomainConfigsResult {

    @JSONField(name = "CacheRuleConfig")
    private CacheRuleConfig cacheRuleConfig;

    @JSONField(name = "IpProtectionConfig")
    private IpProtectionConfig ipProtectionConfig;

    @JSONField(name = "BackOriginHostConfig")
    private BackOriginHostConfig backOriginHostConfig;

    @JSONField(name = "ReferProtectionConfig")
    private ReferProtectionConfig referProtectionConfig;

    @JSONField(name = "PageCompressConfig")
    private PageCompressConfig pageCompressConfig;

    @JSONField(name = "BrCompressConfig")
    private PageCompressConfig brCompressConfig;

    @JSONField(name = "HttpHeadersConfig")
    private HttpHeadersConfig httpHeadersConfig;

    @JSONField(name = "CertificateConfig")
    private CertificateConfig certificateConfig;

    @JSONField(name = "ForceRedirectConfig")
    private ForceRedirectConfig forceRedirectConfig;

    @JSONField(name = "Http2OptionConfig")
    private Http2OptionConfig http2OptionConfig;

    @JSONField(name = "VideoSeekConfig")
    private VideoSeekConfig videoSeekConfig;

    @JSONField(name = "OriginAdvancedConfig")
    private OriginAdvancedConfig originAdvancedConfig;

    @JSONField(name = "TLSVersionConfig")
    private TLSVersionConfig tlsVersionConfig;

    @JSONField(name = "IgnoreQueryStringConfig")
    private IgnoreQueryStringConfig ignoreQueryStringConfig;

    @JSONField(name = "RequestAuthConfig")
    private RequestAuthConfig requestAuthConfig;

    @JSONField(name = "ErrorPageConfig")
    private ErrorPageConfig errorPageConfig;

    // --- 内部静态类定义 ---
    @Data public static class IpProtectionConfig {
        @JSONField(name = "Enable") private String enable;
        @JSONField(name = "IpType") private String ipType;
        @JSONField(name = "IpList") private String ipList;
    }
    @Data public static class BackOriginHostConfig {
        @JSONField(name = "BackOriginHost") private String backOriginHost;
    }
    @Data public static class ReferProtectionConfig {
        @JSONField(name = "Enable") private String enable;
        @JSONField(name = "ReferType") private String referType;
        @JSONField(name = "ReferList") private String referList;
        @JSONField(name = "AllowEmpty") private String allowEmpty;
    }
    @Data public static class PageCompressConfig {
        @JSONField(name = "Enable") private String enable;
    }
    @Data public static class CertificateConfig {
        @JSONField(name = "Enable") private String enable;
    }
    @Data public static class ForceRedirectConfig {
        @JSONField(name = "RedirectType") private String redirectType;
        @JSONField(name = "RedirectCode") private int redirectCode;
    }
    @Data public static class Http2OptionConfig {
        @JSONField(name = "Enable") private String enable;
    }
    @Data public static class VideoSeekConfig {
        @JSONField(name = "Enable") private String enable;
    }
    @Data
    public static class OriginAdvancedConfig {
        @JSONField(name = "Enable")
        private String enable;

        @JSONField(name = "Origin")
        private String origin;

        @JSONField(name = "OriginType")
        private String originType;

        @JSONField(name = "BackupOrigin")
        private String backupOrigin;

        @JSONField(name = "BackupOriginType")
        private String backupOriginType;

        @JSONField(name = "OriginPolicy")
        private String originPolicy;

        @JSONField(name = "OriginPolicyBestCount")
        private Integer originPolicyBestCount;

        @JSONField(name = "OriginReadTimeout")
        private Integer originReadTimeout;

    }
    @Data public static class TLSVersionConfig {
        @JSONField(name = "TLSVersion") private List<String> tlsVersion;
    }

    @Data
    public static class IgnoreQueryStringConfig {
        @JSONField(name = "Enable")
        private String enable;
        @JSONField(name = "Type")
        private String type;
        @JSONField(name = "HashKeyArgs")
        private String hashKeyArgs;
    }

    @Data
    public static class RequestAuthConfig {
        private String Enable;
        private String AuthType;
        private String Key1;
        private String Key2;
        private Long ExpirationTime;
    }

    @Data
    public static class ErrorPageConfig {
        @JSONField(name = "ErrorPages")
        private List<ErrorPage> errorPages;
    }

    @Data
    public static class ErrorPage {
        @JSONField(name = "ErrorHttpCode")
        private String errorHttpCode;
        
        @JSONField(name = "CustomPageUrl")
        private String customPageUrl;
    }
}