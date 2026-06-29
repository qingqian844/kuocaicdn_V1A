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
public class WeChatCodeConfigVo {

    /**
     * 启用状态：1启用，0停用
     */
    private Integer wechatStatus;

    /**
     * 应用ID
     */
    private String appId;

    /**
     * 应用秘钥
     */
    private String appSecret;

    /**
     * 微信服务器配置 Token
     */
    private String token;

    /**
     * 回调地址
     */
    private String notifyUrl;

}
