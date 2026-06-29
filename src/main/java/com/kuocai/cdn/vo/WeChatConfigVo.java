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
public class WeChatConfigVo {

    /**
     * 应用ID
     */
    private String appIdWechat;

    /**
     * 直连商户号
     */
    private String merchantIdWechat;

    /**
     * 商户秘钥
     */
    private String wechatKeyWechat;

    /**
     * 序列号
     */
    private String merchantSerialNumberWechat;

    /**
     * 回调地址
     */
    private String notifyUrlWechat;

    /**
     * 私密路径
     */
    private String privateKeyPathWechat;

    /**
     * 启用状态
     */
    private Integer wechatStatus;

}
