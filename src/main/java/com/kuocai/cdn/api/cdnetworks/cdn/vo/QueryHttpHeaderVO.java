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
public class QueryHttpHeaderVO implements IResponseVO {

    private String domainId;

    private String domainName;

    private HeaderModifyRule[] headerModifyRules;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    public static class HeaderModifyRule {
        private String dataId;
        private String pathPattern;
        private String exceptPathPattern;
        private String customPattern;
        private String fileType;
        private String customFileType;
        private String directory;
        private String specifyUrl;
        private String requestMethod;
        private String headerDirection;
        private String action;
        private String allowRegexp;
        private String headerName;
        private String headerValue;
        private String requestHeader;
        private String priority;
        private String exceptRequestMethod;
        private String exceptRequestHeader;
        private String exceptDirectory;
        private String exceptFileType;
    }

    public static QueryHttpHeaderVO convert(CdnetworksHttp.Response response) throws CdnetworksException {
        int statusCode = response.getStatusCode();
        // String statusMessage = response.getStatusMessage();
        if (200 == statusCode) {
            return JSONObject.parseObject(response.getBody(), QueryHttpHeaderVO.class);
        } else {
            DefaultErrorVO errorVO = DefaultErrorVO.convert(response);
            throw new CdnetworksException("获取域名 HttpHeader 配置失败，错误信息：" + errorVO.getErrorMessages());
        }
    }
}
