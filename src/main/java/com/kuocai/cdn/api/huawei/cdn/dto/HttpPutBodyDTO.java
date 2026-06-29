package com.kuocai.cdn.api.huawei.cdn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 证书设置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class HttpPutBodyDTO {

    /**
     * HTTPS证书是否启用。（on：开启，off：关闭）
     */
    private String https_status;

    /**
     * 证书名字。（长度限制为3-32字符）。当证书开启时必传。
     */
    private String certificate_name;

    /**
     * HTTPS协议使用的证书内容，当证书开启时必传。取值范围：PEM编码格式。
     */
    private String certificate_value;

    /**
     * HTTPS协议使用的私钥，当证书开启时必传。取值范围：PEM编码格式。
     */
    private String private_key;

    /**
     * 证书来源。1：代表华为云托管证书；0：表示自有证书。 默认值0。当证书开启时必传。
     */
    private Integer certificate_source;

    /**
     * 是否使用HTTP2.0。（on：是，off：否。）,默认关闭，https_status=off时，该值不生效。
     */
    private String http2_status;

    /**
     * OCSP Stapling
     * 实现由CDN预先缓存在线证书验证结果并下发客户端。
     */
    private String ocsp_stapling_status;

    /**
     * 传输层安全性协议。目前支持TLSv1.0/1.1/1.2/1.3四个版本的协议。默认全部开启，不可全部关闭，只可开启连续或单个版本号。多版本开启时，使用逗号拼接传输，例：TLSv1.1,TLSv1.2。
     */
    private String tls_version;


}
