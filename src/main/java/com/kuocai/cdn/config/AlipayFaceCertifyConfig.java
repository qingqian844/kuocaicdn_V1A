package com.kuocai.cdn.config;

import com.kuocai.cdn.api.aliyun.faceCertifyVerify.FaceCertifyVerifyApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AlipayFaceCertifyConfig {

    public AlipayFaceCertifyConfig(@Value("${alipay.face.server-url:}") String serverUrl,
                                   @Value("${alipay.face.app-id:}") String appId,
                                   @Value("${alipay.face.private-key:}") String privateKey,
                                   @Value("${alipay.face.public-key:}") String publicKey) {
        FaceCertifyVerifyApi.configure(serverUrl, appId, privateKey, publicKey);
    }
}
