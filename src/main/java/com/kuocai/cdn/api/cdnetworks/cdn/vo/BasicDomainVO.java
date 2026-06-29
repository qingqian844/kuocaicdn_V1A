package com.kuocai.cdn.api.cdnetworks.cdn.vo;

import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.api.cdnetworks.cdn.CdnetworksHttp;
import com.kuocai.cdn.exception.CdnetworksException;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class BasicDomainVO implements IResponseVO {

    private String domainId;

    private String domainName;

    private String cname;

    private String serviceType;

    private String status;

    private String cdnServiceStatus;

    private String createdDate;

    private String lastModified;

    private String enabled;

    private String contractId;

    private String itemId;

    private String comment;

    private String cacheHost;

    private OriginConfig originConfig;

    private WafConfig wafConfig;

    private String headerOfClientip;

    private Ssl ssl;

    private CacheBehavior[] cacheBehaviors;

    private VisitControlRule[] visitControlRules;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    public static class OriginConfig {
        private String originIps;
        private String defaultOriginHostHeader;
        private String originPort;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    public static class WafConfig {
        private String wafEnable;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    public static class Ssl {
        private String useSsl;
        private String useForSni;
        private String sslCertificateId;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    public static class CacheBehavior {
        private String pathPattern;
        private String ignoreCacheControl;
        private String cacheTtl;
        private String cacheUnit;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    public static class VisitControlRule {
        private String pathPattern;
        private String allownullreferer;
        private VisitControlReferer validReferers;
        private VisitControlReferer invalidReferers;
        private String forbiddenIps;
        private String allowedIps;
        private String forbiddenUas;
        private String allowedUas;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    public static class VisitControlReferer {
        private String[] referer;
    }

    public static BasicDomainVO convert(CdnetworksHttp.Response response) throws CdnetworksException {
        int statusCode = response.getStatusCode();
        // String statusMessage = response.getStatusMessage();
        if (200 == statusCode) {
            return JSONObject.parseObject(response.getBody(), BasicDomainVO.class);
        } else {
            DefaultErrorVO errorVO = DefaultErrorVO.convert(response);
            throw new CdnetworksException("获取域名配置失败，错误信息：" + errorVO.getErrorMessages());
        }
    }
}
