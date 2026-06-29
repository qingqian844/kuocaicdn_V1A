package com.kuocai.cdn.vo;


import com.kuocai.cdn.api.huawei.cdn.dto.CertificateDomainNameDTO;
import com.kuocai.cdn.api.huawei.cdn.dto.UpdateDomainMultiCertificatesRequestBodyContentDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 证书管理页面VO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CertificationCreateVo {

    /**
     * 一个证书批量设置多个域名
     */
    private UpdateDomainMultiCertificatesRequestBodyContentDTO domainMultiCertificates;

    /**
     * 查询HTTPS证书关联域名接口
     */
    private CertificateDomainNameDTO certificateDomainName;
}
