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
public class QueryHttp2SettingsVO implements IResponseVO {
    private String code;

    private String message;

    private Http2SettingsData data;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    public static class Http2SettingsData {
        private String domainId;
        private String domainName;
        private Http2Settings http2Settings;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    public static class Http2Settings {
        private String enableHttp2;
        private String backToOriginProtocol;
    }

    public static QueryHttp2SettingsVO convert(CdnetworksHttp.Response response) throws CdnetworksException {
        int statusCode = response.getStatusCode();
        // String statusMessage = response.getStatusMessage();
        if (200 == statusCode) {
            return JSONObject.parseObject(response.getBody(), QueryHttp2SettingsVO.class);
        } else {
            DefaultErrorVO errorVO = DefaultErrorVO.convert(response);
            throw new CdnetworksException("获取域名 Http2 配置失败，错误信息：" + errorVO.getErrorMessages());
        }
    }
}
