package com.kuocai.cdn.service;

import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.api.huawei.cdn.DomainConfigureApi;
import com.kuocai.cdn.api.huawei.cdn.dto.HttpsCertificateInfoQueryDTO;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.exception.CdnHuaweiException;
import com.kuocai.cdn.vo.CertificateVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 证书管理服务
 */
@Slf4j
@Service
public class HttpsCertificateService {

    /**
     * 查询所有绑定HTTPS证书的域名信息
     *
     * @param certificateVo
     */
    public JSONObject queryCertificateInfosByPage(CertificateVo certificateVo) throws BusinessException {
        HttpsCertificateInfoQueryDTO query = HttpsCertificateInfoQueryDTO.builder()
                .domain_name(certificateVo.getDomainName()).user_domain_id(certificateVo.getUserDomainId())
                .page_number(certificateVo.getStartRecord()).page_size(certificateVo.getLimitRecord())
                .build();
        try {
            return DomainConfigureApi.getHttpsCertificateInfo(query);
        } catch (CdnHuaweiException e) {
            log.error("查询HTTPS证书绑定的域名信息");
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }
}
