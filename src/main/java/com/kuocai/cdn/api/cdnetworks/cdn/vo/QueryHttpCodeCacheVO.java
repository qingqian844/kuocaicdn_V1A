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
public class QueryHttpCodeCacheVO implements IResponseVO {
    private String domainId;

    private String domainName;

    private HttpCodeCacheRule[] httpCodeCacheRules;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    public static class HttpCodeCacheRule {
        private String dataId;
        private String cacheTtl;
        private String[] httpCodes;
    }

    public static QueryHttpCodeCacheVO convert(CdnetworksHttp.Response response) throws CdnetworksException {
        int statusCode = response.getStatusCode();
        // String statusMessage = response.getStatusMessage();
        if (200 == statusCode) {
            return JSONObject.parseObject(response.getBody(), QueryHttpCodeCacheVO.class);
        } else {
            DefaultErrorVO errorVO = DefaultErrorVO.convert(response);
            throw new CdnetworksException("获取域名 HttpCodeCache 配置失败，错误信息：" + errorVO.getErrorMessages());
        }
    }
}
