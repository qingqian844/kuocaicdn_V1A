package com.kuocai.cdn.api.huawei.cdn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 查询HTTPS证书关联域名DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CertificateDomainNameDTO {

    /**
     * 证书类型
     */
    private String cert_type;

    /**
     * 证书名称
     */
    private String cert_name;

    /**
     * 证书内容
     */
    private String certificate;
}
