package com.kuocai.cdn.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author xiaobo
 * @date 2023/3/27
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlipayAuthenticationConfigVo {

    /**
     * 应用Id
     */
    private String appIdAlipay;

    /**
     * 支付宝公钥
     */
    private String publicKeyAlipay;

    /**
     * 应用私钥
     */
    private String privateKeyAlipay;

    /**
     * 认证链接
     */
    private String alipayCertificationUrl;

    /**
     * 请求网关
     */
    private String gatewayUrlAlipay;

    /**
     * 编码格式
     */
    private String charsetAlipay;

    /**
     * 签名方式
     */
    private String signTypeAlipay;

    /**
     * 格式
     */
    private String formatAlipay;
}
