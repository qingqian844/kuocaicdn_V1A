package com.kuocai.cdn.service.domain.operation;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.api.DomainAdvancedInfo;
import com.kuocai.cdn.api.DomainBasicInfo;
import com.kuocai.cdn.api.DomainConfig;
import com.kuocai.cdn.api.huawei.cdn.dto.*;
import com.kuocai.cdn.api.qiniu.cdn.QiNiuDomainOperationApi;
import com.kuocai.cdn.api.qiniu.cdn.vo.CacheVo;
import com.kuocai.cdn.api.qiniu.cdn.vo.QiniuDomainVo;
import com.kuocai.cdn.api.qiniu.cdn.vo.SourceVo;
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
import com.kuocai.cdn.enumeration.domainmerage.dtoenum.*;
import com.kuocai.cdn.enumeration.domainmerage.route.CdnOperationRoute;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.exception.CdnHuaweiException;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.KuocaiBaseUtil;
import com.kuocai.cdn.util.ValidatorUtils;
import com.kuocai.cdn.vo.*;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.volcengine.model.beans.CDN;
import com.volcengine.service.cdn.CDNService;
import com.volcengine.service.cdn.impl.CDNServiceImpl;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 火山云加速域名(CdnDomain)服务
 */
@Slf4j
@Service
public class QiNiuDomainServiceImpl extends BaseService<CdnDomain> implements ICdnPlatformService {


    @Override
    @Transactional(rollbackFor = {Exception.class})
    public CdnDomain create(Long userId, String domainName, String businessType, String serviceArea, String originType, String ipOrDomain) throws BusinessException {
        if ("video".equals(businessType)) {
            businessType = "vod";
        }
        if ("mainland_china".equals(serviceArea)) {
            serviceArea = "china";
        }
        String qiNiu = OriginTypeEnum.getOtherParam(originType).getQiNiu();
        String[] ipOrDomains = ipOrDomain.split(";");
        List<SourceVo.AdvancedSources> advancedSourceList = new ArrayList<>();
        for (String s : ipOrDomains) {
            SourceVo.AdvancedSources advancedSources = new SourceVo.AdvancedSources();
            advancedSources.setAddr(s);
            advancedSources.setBackup(false);
            advancedSources.setWeight(1);
            advancedSourceList.add(advancedSources);
        }
        SourceVo source = SourceVo.builder()
                .sourceType("advanced")
                .advancedSources(advancedSourceList)
                .testURLPath("/archives/centos7da-jian-kubernetesji-qun")
                .build();
        // 默认设置全局配置,30天
        CacheVo cacheVo = CacheVo.builder()
                .cacheControls(Arrays.asList(CacheVo.CacheControls.builder()
                        .type("all")
                        .rule("*")
                        .time(1)
                        .timeunit(5).build()))
                .build();
        QiniuDomainVo domainVo = QiniuDomainVo.builder()
                .type("normal")
                .platform(businessType)
                .protocol("http")
                .geoCover(serviceArea)
                .IpTypes(1)
                .source(source)
                .cache(cacheVo)
                .build();
        JSONObject jsonObject = QiNiuDomainOperationApi.create(domainName, domainVo);
        DomainBasicInfo domainBasicInfo = new DomainBasicInfo();
        try {
            DomainConfig domainConfig = getDomainConfig(domainName);
            domainBasicInfo = domainConfig.getDomainBasicInfo();
        } catch (Exception e) {
            throw new BusinessException("查询域名详细信息错误:" + e.getMessage()).setCause(e);
        }
        CdnDomain cdnDomain = CdnDomain.builder()
                .domainName(domainName)
                .businessType(domainBasicInfo.getBusinessType())
//                .serviceArea(domainBasicInfo.getServiceArea())
                .serviceArea(ServiceAreaDtoEnum.getSelfParam(serviceArea, CdnOperationRoute.QINIU))
                .domainStatus(domainBasicInfo.getDomainStatus())
//                .cnameQiniu(domainBasicInfo.getCname())
                .userId(userId)
                .route(CdnRoute.QINIU.getCode())
                .build();
        return cdnDomain;
    }

    @Override
    public CdnDomain configDNS(CdnDomain cdnDomain) throws TencentCloudSDKException, BusinessException {
        String domainName = cdnDomain.getDomainName();
//        String cnameQiniu = cdnDomain.getCnameQiniu();
        String cnameQiniu = "";
        CreateRecordDTO createRecordRequest = new CreateRecordDTO();
        createRecordRequest.setDomain(TencentDns.LOCAL_DOMAIN_NAME).setSubDomain(domainName + "." + RandomUtil.randomString(8)).setValue(cnameQiniu);
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
        try {
            int ipv6 = ObjectUtil.equal(status, 1) ? 3 : 1;
           QiNiuDomainOperationApi.ipv6(cdnDomain.getDomainName(), ipv6);
        } catch (Exception e) {
            throw new CdnHuaweiException(e.getMessage()).setCause(e).log();
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
    public void forcedToJump(CdnDomain cdnDomain, DomainHttpsSettingVo config,String redirectCode) throws BusinessException {
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


    @Override
    public void saveCacheRules(CdnDomain cdnDomain, SettingCacheVo config) throws BusinessException {
        CDNService service = CDNServiceImpl.getInstance();
        service.setAccessKey(VolcengineCdn.AK);
        service.setSecretKey(VolcengineCdn.SK);
        try {
            List<CacheRuleDTO> cacheRules = config.getCacheRules();
            List<CDN.CacheControlRule> caches = new ArrayList<>();
            cacheRules.sort((o1, o2) -> {
                return o1.getPriority() - o2.getPriority();
            });
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
                }
                CDN.CacheControlRule cacheControlRule = new CDN.CacheControlRule()
                        .setCondition(new CDN.Condition()
                                .setConditionRule(Arrays.asList(new CDN.ConditionRule()
                                        .setObject(object).
                                        setType("url")
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
            log.error("修改IP黑白名单配置，域名信息：{}", cdnDomain);
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
        JSONObject domainInfo = QiNiuDomainOperationApi.query(domainName);
        String qiNiuDomainName = domainInfo.getString("name");
        String status = DomainStatusDtoEnum.getSelfParam(domainInfo.getString("operatingState"), CdnOperationRoute.QINIU);
        String httpsStatus = HttpsStatusDtoEnum.getSelfParam(domainInfo.getString("protocol"), CdnOperationRoute.QINIU);
        String cname = domainInfo.getString("cname");
        String businessType = BusinessTypeDtoEnum.getSelfParam(domainInfo.getString("platform"), CdnOperationRoute.QINIU);
        String serviceArea = "";
        if(Assert.notEmpty(domainInfo.getString("geoCover"))){
            serviceArea = ServiceAreaDtoEnum.getSelfParam(domainInfo.getString("geoCover"), CdnOperationRoute.QINIU);
        }
        String ipv6 = Ipv6StatusDtoEnum.getSelfParam(domainInfo.getString("ipTypes"), CdnOperationRoute.QINIU);
        Date createAt = domainInfo.getDate("createAt");
        Date modifyAt = domainInfo.getDate("modifyAt");
        // 解析主备源站
        JSONObject source = domainInfo.getJSONObject("source");
        String sourceHost = source.getString("sourceHost");
        JSONArray originLines = source.getJSONArray("advancedSources");
        DomainBasicInfo.SourceStationPrimaryInfo sourceStationPrimaryInfo = DomainBasicInfo.SourceStationPrimaryInfo.builder().build();
        DomainBasicInfo.SourceStationStandbyInfo sourceStationStandbyInfo = DomainBasicInfo.SourceStationStandbyInfo.builder().build();
        List<String> primaryIpOrDomains = new ArrayList<>();
        List<String> standbyIpOrDomains = new ArrayList<>();
        for (int i = 0; i < originLines.size(); i++) {
            JSONObject jsonObject = originLines.getJSONObject(i);
            if (ObjectUtil.equal(jsonObject.getBoolean("backup"), false)) {
                String addr = jsonObject.getString("addr");
                if (ValidatorUtils.isIPAddress(addr)) {
                    sourceStationPrimaryInfo.setSourceStationType(OriginTypeDtoEnum.getSelfParam("ip", CdnOperationRoute.QINIU));
                } else {
                    sourceStationPrimaryInfo.setSourceStationType(OriginTypeDtoEnum.getSelfParam("domain", CdnOperationRoute.QINIU));
                }
                primaryIpOrDomains.add(addr);
//                sourceStationPrimaryInfo.setHttpPort(jsonObject.getString("HttpPort"));
//                sourceStationPrimaryInfo.setHttpsPort(jsonObject.getString("HttpsPort"));
                sourceStationPrimaryInfo.setSourceHost(sourceHost);
            } else if (ObjectUtil.equal(jsonObject.getBoolean("backup"), true)) {
                String addr = jsonObject.getString("addr");
                if (ValidatorUtils.isIPAddress(addr)) {
                    sourceStationStandbyInfo.setSourceStationType(OriginTypeDtoEnum.getSelfParam("ip", CdnOperationRoute.QINIU));
                } else {
                    sourceStationStandbyInfo.setSourceStationType(OriginTypeDtoEnum.getSelfParam("domain", CdnOperationRoute.QINIU));
                }
                standbyIpOrDomains.add(addr);
//                sourceStationStandbyInfo.setHttpPort(jsonObject.getString("HttpPort"));
//                sourceStationStandbyInfo.setHttpsPort(jsonObject.getString("HttpsPort"));
                sourceStationStandbyInfo.setSourceHost(sourceHost);
            }
        }
        sourceStationPrimaryInfo.setIpOrDomain(String.join(";", primaryIpOrDomains));
        sourceStationStandbyInfo.setIpOrDomain(String.join(";", standbyIpOrDomains));
        DomainBasicInfo domainBasicInfo = DomainBasicInfo.builder()
                .domainName(qiNiuDomainName)
                .domainStatus(status)
                .httpsStatus(httpsStatus)
                .cname(cname)
                .businessType(businessType)
                .serviceArea(serviceArea)
                .isIpv6(ipv6)
                .createTime(createAt)
                .updateTime(modifyAt)
                .sourceStationPrimaryInfo(sourceStationPrimaryInfo)
                .sourceStationStandbyInfo(sourceStationStandbyInfo)
                .build();
        DomainAdvancedInfo domainAdvancedInfo = DomainAdvancedInfo.builder().build();
        domainAdvancedInfo.setCompress(new DomainAdvancedInfo.Compress());
        domainAdvancedInfo.setError_code_redirect_rules(new ArrayList<>());
        List<DomainAdvancedInfo.HttpResponseHeader> httpResponseHeaders = new ArrayList<>();
        if(domainInfo.containsKey("responseHeaderControls")){
            JSONArray responseHeaderControls = domainInfo.getJSONArray("responseHeaderControls");
            for (Object responseHeaderControl : responseHeaderControls) {
                JSONObject jsonObject = (JSONObject) responseHeaderControl;
                String action = jsonObject.getString("op");
                String name = jsonObject.getString("key");
                String value = jsonObject.getString("value");
                DomainAdvancedInfo.HttpResponseHeader httpResponseHeader = new DomainAdvancedInfo.HttpResponseHeader();
                httpResponseHeader.setAction(action);
                httpResponseHeader.setName(name);
                httpResponseHeader.setValue(value);
                httpResponseHeaders.add(httpResponseHeader);
            }
        }
        domainAdvancedInfo.setHttp_response_header(httpResponseHeaders);
        return DomainConfig.builder()
                .domainBasicInfo(domainBasicInfo)
                .domainAdvancedInfo(domainAdvancedInfo)
                .build();
    }


    public CDN.CertInfo getCertInfos(String domainName, String certId) throws BusinessException {
        return null;
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
}
