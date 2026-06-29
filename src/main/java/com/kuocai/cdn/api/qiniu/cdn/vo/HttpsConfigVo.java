package com.kuocai.cdn.api.qiniu.cdn.vo;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HttpsConfigVo {

    /**
     * 证书id，从上传或者获取证书列表里拿到证书id
     */
    private String certId;

    /**
     * 是否强制https跳转
     */
    private Boolean forceHttps;

    /**
     * http2功能是否启用，false为关闭，true为开启
     */
    private Boolean http2Enable;
}
