package com.kuocai.cdn.api.cdnetworks.cdn.vo;

import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.api.cdnetworks.cdn.CdnetworksHttp;
import com.kuocai.cdn.exception.CdnetworksException;
import lombok.*;

import java.util.Arrays;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class QueryInnerRedirectVO implements IResponseVO {
    private String domainId;

    private String domainName;

    private RewriteRuleSetting[] rewriteRuleSettings;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    public static class RewriteRuleSetting {
        private String dataId;
        private String pathPattern;
        private String exceptPathPattern;
        private String ignoreLetterCase;
        private String publishType;
        private String priority;
        private String beforeValue;
        private String afterValue;
        private String rewriteType;
        private String requestHeader;
        private String exceptionRequestHeader;
        private String customPattern;
        private String fileType;
        private String customFileType;
        private String directory;
        private SpecifyUrl specifyUrl;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    public static class SpecifyUrl {
        private String uri;
        private String matchQueryString;
        private String queryStrings;
        private String protocol;
    }

    public static boolean isRewriteToHttps(RewriteRuleSetting ruleSetting) {
        List<String> check = Arrays.asList("301:https://$1", "302:https://$1");
        return "before".equals(ruleSetting.getRewriteType()) && check.contains(ruleSetting.getAfterValue()) && "^http://([^/]+/.*)".equals(ruleSetting.getBeforeValue());
    }

    public static boolean isRewriteToHttp(RewriteRuleSetting ruleSetting) {
        List<String> check = Arrays.asList("301:http://$1", "302:http://$1");
        return "before".equals(ruleSetting.getRewriteType()) && check.contains(ruleSetting.getAfterValue()) && "^https://([^/]+/.*)".equals(ruleSetting.getBeforeValue());
    }

    public static QueryInnerRedirectVO convert(CdnetworksHttp.Response response) throws CdnetworksException {
        int statusCode = response.getStatusCode();
        // String statusMessage = response.getStatusMessage();
        if (200 == statusCode) {
            return JSONObject.parseObject(response.getBody(), QueryInnerRedirectVO.class);
        } else {
            DefaultErrorVO errorVO = DefaultErrorVO.convert(response);
            throw new CdnetworksException("查询域名 InnerRedirect 配置失败，错误信息：" + errorVO.getErrorMessages());
        }
    }
}
