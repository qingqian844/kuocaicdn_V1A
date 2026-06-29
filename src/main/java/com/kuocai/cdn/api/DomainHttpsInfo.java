package com.kuocai.cdn.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DomainHttpsInfo {
    private HttpGetBody https;

    private ForceRedirect force_redirect;


    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HttpGetBody {
        /**
         * HTTPS证书是否启用。（on：开启，off：关闭）
         */
        private String https_status;
        /**
         * 证书名字。（长度限制为3-32字符）。当证书开启时必返回该字段。
         */
        private String certificate_name;
        /**
         * HTTPS协议使用的证书内容，当证书开启时必返回该字段。取值范围：PEM编码格式。
         */
        private String certificate_value;
        /**
         * 证书过期时间，单位：毫秒。
         */
        private Long expire_time;
        /**
         * 证书来源。1：代表华为云托管证书；0：表示自有证书。 默认值0。当证书开启时必返回该字段。
         */
        private Integer certificate_source;
        /**
         * 证书类型。server：国际证书；server_sm：国密证书。
         */
        private String certificate_type;
        /**
         * 是否使用HTTP2.0。（on：是，off：否）
         */
        private String http2_status;
        /**
         * 传输层安全性协议，目前支持TLSv1.0/1.1/1.2/1.3四个版本的协议。当证书开启时返回该字段，默认开启TLSv1.1/1.2/1.3，不可全部关闭。
         */
        private String tls_version;
        /**
         * 是否开启ocsp stapling （on：是，off：否）。
         */
        private String ocsp_stapling_status;

        /**
         * 白山云使用
         */
        private Integer certId;


    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ForceRedirect {
        /**
         * 强制跳转开关（on：打开, off：关闭）。
         */
        private String status;
        /**
         * 强制跳转类型（http：强制跳转HTTP，https：强制跳转HTTPS）。
         */
        private String type;

        /**
         * 重定向跳转码301,302。
         */
        private String redirect_code;
        
        /**
         * 强制跳转类型（与type字段相同，用于匹配API返回的RedirectType字段）
         */
        private String redirectType;
        
        /**
         * 重定向跳转码（与redirect_code字段相同，用于匹配API返回的RedirectCode字段）
         */
        private Integer redirectCode;
    }
}
