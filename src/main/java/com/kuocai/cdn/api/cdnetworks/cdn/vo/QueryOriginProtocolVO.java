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
public class QueryOriginProtocolVO {
    private String code;

    private String message;

    private OriginProtocolData data;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    public static class OriginProtocolData {
        private String domainId;
        private String domainName;
        private BackToOriginRewriteRule backToOriginRewriteRule;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    public static class BackToOriginRewriteRule {
        private String protocol;
        private String port;
    }

    public static QueryOriginProtocolVO convert(CdnetworksHttp.Response response) throws CdnetworksException {
        int statusCode = response.getStatusCode();
        String statusMessage = response.getStatusMessage();
        if (200 == statusCode) {
            return JSONObject.parseObject(response.getBody(), QueryOriginProtocolVO.class);
        } else {
            DefaultErrorVO errorVO = DefaultErrorVO.convert(response);
            throw new CdnetworksException("查询域名 OriginProtocol 配置失败，错误信息：" + errorVO.getErrorMessages());
        }
    }
}
