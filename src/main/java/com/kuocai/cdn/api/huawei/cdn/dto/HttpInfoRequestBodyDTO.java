package com.kuocai.cdn.api.huawei.cdn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * https配置对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class HttpInfoRequestBodyDTO {

    /**
     * 证书名字。（长度限制为3-32字符）。
     */
    private String cert_name;

    /**
     * HTTPS证书是否启用。
     * 常量类参考 HttpsStatus
     */
    private Integer https_status;

    /**
     * 功能说明：HTTPS协议使用的证书内容，不启用证书则无需输入。
     * 取值范围：PEM编码格式。
     * 初次配置证书时必传。
     */
    private String certificate;

    /**
     * 功能说明： HTTPS协议使用的私钥，不启用证书则无需输入。
     * 取值范围：PEM编码格式。
     * 初次配置证书时必传。
     */
    private String private_key;

    /**
     * 是否使用HTTP2.0
     * 常量类参考 Http2
     */
    private Integer http2;

    /**
     * 证书类型
     * 常量类参考 CertificateType
     */
    private Integer certificate_type;

    /**
     * 强制跳转HTTPS（0：不强制；1：强制） 为空值时默认设置为关闭。
     * （建议使用force_redirect_config修改配置）
     * 常量类参考 ForceRedirectHttps
     */
    private Integer force_redirect_https;

    /**
     * 强制跳转
     */
    private ForceRedirectDTO force_redirect_config;

    /**
     * OCSP_STAPLING开关
     */
    private Integer ocsp_stapling;
}
