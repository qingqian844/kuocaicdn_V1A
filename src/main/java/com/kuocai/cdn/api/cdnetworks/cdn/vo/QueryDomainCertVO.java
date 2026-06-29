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
public class QueryDomainCertVO implements IResponseVO {
    private String code;

    private String message;

    private CertData data;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @ToString
    public static class CertData {
        private String domainId;
        private String domainName;
        private String certificateId;
        private String cipherSuites;
        private String TLSVersion;
        private String enableOCSP;
    }

    public static QueryDomainCertVO convert(CdnetworksHttp.Response response) throws CdnetworksException {
        int statusCode = response.getStatusCode();
        // String statusMessage = response.getStatusMessage();
        if (200 == statusCode) {
            return JSONObject.parseObject(response.getBody(), QueryDomainCertVO.class);
        } else {
            DefaultErrorVO errorVO = DefaultErrorVO.convert(response);
            throw new CdnetworksException("获取域名 DomainCert 配置失败，错误信息：" + errorVO.getErrorMessages());
        }
    }
}
