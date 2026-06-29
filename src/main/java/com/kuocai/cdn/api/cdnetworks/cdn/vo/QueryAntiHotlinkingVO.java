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
public class QueryAntiHotlinkingVO implements IResponseVO {
    private String domainId;

    private String domainName;

    private VisitControlRule[] visitControlRules;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    public static class VisitControlRule {
        private String dataId;
        private String pathPattern;
        private String exceptPathPattern;
        private String customPattern;
        private String fileType;
        private String customFileType;
        private String specifyUrlPattern;
        private String directory;
        private String exceptFileType;
        private String exceptCustomFileType;
        private String exceptDirectory;
        private String controlAction;
        private String priority;
        private String rewriteTo;
        private VisitControlRuleIpControlRule ipControlRule;
        private VisitControlRuleRefererControlRule refererControlRule;
        private VisitControlRuleUaControlRule uaControlRule;
        private VisitControlRuleAdvanceControlRules advanceControlRules;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    public static class VisitControlRuleIpControlRule {
        private String forbiddenIps;
        private String allowedIps;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    public static class VisitControlRuleRefererControlRule {
        private String allowNullReferer;
        private String validReferer;
        private String validUrl;
        private String validDomain;
        private String invalidReferer;
        private String invalidUrl;
        private String invalidDomain;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    public static class VisitControlRuleUaControlRule {
        private String validUserAgents;
        private String invalidUserAgents;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    public static class VisitControlRuleAdvanceControlRules {
        private String invalidTime;
        private String invalidVisitorRegion;
        private String validVisitorRegion;
    }

    public static QueryAntiHotlinkingVO convert(CdnetworksHttp.Response response) throws CdnetworksException {
        int statusCode = response.getStatusCode();
        // String statusMessage = response.getStatusMessage();
        if (200 == statusCode) {
            return JSONObject.parseObject(response.getBody(), QueryAntiHotlinkingVO.class);
        } else {
            DefaultErrorVO errorVO = DefaultErrorVO.convert(response);
            throw new CdnetworksException("获取域名 AntiHotlinking 配置失败，错误信息：" + errorVO.getErrorMessages());
        }
    }
}
