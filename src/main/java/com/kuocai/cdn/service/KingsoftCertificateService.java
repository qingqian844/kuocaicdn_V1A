package com.kuocai.cdn.service;

import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.api.kingsoft.cdn.KingsoftApiService;
import com.kuocai.cdn.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 金山云证书管理服务
 */
@Slf4j
@Service
public class KingsoftCertificateService {

    @Autowired
    private KingsoftApiService kingsoftApiService;

    /**
     * 创建/更新证书
     *
     * @param certificateName 证书名称
     * @param serverCertificate 服务器证书内容
     * @param privateKey 私钥内容
     * @param certificateId 证书ID（更新时需要）
     * @return 证书ID
     * @throws BusinessException 业务异常
     */
    public void configCertificate(String domainIds, String enable, String certificateId,
                                  String certificateName, String serverCertificate, String privateKey) throws BusinessException {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("DomainIds", domainIds);
            body.put("Enable", enable);

            if ("on".equalsIgnoreCase(enable)) {
                if (certificateId != null && !certificateId.isEmpty()) {
                    body.put("CertificateId", certificateId);
                } else {
                    if (certificateName == null || certificateName.isEmpty() ||
                            serverCertificate == null || serverCertificate.isEmpty() ||
                            privateKey == null || privateKey.isEmpty()) {
                        throw new IllegalArgumentException("启用HTTPS并提供新证书时，证书名称、内容和私钥均不能为空。");
                    }
                    body.put("CertificateName", certificateName);
                    body.put("ServerCertificate", serverCertificate);
                    body.put("PrivateKey", privateKey);
                }
            }

            kingsoftApiService.postKingsoftApi(
                    "ConfigCertificate",
                    "2016-09-01",
                    "/2016-09-01/cert/ConfigCertificate",
                    body
            );

            log.info("金山云CDN域名证书配置成功，域名ID：{}, 启用状态：{}", domainIds, enable);
        } catch (Exception e) {
            log.error("金山云CDN域名证书配置失败，域名ID：{}, 启用状态：{}", domainIds, enable, e);
            // 注意：这里的异常会向上抛出，由上层的 handleKingsoftException 方法处理
            throw new BusinessException("金山云CDN域名证书配置失败：" + e.getMessage());
        }
    }
    
    /**
     * 为域名配置证书
     *
     * @param domainIds 域名ID列表，逗号分隔
     * @param certificateId 证书ID
     * @param enable 是否启用HTTPS，on/off
     * @throws BusinessException 业务异常
     */
    public void configCertificate(String domainIds, String certificateId, String enable) throws BusinessException {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("DomainIds", domainIds);
            body.put("Enable", enable);
            
            if ("on".equals(enable) && certificateId != null && !certificateId.isEmpty()) {
                body.put("CertificateId", certificateId);
            }
            
            kingsoftApiService.postKingsoftApi(
                    "ConfigCertificate",
                    "2016-09-01",
                    "/2016-09-01/cert/ConfigCertificate",
                    body
            );
            
            log.info("金山云CDN域名证书配置成功，域名ID：{}, 证书ID：{}", domainIds, certificateId);
        } catch (Exception e) {
            log.error("金山云CDN域名证书配置失败，域名ID：{}, 证书ID：{}", domainIds, certificateId, e);
            throw new BusinessException("金山云CDN域名证书配置失败：" + e.getMessage());
        }
    }
    
    /**
     * 配置TLS版本
     *
     * @param domainId 域名ID
     * @param tlsVersions TLS版本列表，如["TLS 1.0", "TLS 1.1", "TLS 1.2", "TLS 1.3"]
     * @throws BusinessException 业务异常
     */
    public void setTlsVersions(String domainId, String[] tlsVersions) throws BusinessException {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("DomainId", domainId);
            body.put("TlsVersions", tlsVersions);

            kingsoftApiService.postKingsoftApi(
                    "SetTlsVersionConfig",
                    "2016-09-01",
                    "/2016-09-01/domain/SetTlsVersionConfig",
                    body
            );

            log.info("金山云CDN TLS版本配置成功，域名ID：{}", domainId);
        } catch (Exception e) {
            log.error("金山云CDN TLS版本配置失败，域名ID：{}", domainId, e);
            throw new BusinessException("金山云CDN TLS版本配置失败：" + e.getMessage());
        }
    }

    /**
     * 配置OCSP Stapling
     *
     * @param domainId 域名ID
     * @param enable 是否启用OCSP Stapling，on/off
     * @throws BusinessException 业务异常
     */
    public void setOcspStapling(String domainId, String enable) throws BusinessException {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("DomainId", domainId);
            body.put("Enable", enable);

            kingsoftApiService.postKingsoftApi(
                    "SetOcspStaplingConfig",
                    "2016-09-01",
                    "/2016-09-01/domain/SetOcspStaplingConfig",
                    body
            );

            log.info("金山云CDN OCSP Stapling配置成功，域名ID：{}", domainId);
        } catch (Exception e) {
            log.error("金山云CDN OCSP Stapling配置失败，域名ID：{}", domainId, e);
            throw new BusinessException("金山云CDN OCSP Stapling配置失败：" + e.getMessage());
        }
    }
    
    /**
     * 查询证书列表
     *
     * @param pageNumber 页码，从1开始
     * @param pageSize 每页记录数
     * @return 证书列表信息
     * @throws BusinessException 业务异常
     */
    public JSONObject queryCertificates(int pageNumber, int pageSize) throws BusinessException {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("PageNumber", String.valueOf(pageNumber));
            params.put("PageSize", String.valueOf(pageSize));
            
            JSONObject response = kingsoftApiService.callKingsoftApi(
                    "GetCertificates",
                    "2016-09-01",
                    params
            );
            
            log.info("金山云CDN证书列表查询成功");
            return response;
        } catch (Exception e) {
            log.error("金山云CDN证书列表查询失败", e);
            throw new BusinessException("金山云CDN证书列表查询失败：" + e.getMessage());
        }
    }
} 
