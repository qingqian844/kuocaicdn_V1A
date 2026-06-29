package com.kuocai.cdn.api.yifan.cdn.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * https 配置
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Accessors(chain = true)
public class HttpsConfigDTO {
    /**
     * 公钥
     */
    private String certificate;
    /**
     * 证书名称
     */
    private String certName;
    /**
     * 域名有效期
     */
    private Long expirationTime;
    /**
     * 是否开启https
     */
    private String httpsStatus;
    /**
     * 私钥
     */
    private String privateKey;
}




