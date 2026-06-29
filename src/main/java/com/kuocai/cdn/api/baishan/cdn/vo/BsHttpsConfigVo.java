package com.kuocai.cdn.api.baishan.cdn.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BsHttpsConfigVo {

    private String domains;

    private String token;

    private BsHttpsConfigVo.BsHttpsConfigInner config;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class BsHttpsConfigInner {

        private BsHttpsConfigVo.BsHttpsVo https;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class BsHttpsVo {

        /**
         * 指定绑定的证书ID,可以通过证书查询接口获取
         * 当cert_id=0时，将为域名解除https服务。
         */
        private Integer cert_id;

        /**
         * http2功能on:开启
         * off :关闭
         */
        private String http2;

        /**
         * 请求http跳转为https协议0 :不跳转
         * 302 :http请求302成https请求301 :http请求301成https请求不填充时，默认值为0。
         */
        private String force_https;
    }
}
