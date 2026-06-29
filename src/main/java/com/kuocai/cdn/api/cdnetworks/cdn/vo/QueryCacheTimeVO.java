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
public class QueryCacheTimeVO implements IResponseVO {
    private String domainId;

    private String domainName;

    private CacheTimeBehavior[] cacheTimeBehaviors;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    public static class CacheTimeBehavior {
        private String dataId;
        private String pathPattern;
        private String exceptPathPattern;
        private String customPattern;
        private String filetype;
        private String customFileType;
        private String specifyUrlPattern;
        private String directory;
        private String cacheTtl;
        private String ignoreCacheControl;
        private String isRespectServer;
        private String ignoreLetterCase;
        private String reloadManage;
        private String ignoreAuthenticationHeader;
        private String priority;
    }

    public static QueryCacheTimeVO convert(CdnetworksHttp.Response response) throws CdnetworksException {
        int statusCode = response.getStatusCode();
        // String statusMessage = response.getStatusMessage();
        if (200 == statusCode) {
            return JSONObject.parseObject(response.getBody(), QueryCacheTimeVO.class);
        } else {
            DefaultErrorVO errorVO = DefaultErrorVO.convert(response);
            throw new CdnetworksException("获取域名 CacheTime 配置失败，错误信息：" + errorVO.getErrorMessages());
        }
    }
}
