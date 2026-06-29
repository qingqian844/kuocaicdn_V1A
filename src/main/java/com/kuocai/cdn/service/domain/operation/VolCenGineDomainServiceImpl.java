package com.kuocai.cdn.service.domain.operation;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.kuocai.cdn.api.*;
import com.kuocai.cdn.api.huawei.cdn.dto.*;
import com.kuocai.cdn.api.volcengine.cdn.VolcengineRequest;
import com.kuocai.cdn.api.tencent.dns.CreateRecordResponse;
import com.kuocai.cdn.api.tencent.dns.TencentApi;
import com.kuocai.cdn.api.tencent.dns.dto.CreateRecordDTO;
import com.kuocai.cdn.api.tencent.dns.properties.TencentDns;
import com.kuocai.cdn.api.volcengine.cdn.properties.VolcengineCdn;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.entity.CdnDomainSources;
import com.kuocai.cdn.enumeration.domainmerage.CdnRoute;
import com.kuocai.cdn.enumeration.domainmerage.domain.OriginProtocolEnum;
import com.kuocai.cdn.enumeration.domainmerage.domain.OriginTypeEnum;
import com.kuocai.cdn.enumeration.domainmerage.domain.ServiceAreaEnum;
import com.kuocai.cdn.enumeration.domainmerage.route.CdnOperationRoute;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.exception.CdnHuaweiException;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.service.domain.operation.optional.ICdnDomainVerifyService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.DomainUtil;
import com.kuocai.cdn.util.KuocaiBaseUtil;
import com.kuocai.cdn.vo.*;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.volcengine.model.beans.CDN;
import com.volcengine.service.cdn.CDNService;
import com.volcengine.service.cdn.impl.CDNServiceImpl;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 火山云加速域名(CdnDomain)服务
 */
@Slf4j
@Service
public class VolCenGineDomainServiceImpl extends BaseService<CdnDomain> implements ICdnPlatformService, ICdnDomainVerifyService {


    @Override
    @Transactional(rollbackFor = {Exception.class})
    public CdnDomain create(Long userId, String domainName, String businessType, String serviceArea, String originType, String ipOrDomain) throws BusinessException {
        CDNService service = CDNServiceImpl.getInstance();
        try {
            service.setAccessKey(VolcengineCdn.AK);
            service.setSecretKey(VolcengineCdn.SK);
            originType = OriginTypeEnum.getOtherParam(originType).getVolCenGine();
            String[] ipOrDomains = ipOrDomain.split(";");
            List<CDN.OriginLine> originLines = new ArrayList<>();
            for (String s : ipOrDomains) {
                CDN.OriginLine primary =
                        new CDN.OriginLine()
                                .setOriginType("primary")
                                .setInstanceType(originType)
                                .setAddress(s)
                                .setHttpPort("80")
                                .setHttpsPort("443")
                                .setOriginHost(domainName)
                                .setWeight("100");
                originLines.add(primary);
            }
            CDN.AddCdnDomainRequest req = new CDN.AddCdnDomainRequest()
                    .setProject(VolcengineCdn.normalizeProject(VolcengineCdn.Project))
                    .setDomain(domainName)
                    .setServiceType(businessType)
                    .setOriginProtocol("HTTP")
                    .setOrigin(Arrays.asList(new CDN.OriginRule().setOriginAction(new CDN.OriginAction().setOriginLines(originLines))));
            CDN.AddCdnDomainResponse resp = null;
            resp = service.addCdnDomain(req);
            dealResponse(JSON.toJSONString(resp));
            DomainBasicInfo domainBasicInfo = null;
            boolean flag = true;
            while (flag) {
                DomainConfig domainConfig = getDomainConfig(domainName);
                domainBasicInfo = domainConfig.getDomainBasicInfo();
                if (Assert.notEmpty(domainBasicInfo.getCname())) {
                    flag = false;
                }
                Thread.sleep(1000);
            }
            CdnDomain cdnDomain = CdnDomain.builder()
                    .domainName(domainBasicInfo.getDomainName())
                    .businessType(domainBasicInfo.getBusinessType())
                    .serviceArea(domainBasicInfo.getServiceArea())
                    .domainStatus(domainBasicInfo.getDomainStatus())
                    .cnameVolcengine(domainBasicInfo.getCname())
                    .userId(userId)
                    .route(CdnRoute.VOLCENGINE.getCode())
                    .build();
            try {
                Thread.sleep(1000);
                saveRangeTimeOut(cdnDomain, new DomainOriginSettingVo().setOriginReceiveTimeOut(30));
            } catch (Exception e) {
                log.error("创建域名 {} 初始化回源超时时间失败：{}", domainName, e.getMessage());
            }
            try {
                SettingCacheVo cacheVo = new SettingCacheVo();
                Thread.sleep(1000);
                //设置一个全部文件缓存
                List<CacheRuleDTO> cacheRuleDTOS = ListUtil.toList(new CacheRuleDTO().setMatch_type("all").setPriority(1).setTtl(30).setTtl_unit("d"));
                cacheVo.setCacheRules(cacheRuleDTOS);
                saveCacheRules(cdnDomain, cacheVo);
            } catch (Exception e) {
                log.error("创建域名 {} 初始化缓存规则失败：{}", domainName, e.getMessage());
            }
            return cdnDomain;
        } catch (Exception e) {
            throw new BusinessException(e.getMessage());
        } finally {
            service.destroy();
        }
    }

    @Override
    public CdnDomain configDNS(CdnDomain cdnDomain) throws TencentCloudSDKException, BusinessException {
        String domainName = cdnDomain.getDomainName();
        String cnameVolcengine = cdnDomain.getCnameVolcengine();
        CreateRecordDTO createRecordRequest = new CreateRecordDTO();
        createRecordRequest.setDomain(TencentDns.LOCAL_DOMAIN_NAME).setSubDomain(DomainUtil.convertSubDomain(domainName)).setValue(cnameVolcengine);
        CreateRecordResponse recordResponse = TencentApi.createRecord(createRecordRequest);
        // 如果返回有记录id，说明返回成功
        if (!Assert.isEmpty(recordResponse.getRecordId())) {
            // 自己定义括彩云的
            cdnDomain.setCname(createRecordRequest.getSubDomain() + "." + TencentDns.LOCAL_DOMAIN_NAME);
            cdnDomain.setTencentDnsId(recordResponse.getRecordId());
            cdnDomain = save(cdnDomain);
            return cdnDomain;
        }
        // 如果返回没有记录id,说明失败 TODO**这里如果失败了，dns解析不到
        // 如果返回没有记录id,说明失败 TODO**这里如果失败了，dns解析不到
        if (Assert.isEmpty(recordResponse.getRecordId())) {
            log.error("dns解析失败，域名：{}", cdnDomain.getDomainName());
            throw new BusinessException("dns解析失败");
        }
        return null;
    }

    @Override
    public void save(CdnDomain cdnDomain, String businessType, String serviceArea) throws BusinessException {

    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public void disable(CdnDomain cdnDomain) throws BusinessException {
        CDNService service = CDNServiceImpl.getInstance();
        service.setAccessKey(VolcengineCdn.AK);
        service.setSecretKey(VolcengineCdn.SK);
        try {
            CDN.StopCdnDomainRequest req = new CDN.StopCdnDomainRequest()
                    .setDomain(cdnDomain.getDomainName());
            CDN.StopCdnDomainResponse resp = service.stopCdnDomain(req);
            dealResponse(JSON.toJSONString(resp));
        } catch (Exception e) {
            log.error("停用加速域名失败，域名信息：{}", cdnDomain);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        } finally {
            service.destroy();
        }
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public void enable(CdnDomain cdnDomain) throws BusinessException {
        CDNService service = CDNServiceImpl.getInstance();
        service.setAccessKey(VolcengineCdn.AK);
        service.setSecretKey(VolcengineCdn.SK);
        try {
            CDN.StartCdnDomainRequest req = new CDN.StartCdnDomainRequest()
                    .setDomain(cdnDomain.getDomainName());
            CDN.StartCdnDomainResponse resp = service.startCdnDomain(req);
            dealResponse(JSON.toJSONString(resp));
        } catch (Exception e) {
            log.error("启用加速域名失败，域名信息：{}", cdnDomain);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        } finally {
            service.destroy();
        }
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public void delete(CdnDomain cdnDomain) throws BusinessException {
        CDNService service = CDNServiceImpl.getInstance();
        service.setAccessKey(VolcengineCdn.AK);
        service.setSecretKey(VolcengineCdn.SK);
        try {
            CDN.DeleteCdnDomainRequest req = new CDN.DeleteCdnDomainRequest()
                    .setDomain(cdnDomain.getDomainName());
            CDN.DeleteCdnDomainResponse resp = service.deleteCdnDomain(req);
            dealResponse(JSON.toJSONString(resp));
        } catch (Exception e) {
            log.error("删除加速域名失败，域名信息：{}", cdnDomain);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        } finally {
            service.destroy();
        }
    }

    /**
     * @param cdnDomain
     * @param status    1,0
     * @throws BusinessException
     */
    @Override
    public void ipv6(CdnDomain cdnDomain, Integer status) throws BusinessException {
        CDNService service = CDNServiceImpl.getInstance();
        service.setAccessKey(VolcengineCdn.AK);
        service.setSecretKey(VolcengineCdn.SK);
        try {
            boolean ipv6 = ObjectUtil.equal(status, 1) ? true : false;
            CDN.UpdateCdnConfigRequest req = new CDN.UpdateCdnConfigRequest()
                    .setDomain(cdnDomain.getDomainName())
                    .setIPv6(new CDN.IPv6().setSwitch(ipv6));
            CDN.UpdateCdnConfigResponse resp = service.updateCdnConfig(req);
            dealResponse(JSON.toJSONString(resp));
        } catch (Exception e) {
            throw new CdnHuaweiException(e.getMessage()).setCause(e).log();
        } finally {
            service.destroy();
        }
    }


    @Override
    public void saveSourceStationConfig(CdnDomain cdnDomain, CdnDomainSourcesVo config) throws BusinessException {
        CdnDomainSources main = config.getMain();
        CdnDomainSources back = config.getBack();
        CDNService service = CDNServiceImpl.getInstance();
        service.setAccessKey(VolcengineCdn.AK);
        service.setSecretKey(VolcengineCdn.SK);
        try {
            List<CDN.OriginLine> originLines = covert2OriginLine(new ArrayList<>(), main);
            if (Assert.notEmpty(back)) {
                originLines = covert2OriginLine(originLines, back);
            }
            CDN.UpdateCdnConfigRequest req = new CDN.UpdateCdnConfigRequest()
                    .setDomain(cdnDomain.getDomainName())
                    .setOrigin(Arrays.asList(new CDN.OriginRule().setOriginAction(new CDN.OriginAction().setOriginLines(originLines))));
            CDN.UpdateCdnConfigResponse resp = service.updateCdnConfig(req);
            dealResponse(JSON.toJSONString(resp));
        } catch (Exception e) {
            throw new CdnHuaweiException(e.getMessage()).setCause(e).log();
        } finally {
            service.destroy();
        }
    }

    public List<CDN.OriginLine> covert2OriginLine(List<CDN.OriginLine> params, CdnDomainSources source) throws BusinessException {
        List<String> mainIpOrDmains = Arrays.asList(source.getIpOrDomain().split(";"));
        for (String mainIpOrDomain : mainIpOrDmains) {
            CDN.OriginLine param = new CDN.OriginLine();
            param.setAddress(mainIpOrDomain);
            param.setHttpPort(source.getHttpPort().toString());
            param.setHttpsPort(source.getHttpsPort().toString());
            param.setInstanceType(OriginTypeEnum.getOtherParam(source.getOriginType()).getVolCenGine());
            param.setOriginHost(source.getHostName());
            if (ObjectUtil.equal(source.getActiveStandby(), 1)) {
                param.setOriginType("primary");
            } else {
                param.setOriginType("backup");
            }
            params.add(param);
        }
        return params;
    }

    /**
     * 切换主备源站
     *
     * @param cdnDomain
     * @throws BusinessException
     */
    @Override
    public void change(CdnDomain cdnDomain) throws BusinessException {
        CDNService service = CDNServiceImpl.getInstance();
        service.setAccessKey(VolcengineCdn.AK);
        service.setSecretKey(VolcengineCdn.SK);
        DomainConfig domainConfig = getDomainConfig(cdnDomain.getDomainName());
        DomainBasicInfo.SourceStationPrimaryInfo sourceStationPrimaryInfo = domainConfig.getDomainBasicInfo().getSourceStationPrimaryInfo();
        DomainBasicInfo.SourceStationStandbyInfo sourceStationStandbyInfo = domainConfig.getDomainBasicInfo().getSourceStationStandbyInfo();

        if (Assert.isEmpty(sourceStationStandbyInfo.getIpOrDomain())) {
            throw new BusinessException("加速域名未配置备源站");
        }
        try {
            // 转换参数
            List<String> mainIps = Arrays.asList(sourceStationPrimaryInfo.getIpOrDomain().split(";"));
            List<String> standbyIps = Arrays.asList(sourceStationStandbyInfo.getIpOrDomain().split(";"));
            List<CDN.OriginLine> params = new ArrayList<>();
            for (String mainIp : mainIps) {
                // active_standby 0备战 1主站
                CDN.OriginLine mainSource = new CDN.OriginLine();
                mainSource.setAddress(mainIp);
                mainSource.setHttpPort(sourceStationPrimaryInfo.getHttpPort());
                mainSource.setHttpsPort(sourceStationPrimaryInfo.getHttpsPort());
                mainSource.setInstanceType(OriginTypeEnum.getOtherParam(sourceStationPrimaryInfo.getSourceStationType()).getVolCenGine());
                mainSource.setOriginType("backup");
                params.add(mainSource);
            }
            for (String standbyIp : standbyIps) {
                CDN.OriginLine backSource = new CDN.OriginLine();
                backSource.setAddress(standbyIp);
                backSource.setHttpPort(sourceStationStandbyInfo.getHttpPort());
                backSource.setHttpsPort(sourceStationStandbyInfo.getHttpsPort());
                backSource.setInstanceType(OriginTypeEnum.getOtherParam(sourceStationStandbyInfo.getSourceStationType()).getVolCenGine());
                backSource.setOriginType("primary");
                params.add(backSource);
            }
            CDN.UpdateCdnConfigRequest req = new CDN.UpdateCdnConfigRequest()
                    .setDomain(cdnDomain.getDomainName())
                    .setOrigin(Arrays.asList(new CDN.OriginRule().setOriginAction(new CDN.OriginAction().setOriginLines(params))));
            CDN.UpdateCdnConfigResponse resp = service.updateCdnConfig(req);
            dealResponse(JSON.toJSONString(resp));
        } catch (Exception e) {
            throw new CdnHuaweiException(e.getMessage()).setCause(e).log();
        } finally {
            service.destroy();
        }
    }

    @Override
    public void saveOriginProtocol(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        CDNService service = CDNServiceImpl.getInstance();
        service.setAccessKey(VolcengineCdn.AK);
        service.setSecretKey(VolcengineCdn.SK);
        try {
            CDN.UpdateCdnConfigRequest req = new CDN.UpdateCdnConfigRequest()
                    .setDomain(cdnDomain.getDomainName()).setOriginProtocol(OriginProtocolEnum.getOtherParam(domainOriginSettingVo.getOriginProtocol()).getVolCenGine());
            CDN.UpdateCdnConfigResponse resp = service.updateCdnConfig(req);
            dealResponse(JSON.toJSONString(resp));
        } catch (Exception e) {
            log.error("修改回源配置类型，域名信息：{}", cdnDomain);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        } finally {
            service.destroy();
        }
    }

    @Override
    public void saveOriginRequestUrlRewrite(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {

    }

    @Override
    public void saveAdvancedReturnSource(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {

    }

    @Override
    public void saveRangeSwitch(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        CDNService service = CDNServiceImpl.getInstance();
        service.setAccessKey(VolcengineCdn.AK);
        service.setSecretKey(VolcengineCdn.SK);
        try {
            String status = domainOriginSettingVo.getStatus();
            boolean originRange = ObjectUtil.equal("on", status) ? true : false;
            CDN.UpdateCdnConfigRequest req = new CDN.UpdateCdnConfigRequest()
                    .setDomain(cdnDomain.getDomainName()).setOriginRange(originRange);
            CDN.UpdateCdnConfigResponse resp = service.updateCdnConfig(req);
            dealResponse(JSON.toJSONString(resp));
        } catch (Exception e) {
            log.error("修改Range回源，域名信息：{}", cdnDomain);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        } finally {
            service.destroy();
        }
    }

    @Override
    public void saveRangeVerifyETag(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {

    }

    @Override
    public void saveOriginHost(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {

    }

    @Override
    public void saveRangeTimeOut(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        CDNService service = CDNServiceImpl.getInstance();
        service.setAccessKey(VolcengineCdn.AK);
        service.setSecretKey(VolcengineCdn.SK);
        try {
            Integer overTime = domainOriginSettingVo.getOriginReceiveTimeOut();
            if (Assert.isEmpty(overTime)) {
                throw new BusinessException("参数异常");
            }
            CDN.UpdateCdnConfigRequest req = new CDN.UpdateCdnConfigRequest()
                    .setDomain(cdnDomain.getDomainName()).setTimeout(
                            new CDN.TimeoutArg()
                                    .setSwitch(true).
                                    setTimeoutRules(Arrays.asList(new CDN.TimeoutRule()
                                            .setTimeoutAction(new CDN.OriginTimeoutAction()
                                                    .setHttpTimeout(overTime.longValue())
                                                    .setTcpTimeout(overTime.longValue())))));
            CDN.UpdateCdnConfigResponse resp = service.updateCdnConfig(req);
            dealResponse(JSON.toJSONString(resp));
        } catch (Exception e) {
            log.error("修改回源超时时间，域名信息：{}", cdnDomain);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        } finally {
            service.destroy();
        }
    }

    @Override
    public void saveOriginRequestHeader(CdnDomain cdnDomain, DomainOriginSettingVo config) throws BusinessException {
        CDNService service = CDNServiceImpl.getInstance();
        service.setAccessKey(VolcengineCdn.AK);
        service.setSecretKey(VolcengineCdn.SK);
        try {
            List<OriginRequestHeaderDTO> originRequestHeaders = config.getOriginRequestHeader();
            List<CDN.RequestHeaderInstance> requestHeaderInstances = new ArrayList<>();
            for (OriginRequestHeaderDTO originRequestHeader : originRequestHeaders) {
                CDN.RequestHeaderInstance requestHeaderInstance = new CDN.RequestHeaderInstance();
                requestHeaderInstance.setAction(originRequestHeader.getAction());
                requestHeaderInstance.setKey(originRequestHeader.getName());
                requestHeaderInstance.setValue(originRequestHeader.getValue());
                requestHeaderInstance.setValueType("constant");
                requestHeaderInstances.add(requestHeaderInstance);
            }
            CDN.UpdateCdnConfigRequest req = new CDN.UpdateCdnConfigRequest()
                    .setDomain(cdnDomain.getDomainName())
                    .setRequestHeader(Arrays.asList(new CDN.RequestHeaderRule().setRequestHeaderAction(new CDN.RequestHeaderAction()
                            .setRequestHeaderInstances(requestHeaderInstances))));
            CDN.UpdateCdnConfigResponse resp = service.updateCdnConfig(req);
            dealResponse(JSON.toJSONString(resp));
        } catch (Exception e) {
            log.error("修改HTTP请求头配置，域名信息：{}", cdnDomain);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        } finally {
            service.destroy();
        }
    }

    /**
     * Https配置
     *
     * @param cdnDomain
     * @param config
     * @throws BusinessException
     */
    @Override
    public void httpsConfiguration(CdnDomain cdnDomain, DomainHttpsSettingVo config) throws BusinessException {
        CDNService service = CDNServiceImpl.getInstance();
        service.setAccessKey(VolcengineCdn.AK);
        service.setSecretKey(VolcengineCdn.SK);
        HttpPutBodyDTO https = config.getHttps();
        try {
            String httpsStatus = https.getHttps_status();
            if (ObjectUtil.equal(httpsStatus, "on")) {
                CDN.AddCdnCertificateRequest req = new CDN.AddCdnCertificateRequest()
                        .setCertificate(new CDN.Certificate().setCertificate(https.getCertificate_value()).setPrivateKey(https.getPrivate_key()))
                        .setCertInfo(new CDN.AddCdnCertInfo().setDesc(https.getCertificate_name()))
                        //volc_cert_center：表示将证书存放到证书中心。
                        //cdn_cert_hosting：表示将证书托管在内容分发网络。在内容分发网络上托管证书是白名单功能。要使用该功能，请提交工单。
                        .setSource("volc_cert_center");
                CDN.AddCdnCertificateResponse resp = service.addCdnCertificate(req);
                JSONObject responseObject = JSONObject.parseObject(JSON.toJSONString(resp));
                JSONObject responseMetadata = responseObject.getJSONObject("ResponseMetadata");
                if (responseMetadata.containsKey("Error")) {
                    JSONObject error = responseMetadata.getJSONObject("Error");
                    throw new BusinessException(error.getString("Message"));
                }
                String certId = responseObject.getString("Result");
                // 关联域名和证书
                CDN.UpdateCdnConfigRequest updateCdnConfigRequest = new CDN.UpdateCdnConfigRequest()
                        .setHTTPS(new CDN.HTTPS().setSwitch(true).setCertInfo(new CDN.CertInfo().setCertId(certId)).setHTTP2(false))
                        .setDomain(cdnDomain.getDomainName());
                CDN.UpdateCdnConfigResponse updateCdnConfigResponse = service.updateCdnConfig(updateCdnConfigRequest);
                dealResponse(JSON.toJSONString(updateCdnConfigResponse));
            } else if (ObjectUtil.equal(httpsStatus, "off")) {
                CDN.UpdateCdnConfigRequest updateCdnConfigRequest = new CDN.UpdateCdnConfigRequest()
                        .setHTTPS(new CDN.HTTPS().setSwitch(false))
                        .setDomain(cdnDomain.getDomainName());
                CDN.UpdateCdnConfigResponse updateCdnConfigResponse = service.updateCdnConfig(updateCdnConfigRequest);
                dealResponse(JSON.toJSONString(updateCdnConfigResponse));
            }
        } catch (Exception e) {
            log.error("HTTPS配置，域名信息：{}", cdnDomain);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        } finally {
            service.destroy();
        }
    }

    /**
     * TLS版本配置和HTTP/2配置和OCSP Stapling配置
     *
     * @param cdnDomain
     * @param config
     * @throws BusinessException
     */
    @Override
    public void httpsConfigurationOther(CdnDomain cdnDomain, DomainHttpsSettingVo config) throws BusinessException {
        CDNService service = CDNServiceImpl.getInstance();
        service.setAccessKey(VolcengineCdn.AK);
        service.setSecretKey(VolcengineCdn.SK);
        HttpPutBodyDTO https = config.getHttps();
        String httpsStatus = https.getHttps_status();
        try {
            String http2Status = https.getHttp2_status();
            String ocspStaplingStatus = https.getOcsp_stapling_status();
            String tlsVersion = https.getTls_version();
            CDN.HTTPS httpsParam = new CDN.HTTPS();
            // http2设置
            if (Assert.notEmpty(http2Status)) {
                boolean http2 = ObjectUtil.equal("on", http2Status) ? true : false;
                httpsParam.setHTTP2(http2);
            }
            // OCSP设置
            if (Assert.notEmpty(ocspStaplingStatus)) {
                boolean ocsp = ObjectUtil.equal("on", ocspStaplingStatus) ? true : false;
                httpsParam.setOCSP(ocsp);
            }
            if (Assert.notEmpty(tlsVersion)) {
                String lowerCase = StringUtil.toLowerCase(tlsVersion);
                String[] lowerCaseSplit = lowerCase.split(",");
                httpsParam.setTlsVersion(Arrays.asList(lowerCaseSplit));
            }
            CDN.UpdateCdnConfigRequest req = new CDN.UpdateCdnConfigRequest()
                    .setDomain(cdnDomain.getDomainName())
                    .setHTTPS(httpsParam);
            CDN.UpdateCdnConfigResponse resp = service.updateCdnConfig(req);
            dealResponse(JSON.toJSONString(resp));
        } catch (Exception e) {
            log.error("修改TLS版本配置和HTTP/2配置和OCSP Stapling配置，域名信息：{}", cdnDomain);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        } finally {
            service.destroy();
        }
    }

    @Override
    public void forcedToJump(CdnDomain cdnDomain, DomainHttpsSettingVo config, String redirectCode) throws BusinessException {
        CDNService service = CDNServiceImpl.getInstance();
        service.setAccessKey(VolcengineCdn.AK);
        service.setSecretKey(VolcengineCdn.SK);
        try {
            ForceRedirectConfigDTO forceRedirect = config.getForceRedirect();
            String status = forceRedirect.getStatus();
            boolean enableForceRedirect = ObjectUtil.equal(status, "on") ? true : false;
            String type = forceRedirect.getType();
            Integer myRedirectCode = forceRedirect.getRedirect_code();
            // 设置https跳转
            CDN.HttpForcedRedirect httpForcedRedirect = new CDN.HttpForcedRedirect();
            CDN.ForcedRedirect forcedRedirect = new CDN.ForcedRedirect();
            if (ObjectUtil.equal(type, "http")) {
                httpForcedRedirect.setEnableForcedRedirect(true);
                httpForcedRedirect.setStatusCode(StringUtil.toString(myRedirectCode));
            } else if (ObjectUtil.equal(type, "https")) {
                forcedRedirect.setEnableForcedRedirect(true);
                forcedRedirect.setStatusCode(StringUtil.toString(myRedirectCode));
            }
            CDN.HTTPS https = new CDN.HTTPS();
            https.setForcedRedirect(new CDN.ForcedRedirect().setEnableForcedRedirect(enableForceRedirect).setStatusCode(StringUtil.toString(redirectCode)));
            CDN.UpdateCdnConfigRequest req = new CDN.UpdateCdnConfigRequest()
                    .setDomain(cdnDomain.getDomainName())
                    .setHttpForcedRedirect(httpForcedRedirect)
                    .setHTTPS(new CDN.HTTPS().setForcedRedirect(forcedRedirect));
            CDN.UpdateCdnConfigResponse resp = service.updateCdnConfig(req);
            dealResponse(JSON.toJSONString(resp));
        } catch (Exception e) {
            log.error("修改IP黑白名单配置，域名信息：{}", cdnDomain);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        } finally {
            service.destroy();
        }
    }

    /**
     * 优先级从上到下
     *
     * @param cdnDomain
     * @param config
     * @throws BusinessException
     */
    @Override
    public void saveCacheRules(CdnDomain cdnDomain, SettingCacheVo config) throws BusinessException {
        CDNService service = CDNServiceImpl.getInstance();
        service.setAccessKey(VolcengineCdn.AK);
        service.setSecretKey(VolcengineCdn.SK);
        try {
            List<CacheRuleDTO> cacheRules = config.getCacheRules();
            List<CDN.CacheControlRule> caches = new ArrayList<>();
            for (CacheRuleDTO cacheRule : cacheRules) {
                String object = "";
                String value = "";
                // 全路径
                if (ObjectUtil.equal(cacheRule.getMatch_type(), "full_path")) {
                    object = "path";
                    value = cacheRule.getMatch_value();
                } else if (ObjectUtil.equal(cacheRule.getMatch_type(), "catalog")) {
                    // 目录路径
                    object = "directory";

                    value = String.join("/;", Arrays.stream(cacheRule.getMatch_value().split(";")).collect(Collectors.toList())) + "/";

                } else if (ObjectUtil.equal(cacheRule.getMatch_type(), "file_extension")) {
                    object = "filetype";
                    value = cacheRule.getMatch_value().replace(".", "");
                } else {
                    // 全部文件
                    if (ObjectUtil.equal(cacheRule.getMatch_type(), "all")) {
                        object = "directory";
                        value = "/";
                    }
                }
                CDN.CacheControlRule cacheControlRule = new CDN.CacheControlRule()
                        .setCondition(new CDN.Condition()
                                .setConditionRule(Arrays.asList(new CDN.ConditionRule()
                                        .setObject(object)
                                        .setType("url")
                                        .setOperator("match")
                                        .setValue(value)
                                )))
                        .setCacheAction(new CDN.CacheAction().setAction("cache").setTtl(KuocaiBaseUtil.toSeconds(cacheRule.getTtl(), cacheRule.getTtl_unit())));
                caches.add(cacheControlRule);

            }
            CDN.UpdateCdnConfigRequest req = new CDN.UpdateCdnConfigRequest()
                    .setDomain(cdnDomain.getDomainName())
                    .setCache(caches);
            CDN.UpdateCdnConfigResponse resp = service.updateCdnConfig(req);
            dealResponse(JSON.toJSONString(resp));
        } catch (Exception e) {
            log.error("修改缓存配制失败，域名信息：{}", cdnDomain);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        } finally {
            service.destroy();
        }
    }

    @Override
    public void saveCacheFollowOriginStatusSwitch(CdnDomain cdnDomain, SettingCacheVo config) throws BusinessException {

    }

    @Override
    public void saveErrorCodeCache(CdnDomain cdnDomain, SettingCacheVo config) throws BusinessException {
        CDNService service = CDNServiceImpl.getInstance();
        service.setAccessKey(VolcengineCdn.AK);
        service.setSecretKey(VolcengineCdn.SK);
        try {
            List<ErrorCodeCacheDTO> errorCodeCaches = config.getErrorCodeCache();
            List<CDN.NegativeCache> negativeCaches = new ArrayList<>();
            for (ErrorCodeCacheDTO errorCodeCach : errorCodeCaches) {
                CDN.NegativeCache negativeCache = new CDN.NegativeCache();
                negativeCache.setCondition(null);
                negativeCache.setNegativeCacheRule(new CDN.NegativeCacheAction()
                        .setAction("cache")
                        .setStatusCode(errorCodeCach.getCode().toString())
                        .setTtl(errorCodeCach.getTtl().longValue())
                );
                negativeCaches.add(negativeCache);
            }
            CDN.UpdateCdnConfigRequest req = new CDN.UpdateCdnConfigRequest()
                    .setDomain(cdnDomain.getDomainName())
                    .setNegativeCache(negativeCaches);
            CDN.UpdateCdnConfigResponse resp = service.updateCdnConfig(req);
            dealResponse(JSON.toJSONString(resp));
        } catch (Exception e) {
            log.error("修改状态码缓存配置，域名信息：{}", cdnDomain);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        } finally {
            service.destroy();
        }
    }


    @Override
    public void saveHotlinkPrevention(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        CDNService service = CDNServiceImpl.getInstance();
        service.setAccessKey(VolcengineCdn.AK);
        service.setSecretKey(VolcengineCdn.SK);
        try {
            Boolean switchParam = null;
            String ruleType = null;
            RefererDTO referer = config.getReferer();
            Integer refererType = referer.getReferer_type();
            if (ObjectUtil.equal(refererType, 0)) {
                switchParam = false;
            } else if (ObjectUtil.equal(refererType, 1)) {
                switchParam = true;
                ruleType = "deny";
            } else if (ObjectUtil.equal(refererType, 2)) {
                switchParam = true;
                ruleType = "allow";
            }
            Boolean includeEmpty = referer.getInclude_empty();
            List<String> referers = referer.getReferers();
            CDN.UpdateCdnConfigRequest req = new CDN.UpdateCdnConfigRequest()
                    .setDomain(cdnDomain.getDomainName()).setRefererAccessRule(
                            new CDN.RefererAccessRule()
                                    .setSwitch(switchParam)
                                    .setRuleType(ruleType)
                                    .setReferers(referers)
                                    .setAllowEmpty(includeEmpty));
            CDN.UpdateCdnConfigResponse resp = service.updateCdnConfig(req);
            dealResponse(JSON.toJSONString(resp));
        } catch (Exception e) {
            log.error("修改Referer配置，域名信息：{}", cdnDomain);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        } finally {
            service.destroy();
        }
    }

    @Override
    public void saveIpBlackWhiteList(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        CDNService service = CDNServiceImpl.getInstance();
        service.setAccessKey(VolcengineCdn.AK);
        service.setSecretKey(VolcengineCdn.SK);
        try {
            Boolean switchParam = null;
            String ruleType = null;
            Integer type = config.getType();
            List<String> ips = config.getIps();
            if (ObjectUtil.equal(type, 0)) {
                switchParam = false;
            } else if (ObjectUtil.equal(type, 1)) {
                switchParam = true;
                ruleType = "deny";
            } else if (ObjectUtil.equal(type, 2)) {
                switchParam = true;
                ruleType = "allow";
            }
            CDN.UpdateCdnConfigRequest req = new CDN.UpdateCdnConfigRequest()
                    .setDomain(cdnDomain.getDomainName()).setIpAccessRule(
                            new CDN.IpAccessRule()
                                    .setSwitch(switchParam)
                                    .setRuleType(ruleType)
                                    .setIp(ips));
            CDN.UpdateCdnConfigResponse resp = service.updateCdnConfig(req);
            dealResponse(JSON.toJSONString(resp));
        } catch (Exception e) {
            log.error("修改IP黑白名单配置，域名信息：{}", cdnDomain);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        } finally {
            service.destroy();
        }
    }

    @Override
    public void saveUserAgentFilter(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        CDNService service = CDNServiceImpl.getInstance();
        service.setAccessKey(VolcengineCdn.AK);
        service.setSecretKey(VolcengineCdn.SK);
        try {
            Boolean switchParam = null;
            String ruleType = null;
            UserAgentBlackAndWhiteListDTO userAgentBlackAndWhiteListDTO = config.getUserAgentBlackAndWhiteListDTO();
            Integer type = userAgentBlackAndWhiteListDTO.getType();
            List<String> uaList = userAgentBlackAndWhiteListDTO.getUa_list();
            if (ObjectUtil.equal(type, 0)) {
                switchParam = false;
            } else if (ObjectUtil.equal(type, 1)) {
                switchParam = true;
                ruleType = "deny";
            } else if (ObjectUtil.equal(type, 2)) {
                switchParam = true;
                ruleType = "allow";
            }
            CDN.UpdateCdnConfigRequest req = new CDN.UpdateCdnConfigRequest()
                    .setDomain(cdnDomain.getDomainName())
                    .setUaAccessRule(new CDN.UserAgentAccessRule()
                            .setSwitch(switchParam)
                            .setRuleType(ruleType)
                            .setUserAgent(uaList)
                            .setAllowEmpty(false));
            CDN.UpdateCdnConfigResponse resp = service.updateCdnConfig(req);
            dealResponse(JSON.toJSONString(resp));
        } catch (Exception e) {
            log.error("修改IP黑白名单配置，域名信息：{}", cdnDomain);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        } finally {
            service.destroy();
        }
    }

    @Override
    public void saveUrlAuth(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {

    }

    @Override
    public DomainVerifyRecordInfo createVerifyRecord(String domainName) throws BusinessException {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("Domain", domainName);
            JSONObject result = VolcengineRequest.doRequest("DescribeRetrieveInfo", body);
            String host = result.getString("Host");
            String recordType = result.getString("RecordType");
            String recordValue = result.getString("RecordValue");
            String verifyDomain = result.getString("VerifyDomain");
            if (Assert.isEmpty(host) || Assert.isEmpty(recordType) || Assert.isEmpty(recordValue)) {
                throw new BusinessException("火山云未返回完整的域名归属验证记录");
            }
            if (Assert.isEmpty(verifyDomain)) {
                verifyDomain = domainName;
            }
            return DomainVerifyRecordInfo.builder()
                    .subDomain(host)
                    .recordType(recordType)
                    .record(recordValue)
                    .fileVerifyDomains(new String[]{verifyDomain})
                    .fileVerifyUrl("#")
                    .fileVerifyName("火山云仅支持DNS验证")
                    .content(recordValue)
                    .build();
        } catch (Exception e) {
            throw new BusinessException(e.getMessage());
        }
    }

    @Override
    public void verifyDomainRecord(String domainName, String verifyType) throws BusinessException {
        if (!Assert.isEmpty(verifyType) && !"dns".equalsIgnoreCase(verifyType) && !"dnsCheck".equalsIgnoreCase(verifyType)) {
            throw new BusinessException("火山云域名归属验证仅支持DNS验证");
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("Domain", domainName);
            JSONObject result = VolcengineRequest.doRequest("CheckCdnDomain", body);
            String status = result.getString("Status");
            if ("success".equalsIgnoreCase(status) || "pass".equalsIgnoreCase(status) || "ok".equalsIgnoreCase(status)) {
                return;
            }
            String errorMsg = result.getString("ErrorMsg");
            throw new BusinessException(Assert.isEmpty(errorMsg) ? "火山云域名归属验证未通过，请检查TXT记录后重试" : errorMsg);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(e.getMessage());
        }
    }

    @Override
    public void saveHttpHeader(CdnDomain cdnDomain, SettingHigherVo config) throws BusinessException {
        CDNService service = CDNServiceImpl.getInstance();
        service.setAccessKey(VolcengineCdn.AK);
        service.setSecretKey(VolcengineCdn.SK);
        try {
            List<HttpResponseHeaderDTO> httpResponseHeaders = config.getHttpResponseHeaders();
            List<CDN.ResponseHeaderInstance> responseHeaderInstances = new ArrayList<>();
            for (HttpResponseHeaderDTO httpResponseHeader : httpResponseHeaders) {
                CDN.ResponseHeaderInstance responseHeaderInstance = new CDN.ResponseHeaderInstance();
                responseHeaderInstance.setAction(httpResponseHeader.getAction());
                responseHeaderInstance.setKey(httpResponseHeader.getName());
                responseHeaderInstance.setValue(httpResponseHeader.getValue());
                responseHeaderInstance.setValueType("constant");
                responseHeaderInstances.add(responseHeaderInstance);
            }
            CDN.UpdateCdnConfigRequest req = new CDN.UpdateCdnConfigRequest()
                    .setDomain(cdnDomain.getDomainName())
                    .setResponseHeader(Arrays.asList(new CDN.ResponseHeaderRule()
                            .setResponseHeaderAction(new CDN.ResponseHeaderAction()
                                    .setResponseHeaderInstances(responseHeaderInstances))
                    ));
            CDN.UpdateCdnConfigResponse resp = service.updateCdnConfig(req);
            dealResponse(JSON.toJSONString(resp));
        } catch (Exception e) {
            log.error("修改HTTP响应头配置，域名信息：{}", cdnDomain);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        } finally {
            service.destroy();
        }
    }

    /**
     * 火山这个功能好像还不能使用Message=配置项是白名单/Beta功能: CustomErrorPage。
     *
     * @param cdnDomain
     * @param config
     * @throws BusinessException
     */
    @Override
    public void saveCustomErrorPageConfiguration(CdnDomain cdnDomain, SettingHigherVo config) throws BusinessException {
        CDNService service = CDNServiceImpl.getInstance();
        service.setAccessKey(VolcengineCdn.AK);
        service.setSecretKey(VolcengineCdn.SK);
        try {
            List<ErrorCodeRedirectRulesDTO> errorCodeRedirectRules = config.getErrorCodeRedirectRules();
            List<CDN.ErrorPageRule> rules = new ArrayList<>();
            for (ErrorCodeRedirectRulesDTO errorCodeRedirectRule : errorCodeRedirectRules) {
                CDN.ErrorPageRule errorPageRule = new CDN.ErrorPageRule();
                errorPageRule.setErrorPageAction(new CDN.ErrorPageAction()
                        .setAction("'redirect'")
                        .setStatusCode(errorCodeRedirectRule.getError_code())
                        .setRedirectCode(errorCodeRedirectRule.getTarget_code())
                        .setRedirectUrl(errorCodeRedirectRule.getTarget_link()));
                rules.add(errorPageRule);
            }
            CDN.UpdateCdnConfigRequest req = new CDN.UpdateCdnConfigRequest()
                    .setDomain(cdnDomain.getDomainName())
                    .setCustomErrorPage(new CDN.CustomErrorPage().setSwitch(true).setErrorPageRule(rules));
            CDN.UpdateCdnConfigResponse resp = service.updateCdnConfig(req);
            dealResponse(JSON.toJSONString(resp));
        } catch (Exception e) {
            log.error("修改CustomErrorPage配置，域名信息：{}", cdnDomain);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        } finally {
            service.destroy();
        }
    }

    @Override
    public void saveCompress(CdnDomain cdnDomain, SettingHigherVo config) throws BusinessException {
        CDNService service = CDNServiceImpl.getInstance();
        service.setAccessKey(VolcengineCdn.AK);
        service.setSecretKey(VolcengineCdn.SK);
        try {
            CompressDTO compress = config.getCompress();
            String status = compress.getStatus();
            boolean switchStatus = ObjectUtil.equal("on", status) ? true : false;
            String type = compress.getType();
            List<String> types = Arrays.asList(type.split(","));
            CDN.UpdateCdnConfigRequest req = new CDN.UpdateCdnConfigRequest()
                    .setDomain(cdnDomain.getDomainName()).setCompression(new CDN.Compression()
                            .setSwitch(switchStatus).setCompressionRules(Arrays.asList(
                                    new CDN.CompressionRule().setCompressionAction(
                                            new CDN.CompressionAction()
                                                    .setCompressionType(types)
                                                    .setCompressionTarget("*")
                                    ))));
            CDN.UpdateCdnConfigResponse resp = service.updateCdnConfig(req);
            dealResponse(JSON.toJSONString(resp));
        } catch (Exception e) {
            log.error("修改高级配置智能压缩，域名信息：{}", cdnDomain);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        } finally {
            service.destroy();
        }
    }

    /**
     * 获取火山云的全量信息
     */
    public DomainConfig getDomainConfig(String domainName) throws BusinessException {
        CDNService service = CDNServiceImpl.getInstance();
        try {

            service.setAccessKey(VolcengineCdn.AK);
            service.setSecretKey(VolcengineCdn.SK);
            CDN.DescribeCdnConfigRequest req = new CDN.DescribeCdnConfigRequest()
                    .setDomain(domainName);

            CDN.DescribeCdnConfigResponse resp = service.describeCdnConfig(req);
            JSONObject response = dealResponse(JSON.toJSONString(resp));
            JSONObject domainConfig = response.getJSONObject("DomainConfig");
            // 需要特殊处理
            String serviceRegion = ServiceAreaEnum.getSelfParam(domainConfig.getString("ServiceRegion"), CdnOperationRoute.VOLCENGINE);
            String serviceType = domainConfig.getString("ServiceType");
            String Status = domainConfig.getString("Status");
            String cname = domainConfig.getString("Cname");
            String domain = domainConfig.getString("Domain");
            Date createTime = domainConfig.getDate("CreateTime");
            Date updateTime = domainConfig.getDate("UpdateTime");
            JSONObject https = domainConfig.getJSONObject("HTTPS");
            String originHost = Assert.isEmpty(domainConfig.getString("OriginHost")) ? domain : domainConfig.getString("OriginHost");
            String httpsStatus = ObjectUtil.equal(https.getString("Switch"), "true") ? "1" : "0";
            JSONObject iPv6 = domainConfig.getJSONObject("IPv6");
            String ipv6Status = ObjectUtil.equal(iPv6.getString("Switch"), "true") ? "1" : "0";
            // 处理源站信息
            JSONArray originLines = domainConfig.getJSONArray("Origin").getJSONObject(0).getJSONObject("OriginAction").getJSONArray("OriginLines");
            DomainBasicInfo.SourceStationPrimaryInfo sourceStationPrimaryInfo = DomainBasicInfo.SourceStationPrimaryInfo.builder().build();
            DomainBasicInfo.SourceStationStandbyInfo sourceStationStandbyInfo = DomainBasicInfo.SourceStationStandbyInfo.builder().build();
            List<String> primaryIpOrDomains = new ArrayList<>();
            List<String> standbyIpOrDomains = new ArrayList<>();
            for (int i = 0; i < originLines.size(); i++) {
                JSONObject jsonObject = originLines.getJSONObject(i);
                // 参数转换，业务类型
                String instanceType = jsonObject.getString("InstanceType");
                String sourceStationType = OriginTypeEnum.getSelfParam(instanceType, CdnOperationRoute.VOLCENGINE);
                if (ObjectUtil.equal(jsonObject.getString("OriginType"), "primary")) {
                    primaryIpOrDomains.add(jsonObject.getString("Address"));
                    sourceStationPrimaryInfo.setSourceStationType(sourceStationType);
                    sourceStationPrimaryInfo.setHttpPort(jsonObject.getString("HttpPort"));
                    sourceStationPrimaryInfo.setHttpsPort(jsonObject.getString("HttpsPort"));
                    sourceStationPrimaryInfo.setSourceHost(Assert.isEmpty(jsonObject.getString("OriginHost")) ? originHost : jsonObject.getString("OriginHost"));
                } else if (ObjectUtil.equal(jsonObject.getString("OriginType"), "backup")) {
                    standbyIpOrDomains.add(jsonObject.getString("Address"));
                    sourceStationStandbyInfo.setSourceStationType(sourceStationType);
                    sourceStationStandbyInfo.setHttpPort(jsonObject.getString("HttpPort"));
                    sourceStationStandbyInfo.setHttpsPort(jsonObject.getString("HttpsPort"));
                    sourceStationStandbyInfo.setSourceHost(Assert.isEmpty(jsonObject.getString("OriginHost")) ? originHost : jsonObject.getString("OriginHost"));
                }
            }
            sourceStationPrimaryInfo.setIpOrDomain(String.join(";", primaryIpOrDomains));
            sourceStationStandbyInfo.setIpOrDomain(String.join(";", standbyIpOrDomains));

            // 处理回源配置信息
            DomainBackSourceInfo domainBackSourceInfo = new DomainBackSourceInfo();
            // 回源协议
            String originProtocol = OriginProtocolEnum.getSelfParam(domainConfig.getString("OriginProtocol"), CdnOperationRoute.VOLCENGINE);
            String originRangeStatus = domainConfig.getBoolean("OriginRange") == true ? "on" : "off";
//            String sliceEtagStatus = domainConfig.getBoolean("slice_etag_status") == true ? "yes" : "no";
            String originReceiveTimeout = "";
            // 回源超时
            JSONObject timeout = domainConfig.getJSONObject("Timeout");
            CDN.TimeoutArg timeoutArg = JSONObject.parseObject(timeout.toString(), new TypeReference<CDN.TimeoutArg>() {
            });
            if (timeoutArg.getSwitch()) {
                List<CDN.TimeoutRule> timeoutRules = timeoutArg.getTimeoutRules();
                originReceiveTimeout = timeoutRules.get(0).getTimeoutAction().getHttpTimeout().toString();
            }

            // 回源URL改写信息
//            List<DomainBackSourceInfo.BackSourceUrlChange> backSourceUrlChanges = new ArrayList<>();

//            JSONArray originRequestUrlRewrites = jsonObjectConfig.getJSONArray("origin_request_url_rewrite");
//            backSourceUrlChanges = JSONArray.parseObject(originRequestUrlRewrites.toString(), new TypeReference<List<DomainBackSourceInfo.BackSourceUrlChange>>() {
//            });
            // 高级回源信息
//            List<DomainBackSourceInfo.BackSourceAdvancedInfo> backSourceAdvancedInfos = new ArrayList<>();
//            JSONArray flexibleOrigins = jsonObjectConfig.getJSONArray("flexible_origin");
//            backSourceAdvancedInfos = JSONArray.parseObject(flexibleOrigins.toString(), new TypeReference<List<DomainBackSourceInfo.BackSourceAdvancedInfo>>() {
//            });
//
            List<DomainBackSourceInfo.BackSourceRequestInfo> backSourceRequestInfos = new ArrayList<>();
            JSONArray originRequestHeaders = domainConfig.getJSONArray("RequestHeader");
            if (Assert.notEmpty(originRequestHeaders)) {
                List<CDN.RequestHeaderRule> requestHeaderRules = JSONArray.parseObject(originRequestHeaders.toString(), new TypeReference<List<CDN.RequestHeaderRule>>() {
                });
                if (Assert.notEmpty(requestHeaderRules)) {
                    CDN.RequestHeaderRule requestHeaderRule = requestHeaderRules.get(0);
                    List<CDN.RequestHeaderInstance> requestHeaderInstances = requestHeaderRule.getRequestHeaderAction().getRequestHeaderInstances();
                    for (CDN.RequestHeaderInstance requestHeaderInstance : requestHeaderInstances) {
                        DomainBackSourceInfo.BackSourceRequestInfo backSourceRequestInfo = new DomainBackSourceInfo.BackSourceRequestInfo(requestHeaderInstance.getKey(), requestHeaderInstance.getValue(), requestHeaderInstance.getAction());
                        backSourceRequestInfos.add(backSourceRequestInfo);
                    }
                }

            }

            domainBackSourceInfo.setOrigin_protocol(originProtocol);
            domainBackSourceInfo.setOrigin_receive_timeout(originReceiveTimeout);
            domainBackSourceInfo.setOrigin_range_status(originRangeStatus);
//            domainBackSourceInfo.setSlice_etag_status(sliceEtagStatus);
//            domainBackSourceInfo.setOrigin_request_url_rewrite(backSourceUrlChanges);
//            domainBackSourceInfo.setFlexible_origin(backSourceAdvancedInfos);
            domainBackSourceInfo.setOrigin_request_header(backSourceRequestInfos);
//
            // 域名HTTPS信息
            DomainHttpsInfo domainHttpsInfo = new DomainHttpsInfo();
            CDN.HTTPS httpsCdnObj = JSONObject.parseObject(https.toJSONString(), CDN.HTTPS.class);
            // HTTPS跳转HTTP配置
            CDN.HttpForcedRedirect httpForcedRedirect = JSONObject.parseObject(domainConfig.getJSONObject("HttpForcedRedirect").toJSONString(), CDN.HttpForcedRedirect.class);


            DomainHttpsInfo.HttpGetBody httpGetBody = DomainHttpsInfo.HttpGetBody.builder()
                    .https_status(httpsCdnObj.getSwitch() == true ? "on" : "off")
                    .http2_status(httpsCdnObj.getHTTP2() == true ? "on" : "off")
                    .tls_version(String.join(";", httpsCdnObj.getTlsVersion()))
                    .ocsp_stapling_status(httpsCdnObj.getOCSP() == true ? "on" : "off")
                    .certificate_value("")
                    .certificate_source(0)
                    .certificate_type("server")
                    .build();
            // 获取证书信息
            if (httpsCdnObj.getSwitch()) {
                String certId = httpsCdnObj.getCertInfo().getCertId();
                CDN.CertInfo certInfo = getCertInfos(domainName, certId);
                httpGetBody.setCertificate_name(certInfo.getDesc());
            }
            String httpType = "";
            String redirectCode = "";
            if (httpForcedRedirect.getEnableForcedRedirect() == true) {
                httpType = "HTTP";
                redirectCode = httpForcedRedirect.getStatusCode();
            } else if (httpsCdnObj.getForcedRedirect().getEnableForcedRedirect() == true) {
                httpType = "HTTPS";
                redirectCode = httpsCdnObj.getForcedRedirect().getStatusCode();
            }
            DomainHttpsInfo.ForceRedirect forceRedirect = DomainHttpsInfo.ForceRedirect.builder()
                    .status((httpsCdnObj.getForcedRedirect().getEnableForcedRedirect() || httpForcedRedirect.getEnableForcedRedirect()) == true ? "on" : "off")
                    .type(httpType)
                    .redirect_code(redirectCode)
                    .build();
            domainHttpsInfo.setHttps(httpGetBody);
            domainHttpsInfo.setForce_redirect(forceRedirect);

            // 域名缓存信息
            DomainCacheInfo domainCacheInfo = new DomainCacheInfo();
            // 缓存规则
            JSONArray caches = domainConfig.getJSONArray("Cache");
            List<DomainCacheInfo.CacheRule> cacheRules = new ArrayList<>();
            if (Assert.notEmpty(caches)) {
                for (int i = 0; i < caches.size(); i++) {
                    JSONObject cacheJson = caches.getJSONObject(i);
                    JSONObject condition = cacheJson.getJSONObject("Condition");
                    JSONObject conditionRule = condition.getJSONArray("ConditionRule").getJSONObject(0);
                    JSONObject cacheAction = cacheJson.getJSONObject("CacheAction");
                    String object = conditionRule.getString("Object");
                    String value = conditionRule.getString("Value");
                    if (ObjectUtil.equal(object, "path")) {
                        object = "full_path";
                    } else if (ObjectUtil.equal(object, "directory")) {
                        if (ObjectUtil.equal("/", value)) {
                            object = "all";
                            value = "";
                        } else {
                            // 目录路径
                            object = "catalog";
                            value = value.substring(0, value.lastIndexOf("/"));
                            List<String> collect = Arrays.stream(value.split("/;")).collect(Collectors.toList());
                            value = String.join(";", collect);
                        }
                    } else if (ObjectUtil.equal(object, "filetype")) {
                        object = "file_extension";
                        List<String> collect = Arrays.stream(value.split(";")).collect(Collectors.toList());
                        value = "." + String.join(";.", collect);
                    }
                    DomainCacheInfo.CacheRule cacheRuleObj = new DomainCacheInfo.CacheRule();
                    cacheRuleObj.setMatch_type(object);
                    cacheRuleObj.setMatch_value(value);
//                    cacheRuleObj.setPriority(caches.size() - i);
                    // TODO 自动转化单位
                    Integer ttl = cacheAction.getInteger("Ttl");
                    cacheRuleObj.setTtl(KuocaiBaseUtil.getUnitCacheTime(ttl));
                    cacheRuleObj.setTtl_unit(KuocaiBaseUtil.getCacheTimeUnit(ttl));
                    cacheRules.add(cacheRuleObj);
                }
            }

            JSONArray negativeCaches = domainConfig.getJSONArray("NegativeCache");
            List<DomainCacheInfo.ErrorCodeCache> errorCodeCaches = new ArrayList<>();
            if (Assert.notEmpty(negativeCaches)) {
                for (Object negativeCach : negativeCaches) {
                    JSONObject negativeCacheJson = (JSONObject) negativeCach;
                    JSONObject negativeCacheRule = negativeCacheJson.getJSONObject("NegativeCacheRule");
                    Integer statusCode = negativeCacheRule.getInteger("StatusCode");
                    Integer ttl = negativeCacheRule.getInteger("Ttl");
                    DomainCacheInfo.ErrorCodeCache errorCodeCache = new DomainCacheInfo.ErrorCodeCache();
                    errorCodeCache.setCode(statusCode);
                    errorCodeCache.setTtl(ttl);
                    errorCodeCaches.add(errorCodeCache);
                }
            }
            domainCacheInfo.setCache_rules(cacheRules);
            domainCacheInfo.setError_code_cache(errorCodeCaches);

            // 域名访问信息
            DomainVisitInfo domainVisitInfo = new DomainVisitInfo();
            JSONObject refererAccessRuleJson = domainConfig.getJSONObject("RefererAccessRule");
            JSONObject ipAccessRuleJson = domainConfig.getJSONObject("IpAccessRule");
            JSONObject uaAccessRuleJson = domainConfig.getJSONObject("UaAccessRule");
            Integer refererType = 0;
            if (Assert.notEmpty(refererAccessRuleJson)) {
                CDN.RefererAccessRule refererAccessRule = JSONObject.parseObject(refererAccessRuleJson.toJSONString(), CDN.RefererAccessRule.class);
                if (refererAccessRule.getSwitch() == false) {
                    refererType = 0;
                } else {
                    if (ObjectUtil.equal(refererAccessRule.getRuleType(), "allow")) {
                        refererType = 2;
                    } else if (ObjectUtil.equal(refererAccessRule.getRuleType(), "deny")) {
                        refererType = 1;
                    }
                }
                String refererValue = refererAccessRule.getReferers().stream()
                        .collect(Collectors.joining(System.lineSeparator()));

                DomainVisitInfo.Referer referer = DomainVisitInfo.Referer.builder()
                        .include_empty(refererAccessRule.getAllowEmpty())
                        .referer_type(refererType)
                        .value(refererValue)
                        .build();
                domainVisitInfo.setReferer(referer);
            } else {
                DomainVisitInfo.Referer referer = DomainVisitInfo.Referer.builder()
                        .referer_type(refererType)
                        .include_empty(false)
                        .build();
                domainVisitInfo.setReferer(referer);
            }

            String ipType = "off";
            if (Assert.notEmpty(ipAccessRuleJson)) {
                CDN.IpAccessRule ipAccessRule = JSONObject.parseObject(ipAccessRuleJson.toJSONString(), CDN.IpAccessRule.class);
                if (ipAccessRule.getSwitch() == false) {
                    ipType = "off";
                } else {
                    if (ObjectUtil.equal(ipAccessRule.getRuleType(), "allow")) {
                        ipType = "white";
                    } else if (ObjectUtil.equal(ipAccessRule.getRuleType(), "deny")) {
                        ipType = "black";
                    }
                }
                String ipValue = ipAccessRule.getIp().stream()
                        .collect(Collectors.joining(System.lineSeparator()));
                DomainVisitInfo.IpFilter ipFilter = DomainVisitInfo.IpFilter.builder()
                        .type(ipType)
                        .value(ipValue)
                        .build();
                domainVisitInfo.setIp_filter(ipFilter);
            } else {
                DomainVisitInfo.IpFilter ipFilter = DomainVisitInfo.IpFilter.builder()
                        .type(ipType)
                        .build();
                domainVisitInfo.setIp_filter(ipFilter);
            }
            String uaType = "off";
            if (Assert.notEmpty(uaAccessRuleJson)) {
                CDN.UserAgentAccessRule userAgentAccessRule = JSONObject.parseObject(uaAccessRuleJson.toJSONString(), CDN.UserAgentAccessRule.class);
                String value = "";
                if (userAgentAccessRule.getSwitch() == false) {
                    uaType = "off";
                } else {
                    if (ObjectUtil.equal(userAgentAccessRule.getRuleType(), "allow")) {
                        uaType = "white";
                    } else if (ObjectUtil.equal(userAgentAccessRule.getRuleType(), "deny")) {
                        uaType = "black";
                    }
                    value = userAgentAccessRule.getUserAgent().stream()
                            .collect(Collectors.joining(System.lineSeparator()));
                }
                DomainVisitInfo.UserAgentFilter userAgentFilter = DomainVisitInfo.UserAgentFilter.builder()
                        .type(uaType)
                        .value(value)
                        .ua_list(userAgentAccessRule.getUserAgent())
                        .build();
                domainVisitInfo.setUser_agent_filter(userAgentFilter);
            } else {
                DomainVisitInfo.UserAgentFilter userAgentFilter = DomainVisitInfo.UserAgentFilter.builder()
                        .type(uaType)
                        .build();
                domainVisitInfo.setUser_agent_filter(userAgentFilter);
            }

            // 域名高级信息
            DomainAdvancedInfo domainAdvancedInfo = new DomainAdvancedInfo();
            List<DomainAdvancedInfo.HttpResponseHeader> httpResponseHeaders = new ArrayList<>();
            JSONArray responseHeaders = domainConfig.getJSONArray("ResponseHeader");
            if (Assert.notEmpty(responseHeaders)) {
                List<CDN.ResponseHeaderRule> responseHeaderRules = JSONArray.parseObject(responseHeaders.toString(), new TypeReference<List<CDN.ResponseHeaderRule>>() {
                });
                if (Assert.notEmpty(responseHeaderRules)) {
                    CDN.ResponseHeaderRule responseHeaderRule = responseHeaderRules.get(0);
                    List<CDN.ResponseHeaderInstance> responseHeaderInstances = responseHeaderRule.getResponseHeaderAction().getResponseHeaderInstances();
                    for (CDN.ResponseHeaderInstance responseHeaderInstance : responseHeaderInstances) {
                        DomainAdvancedInfo.HttpResponseHeader httpResponseHeader = new DomainAdvancedInfo.HttpResponseHeader(responseHeaderInstance.getKey(), responseHeaderInstance.getValue(), responseHeaderInstance.getAction());
                        httpResponseHeaders.add(httpResponseHeader);
                    }
                }
            }
            domainAdvancedInfo.setHttp_response_header(httpResponseHeaders);
            JSONObject compression = domainConfig.getJSONObject("Compression");
            DomainAdvancedInfo.Compress compress = new DomainAdvancedInfo.Compress();
            String compressStatus = "off";
            if (Assert.notEmpty(compression)) {
                CDN.Compression compressionCdnObj = JSONObject.parseObject(compression.toJSONString(), CDN.Compression.class);
                Boolean aSwitch = compressionCdnObj.getSwitch();
                if (aSwitch) {
                    compressStatus = "on";
                    compress.setType(String.join(",", compressionCdnObj.getCompressionRules().get(0).getCompressionAction().getCompressionType()));
                } else {
                    compressStatus = "off";
                }
            }
            compress.setStatus(compressStatus);
            domainAdvancedInfo.setCompress(compress);
            // TODO 自定义错误页面配置
            return DomainConfig.builder()
                    .domainBasicInfo(
                            DomainBasicInfo.builder().
                                    domainName(domain)
                                    .cname(cname)
                                    .domainStatus(Status)
                                    .businessType(serviceType)
                                    .serviceArea(serviceRegion)
                                    .httpsStatus(httpsStatus)
                                    .isIpv6(ipv6Status)
                                    .createTime(createTime)
                                    .updateTime(updateTime)
                                    .sourceStationPrimaryInfo(sourceStationPrimaryInfo)
                                    .sourceStationStandbyInfo(sourceStationStandbyInfo)
                                    .build())
                    .domainBackSourceInfo(domainBackSourceInfo)
                    .domainHttpsInfo(domainHttpsInfo)
                    .domainCacheInfo(domainCacheInfo)
                    .domainVisitInfo(domainVisitInfo)
                    .domainAdvancedInfo(domainAdvancedInfo)
                    .build();
        } catch (Exception e) {
            throw new BusinessException(e.getMessage()).setCause(e).log();
        } finally {
            service.destroy();
        }

    }

    public CDN.CertInfo getCertInfos(String domainName, String certId) throws BusinessException {
        CDNService service = CDNServiceImpl.getInstance();
        service.setAccessKey(VolcengineCdn.AK);
        service.setSecretKey(VolcengineCdn.SK);
        try {
            CDN.ListCertInfoRequest req = new CDN.ListCertInfoRequest()
                    .setName(domainName)
                    .setSource("volc_cert_center")
                    .setPageSize(Long.valueOf(Integer.MAX_VALUE));
            CDN.ListCertInfoResponse resp = service.listCertInfo(req);
            dealResponse(JSON.toJSONString(resp));
            List<CDN.CertInfo> certInfos = resp.getResult().getCertInfo();
            Optional<CDN.CertInfo> first = certInfos.stream().filter(item -> ObjectUtil.equal(item.getCertId(), certId)).findFirst();
            if (first.isPresent()) {
                return first.get();
            } else {
                log.error("查询域名证书信息为空，域名信息：{}", domainName);
                throw new BusinessException("系统异常");
            }
        } catch (Exception e) {
            log.error("查询域名证书信息，域名信息：{}", domainName);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        } finally {
            service.destroy();
        }
    }

    public JSONObject dealResponse(String response) throws BusinessException {
        JSONObject responseObject = JSONObject.parseObject(response);
        JSONObject responseMetadata = responseObject.getJSONObject("ResponseMetadata");
        if (responseMetadata.containsKey("Error")) {
            JSONObject error = responseMetadata.getJSONObject("Error");
            throw new BusinessException(error.getString("Message"));
        } else {
            if (responseObject.containsKey("Result")) {
                return responseObject.getJSONObject("Result");
            }
        }
        return null;
    }

    public static void main(String[] args) throws BusinessException {
        VolCenGineDomainServiceImpl service = new VolCenGineDomainServiceImpl();
        service.getDomainConfig("wps.cdn.haozi.net");
    }
}
