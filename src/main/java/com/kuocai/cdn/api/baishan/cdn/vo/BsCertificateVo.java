package com.kuocai.cdn.api.baishan.cdn.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BsCertificateVo {

    /**
     * pem文件
     */
    private String certificate;

    /**
     * key文件
     */
    private String key;

    /**
     * 证书名称
     */
    private String name;

    /**
     * 传入证书id,则默认修改
     */
    private Integer cert_id;
}
