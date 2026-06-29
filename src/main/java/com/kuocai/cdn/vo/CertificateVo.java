package com.kuocai.cdn.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * (CertificateVo)
 *
 * @author CHENWEI
 * @since 2023-03-10 21:06:04
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificateVo {

    /**
     * 分页页码
     */
    private int startRecord = 1;

    /**
     * 分页数据大小
     */
    private int limitRecord = 10000;

    /**
     * 加速域名名称
     */
    private String domainName;

    /**
     * 域名所属用户的domain_id。
     */
    private String userDomainId;
}
