package com.kuocai.cdn.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author xiaobo
 * @date 2023/3/22
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AliPayConfigVo {

    /**
     * 应用Id
     */
    private String appIdAlipay;

    /**
     * 商户私钥
     */
    private String privateKeyAlipay;

    /**
     * 支付宝公钥
     */
    private String publicKeyAlipay;

    /**
     * 回调地址
     */
    private String notifyUrlAlipay;

    /**
     * 签名方式
     */
    private String signTypeAlipay;

    /**
     * 字符编码格式
     */
    private String charsetAlipay;

    /**
     * 支付宝网关
     */
    private String gatewayUrlAlipay;

    /**
     * 启用状态
     */
    private Integer alipayStatus;

}
