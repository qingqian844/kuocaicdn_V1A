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
public class AliWithdrawConfigVo {

    /**
     * 应用Id
     */
    private String appIdAliWithdraw;

    /**
     * 商户私钥
     */
    private String privateKeyAliWithdraw;

    /**
     * 应用公钥存放路径
     */
    private String appCertPublicKeyPath;

    /**
     * 支付宝公钥存放的路径
     */
    private String alipayCertPublicKeyPath;

    /**
     * 支付宝根证书存放的路径
     */
    private String alipayRootCertPath;

    /**
     * 签名方式
     */
    private String signTypeAliWithdraw;

    /**
     * 字符编码格式
     */
    private String charsetAliWithdraw;

    /**
     * 支付宝网关
     */
    private String gatewayUrlAliWithdraw;

    /**
     * 启用状态
     */
    private Integer aliWithdrawStatus;

}
