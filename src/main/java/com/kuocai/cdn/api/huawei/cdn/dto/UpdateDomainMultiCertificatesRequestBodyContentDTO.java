package com.kuocai.cdn.api.huawei.cdn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * https对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class UpdateDomainMultiCertificatesRequestBodyContentDTO {

    /**
     * 域名列表,逗号分割，上限50个域名
     */
    private String domain_name;

    /**
     * https开关
     * 常量类参考 HttpsSwitch
     * https_switch为1时，证书参数不能为空
     */
    private Integer https_switch;

    /**
     * 回源方式
     * 常量类参考 AccessOriginWay
     */
    private Integer access_origin_way;

    /**
     * 强制跳转HTTPS
     * 常量类参考 ForceRedirectHttps
     */
    private Integer force_redirect_https;

    /**
     * 强制跳转
     */
    private ForceRedirectDTO force_redirect_config;

    /**
     * 是否使用HTTP2.0
     * 常量类参考 Http2
     */
    private Integer http2;

    /**
     * 证书名字。（长度限制为3-32字符）。
     */
    private String cert_name;

    /**
     * 证书内容（设置证书必填）
     */
    private String certificate;

    /**
     * 私钥内容（设置证书必填）
     */
    private String private_key;

    /**
     * 证书类型
     * 常量类参考 CertificateType
     */
    private Integer certificate_type;
}
