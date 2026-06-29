package com.kuocai.cdn.service.domain.operation;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.api.*;
import com.kuocai.cdn.api.baishan.cdn.BsDomainOperationApi;
import com.kuocai.cdn.api.baishan.cdn.vo.*;
import com.kuocai.cdn.api.huawei.cdn.dto.*;
import com.kuocai.cdn.api.huawei.cdn.dto.CacheRuleDTO;
import com.kuocai.cdn.api.tencent.dns.CreateRecordResponse;
import com.kuocai.cdn.api.tencent.dns.TencentApi;
import com.kuocai.cdn.api.tencent.dns.dto.CreateRecordDTO;
import com.kuocai.cdn.api.tencent.dns.properties.TencentDns;
import com.kuocai.cdn.api.yifan.cdn.YiFanOperationApi;
import com.kuocai.cdn.api.yifan.cdn.dto.*;
import com.kuocai.cdn.api.yifan.cdn.dto.ForceRedirectDTO;
import com.kuocai.cdn.api.yifan.cdn.enums.OriginProtocol;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.entity.CdnDomainSources;
import com.kuocai.cdn.enumeration.domainmerage.CdnRoute;
import com.kuocai.cdn.enumeration.domainmerage.domain.BusinessTypeEnum;
import com.kuocai.cdn.enumeration.domainmerage.domain.DomainStatus;
import com.kuocai.cdn.enumeration.domainmerage.domain.OriginTypeEnum;
import com.kuocai.cdn.enumeration.domainmerage.domain.ServiceAreaEnum;
import com.kuocai.cdn.enumeration.domainmerage.dtoenum.*;
import com.kuocai.cdn.enumeration.domainmerage.route.CdnOperationRoute;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.CdnDomainService;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.KuocaiBaseUtil;
import com.kuocai.cdn.util.ValidatorUtils;
import com.kuocai.cdn.vo.*;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.volcengine.model.beans.CDN;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BaiShanDomainServiceImpl extends BaseService<CdnDomain> implements ICdnPlatformService {


    @Resource
    private CdnDomainService domainService;

    /**
     * 创建域名信息
     *
     * @param userId
     * @param domainName
     * @param businessType
     * @param serviceArea
     * @param originType
     * @param ipOrDomain
     * @return
     * @throws BusinessException
     * @throws InterruptedException
     */
    @Override
    public CdnDomain create(Long userId, String domainName, String businessType, String serviceArea, String originType, String ipOrDomain) throws BusinessException, InterruptedException {
        // 封装源站信息
        JSONObject domain = null;
        if (ObjectUtil.equal(originType, OriginTypeEnum.DOMAIN.getParam())) {
            if (ipOrDomain.split(";").length > 1) {
                throw new BusinessException("备源站不支持多域名");
            }
        }
        try {
            String area = ServiceAreaEnum.getOtherParam(serviceArea).getBaiShan();
            String type = BusinessTypeEnum.getOtherParam(businessType).getBaiShan();
            String defaultMaster = ipOrDomain.replace(";", ",");
            BsDomainVo.BsBackSourceConfig config = BsDomainVo.BsBackSourceConfig.builder()
                    .origin(BsDomainVo.BsOrigin.builder()
                            .default_master(defaultMaster)
                            .origin_mode("default")
                            .build())
                    .origin_host(BsDomainVo.BsOriginHost.builder().host(domainName).build()).build();
            BsDomainVo bsDomainVo = BsDomainVo.builder()
                    .domain(domainName)
                    .area(area)
                    .type(type)
                    .config(config)
                    .build();
            domain = BsDomainOperationApi.create(bsDomainVo);
            /** Thread.sleep(300);
            // 默认添加一个全局缓存30天
            BsCacheVo bsCacheVo = BsCacheVo.builder()
                    .domains(domainName)
                    .config(BsCacheVo.BsCacheConfig.builder()
                            .cache_rule_list(Arrays.asList(BsCacheVo.BsCacheRule.builder()
                                    .match_method("all")
                                    .pattern(".*")
                                    .expire(30)
                                    .expire_unit("D")
                                    .priority(1)
                                    .case_ignore("no")
                                    .build()))
                            .build())
                    .build();
            BsDomainOperationApi.saveCacheInfo(bsCacheVo); **/
        } catch (Exception e) {
            log.error("白山创建加速域名失败，用户：{}，域名：{}，业务类型：{}，服务范围：{}，源站类别：{}，源站：{}",
                    userId, domainName, businessType, serviceArea, originType, ipOrDomain);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
        //保存域名信息
        if (Assert.isEmpty(domain)) {
            throw new BusinessException("创建加速域名失败");
        }
        String domainIdResult = domain.getString("id");
        String domainNameResult = domain.getString("domain");
        String businessTypeResult = BusinessTypeDtoEnum.getSelfParam(domain.getString("type"), CdnOperationRoute.BAISHAN);
        String serviceAreaResult = ServiceAreaDtoEnum.getSelfParam(domain.getString("area"), CdnOperationRoute.BAISHAN);
        String status = DomainStatusDtoEnum.getSelfParam(domain.getString("status"), CdnOperationRoute.BAISHAN);
        String cname = domain.getString("cname");
        CdnDomain cdnDomain = CdnDomain.builder()
                .domainId(domainIdResult)
                .domainName(domainNameResult)
                .businessType(businessTypeResult)
                .serviceArea(serviceAreaResult)
                .domainStatus(status)
                .cnameYifan(cname)
                .userId(userId)
                .route(CdnRoute.BAISHAN.getCode())
                .build();
        return save(cdnDomain);
    }

    /*
     * <p>
     * /**
     * 配置域名CNAME的DNS解析
     *
     * @param cdnDomain
     * @return
     * @throws TencentCloudSDKException
     * @throws BusinessException
     */
    @Override
    public CdnDomain configDNS(CdnDomain cdnDomain) throws TencentCloudSDKException, BusinessException {
        String domainName = cdnDomain.getDomainName();
        String cnameYifan = cdnDomain.getCnameYifan();
        CreateRecordDTO createRecordRequest = new CreateRecordDTO();
        createRecordRequest.setDomain(TencentDns.LOCAL_DOMAIN_NAME).setSubDomain(domainName + "." + RandomUtil.randomString(8)).setValue(cnameYifan);
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
            log.error("易凡dns解析失败，域名：{}", cdnDomain.getDomainName());
            throw new BusinessException("dns解析失败");
        }
        return null;
    }

    /**
     * 修改域名的一些基础信息
     *
     * @param cdnDomain
     * @param businessType
     * @param serviceArea
     * @throws BusinessException
     */
    @Override
    public void save(CdnDomain cdnDomain, String businessType, String serviceArea) throws BusinessException {
        // TODO 修改服务类型,以及服务区域
    }

    /**
     * 停用域名
     *
     * @param cdnDomain
     * @throws BusinessException
     */
    @Override
    public void disable(CdnDomain cdnDomain) throws BusinessException {
        try {
            BsDomainOperationApi.disable(cdnDomain.getDomainName());
            cdnDomain.setDomainStatus(DomainStatus.OFFLINE.getParam());
            domainService.save(cdnDomain);
        } catch (Exception e) {
            log.error("bs停用域名失败，用户：{}，域名：{}", cdnDomain.getUserId(), cdnDomain.getDomainName());
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }

    /**
     * 启用域名
     *
     * @param cdnDomain
     * @throws BusinessException
     */
    @Override
    public void enable(CdnDomain cdnDomain) throws BusinessException {
        try {
            BsDomainOperationApi.enable(cdnDomain.getDomainName());
        } catch (Exception e) {
            log.error("bs启用域名失败，用户：{}，域名：{}", cdnDomain.getUserId(), cdnDomain.getDomainName());
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }

    /**
     * 删除域名信息
     *
     * @param cdnDomain
     * @throws BusinessException
     */
    @Override
    public void delete(CdnDomain cdnDomain) throws BusinessException {
        try {
            BsDomainOperationApi.delete(cdnDomain.getDomainName());
        } catch (Exception e) {
            log.error("bs删除域名失败，用户：{}，域名：{}", cdnDomain.getUserId(), cdnDomain.getDomainName());
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }

    /**
     * 打开域名的IPV6
     *
     * @param cdnDomain
     * @param status
     * @throws BusinessException
     */
    @Override
    public void ipv6(CdnDomain cdnDomain, Integer status) throws BusinessException {
        // TODO 不支持IPV6修改
    }

    /**
     * 保存域名的源站信息
     *
     * @param cdnDomain
     * @param config
     * @throws BusinessException
     */
    @Override
    public void saveSourceStationConfig(CdnDomain cdnDomain, CdnDomainSourcesVo config) throws BusinessException {
        CdnDomainSources main = config.getMain();
        CdnDomainSources back = config.getBack();
        Integer port = config.getPort();
        String originProtocol = config.getOriginProtocol();
        String defaultMaster = main.getIpOrDomain().replace(";", ",");
        if (ObjectUtil.equal(main.getOriginType(), OriginTypeEnum.DOMAIN.getParam())) {
            defaultMaster = main.getIpOrDomain();
            String[] split = defaultMaster.split(";");
            if (split.length > 1) {
                throw new BusinessException("主源站不支持多域名");
            }
        }
        BsBackSourceVo.BsOrigin bsOrigin = BsBackSourceVo.BsOrigin.builder()
                .default_master(defaultMaster)
                .build();
        if (Assert.notEmpty(back) && Assert.notEmpty(back.getIpOrDomain())) {
            if (ObjectUtil.equal(main.getOriginType(), OriginTypeEnum.DOMAIN.getParam())) {
                defaultMaster = main.getIpOrDomain();
                String[] split = defaultMaster.split(";");
                if (split.length > 1) {
                    throw new BusinessException("备源站不支持多域名");
                }
            }
            String defaultSlave = back.getIpOrDomain().replace(";", ",");
            bsOrigin.setDefault_slave(defaultSlave);
        }
        if (ObjectUtil.equal(originProtocol, "http")) {
            bsOrigin.setOrigin_mode("custom");
            bsOrigin.setOri_https("no");
            bsOrigin.setPort(port);
        } else if (ObjectUtil.equal(originProtocol, "https")) {
            bsOrigin.setOrigin_mode("custom");
            bsOrigin.setOri_https("yes");
            bsOrigin.setPort(port);
        } else if (ObjectUtil.equal(originProtocol, "follow")) {
            bsOrigin.setOrigin_mode("default");
        }
        BsBackSourceVo bsBackSourceVo = BsBackSourceVo.builder()
                .domains(cdnDomain.getDomainName())
                .config(BsBackSourceVo.BsConfig.builder()
                        .origin(bsOrigin)
                        .build())
                .build();
        try {
            BsDomainOperationApi.saveSourceInfo(bsBackSourceVo);
        } catch (Exception e) {
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }

    /**
     * 主备域名信息切换
     *
     * @param cdnDomain
     * @throws BusinessException
     */
    @Override
    public void change(CdnDomain cdnDomain) throws BusinessException {
        // TODO
    }


    /**
     * 修改回源类型
     *
     * @param cdnDomain
     * @param domainOriginSettingVo
     * @throws BusinessException
     */
    @Override
    public void saveOriginProtocol(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        // TODO 没有修改回源类型,放在了修改源站配置中
    }

    @Override
    public void saveOriginRequestUrlRewrite(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        // 没有
    }

    @Override
    public void saveAdvancedReturnSource(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        // 没有
    }

    /**
     * 修改Range回源
     *
     * @param cdnDomain
     * @param domainOriginSettingVo
     * @throws BusinessException
     */
    @Override
    public void saveRangeSwitch(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        String status = domainOriginSettingVo.getStatus();
        BsDomainOperationApi.rangeBackSource(cdnDomain.getDomainName(), status);
    }

    @Override
    public void saveRangeVerifyETag(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        // 没有
    }

    @Override
    public void saveOriginHost(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {

    }

    @Override
    public void saveRangeTimeOut(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        // 没有
    }

    @Override
    public void saveOriginRequestHeader(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        try {
            List<OriginRequestHeaderDTO> originRequestHeaders = domainOriginSettingVo.getOriginRequestHeader();
            List<BsRequestHeadVo.RequestHeadInfo> requestHeadInfos = new ArrayList<>();
            int i = 1;
            for (OriginRequestHeaderDTO originRequestHeader : originRequestHeaders) {
                BsRequestHeadVo.RequestHeadInfo requestHeadInfo = new BsRequestHeadVo.RequestHeadInfo();
                requestHeadInfo.setRegex(".*");
                requestHeadInfo.setHead_direction("SER_REQ");
                requestHeadInfo.setOrder(i++);
                String value = originRequestHeader.getValue();
                if (ObjectUtil.equal(originRequestHeader.getAction(), "set")) {
                    requestHeadInfo.setHead_op("ADD");

                } else if (ObjectUtil.equal(originRequestHeader.getAction(), "delete")) {
                    requestHeadInfo.setHead_op("DEL");
                    value = "";
                }
                requestHeadInfo.setHead(originRequestHeader.getName());
                requestHeadInfo.setValue(value);
                requestHeadInfos.add(requestHeadInfo);
            }
            DomainConfig domainConfig = getDomainConfig(cdnDomain.getDomainName());
            // 添加客户端响应头
            List<DomainAdvancedInfo.HttpResponseHeader> httpResponseHeader = domainConfig.getDomainAdvancedInfo().getHttp_response_header();
            for (DomainAdvancedInfo.HttpResponseHeader responseHeader : httpResponseHeader) {
                BsRequestHeadVo.RequestHeadInfo requestHeadInfo = new BsRequestHeadVo.RequestHeadInfo();
                requestHeadInfo.setRegex(".*");
                requestHeadInfo.setHead_direction("CLI_REP");
                requestHeadInfo.setOrder(i++);
                String value = responseHeader.getValue();
                if (ObjectUtil.equal(responseHeader.getAction(), "set")) {
                    requestHeadInfo.setHead_op("ADD");
                } else if (ObjectUtil.equal(responseHeader.getAction(), "delete")) {
                    requestHeadInfo.setHead_op("DEL");
                    value = "";
                }
                requestHeadInfo.setHead(responseHeader.getName());
                requestHeadInfo.setValue(value);
                requestHeadInfos.add(requestHeadInfo);
            }
            BsRequestHeadVo requestHeadVo = BsRequestHeadVo.builder()
                    .domains(cdnDomain.getDomainName())
                    .config(BsRequestHeadVo.BsRequestHeadConfig.builder()
                            .head_control(BsRequestHeadVo.HeadControl.builder()
                                    .list(requestHeadInfos)
                                    .build())
                            .build())
                    .build();
            BsDomainOperationApi.saveRequestHeadInfo(requestHeadVo);
        } catch (Exception e) {
            log.error("bs保存回源请求头信息失败，用户：{}，域名：{}", cdnDomain.getUserId(), cdnDomain.getDomainName());
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }

    @Override
    public void httpsConfiguration(CdnDomain cdnDomain, DomainHttpsSettingVo config) throws BusinessException {
        try {
            HttpPutBodyDTO https = config.getHttps();

            BsHttpsConfigVo.BsHttpsVo bsHttpsVo = BsHttpsConfigVo.BsHttpsVo.builder()
                    .http2("off")
                    .force_https("0")
                    .build();
            if (ObjectUtil.equal(https.getHttps_status(), "off")) {
                // 关闭HTTPS
                bsHttpsVo.setCert_id(0);
                BsHttpsConfigVo bsHttpsConfigVo = BsHttpsConfigVo.builder()
                        .domains(cdnDomain.getDomainName())
                        .config(BsHttpsConfigVo.BsHttpsConfigInner.builder()
                                .https(bsHttpsVo)
                                .build())
                        .build();
                DomainConfig domainConfig = getDomainConfig(cdnDomain.getDomainName());
                Integer certId = domainConfig.getDomainHttpsInfo().getHttps().getCertId();

                BsDomainOperationApi.httpsConfig(bsHttpsConfigVo);
                Thread.sleep(300);
                BsDomainOperationApi.delCertificate(certId);
            } else {
                // 开启HTTPS
                BsCertificateVo bsCertificateVo = BsCertificateVo.builder()
                        .name(https.getCertificate_name())
                        .certificate(https.getCertificate_value())
                        .key(https.getPrivate_key())
                        .build();
                int certificate = BsDomainOperationApi.certificate(bsCertificateVo);
                bsHttpsVo.setCert_id(certificate);
                BsHttpsConfigVo bsHttpsConfigVo = BsHttpsConfigVo.builder()
                        .domains(cdnDomain.getDomainName())
                        .config(BsHttpsConfigVo.BsHttpsConfigInner.builder()
                                .https(bsHttpsVo)
                                .build())
                        .build();
                BsDomainOperationApi.httpsConfig(bsHttpsConfigVo);
            }
        } catch (Exception e) {
            log.error("bs设置HTTPS信息失败->{}", e.getMessage());
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }

    /**
     * 修改HTTP2信息
     *
     * @param cdnDomain
     * @param config
     * @throws BusinessException
     */
    @Override
    public void httpsConfigurationOther(CdnDomain cdnDomain, DomainHttpsSettingVo config) throws BusinessException {
        BsDomainOperationApi.http2Info(cdnDomain.getDomainName(), config.getHttps().getHttp2_status());
    }

    /**
     * HTTPS强制跳转
     *
     * @param cdnDomain
     * @param config
     * @throws BusinessException
     */
    @Override
    public void forcedToJump(CdnDomain cdnDomain, DomainHttpsSettingVo config,String redirectCode) throws BusinessException {
        try {
            if (ObjectUtil.equal(config.getForceRedirect().getStatus(), "off")) {
                BsDomainOperationApi.forceRedirect(cdnDomain.getDomainName(), "0");
            } else {
                BsDomainOperationApi.forceRedirect(cdnDomain.getDomainName(), config.getForceRedirect().getRedirect_code().toString());
            }
        } catch (Exception e) {
            log.error("bs强制跳转失败->{}", e.getMessage());
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }

    @Override
    public void saveCacheRules(CdnDomain cdnDomain, SettingCacheVo config) throws BusinessException {
        try {
            List<CacheRuleDTO> cacheRules = config.getCacheRules();
            List<BsCacheVo.BsCacheRule> caches = new ArrayList<>();
            int i = 1;
            for (CacheRuleDTO cacheRule : cacheRules) {
                String object = cacheRule.getMatch_type();
                String value = cacheRule.getMatch_value();
                // 全路径
                if (ObjectUtil.equal(cacheRule.getMatch_type(), "full_path")) {
                    object = "route";
                    value = cacheRule.getMatch_value().replace(";", ",");
                } else if (ObjectUtil.equal(cacheRule.getMatch_type(), "catalog")) {
                    // 目录路径
                    object = "dir";
                    value = cacheRule.getMatch_value().replace(";", ",");
                } else if (ObjectUtil.equal(cacheRule.getMatch_type(), "file_extension")) {
                    object = "ext";
                    String tempValue = cacheRule.getMatch_value().replace(".", "");
                    value = tempValue.replace(";", ",");
                } else if (ObjectUtil.equal(cacheRule.getMatch_type(), "all")) {
                    object = "all";
                    value = ".*";
                }
                String ttlUnit = "";
                if (ObjectUtil.equal(cacheRule.getTtl_unit(), "d")) {
                    ttlUnit = "D";
                } else if (ObjectUtil.equal(cacheRule.getTtl_unit(), "h")) {
                    ttlUnit = "h";
                } else if (ObjectUtil.equal(cacheRule.getTtl_unit(), "m")) {
                    ttlUnit = "i";
                } else if (ObjectUtil.equal(cacheRule.getTtl_unit(), "s")) {
                    ttlUnit = "s";
                }
                BsCacheVo.BsCacheRule cacheControlRule = BsCacheVo.BsCacheRule.builder()
                        .match_method(object)
                        .pattern(value)
                        .expire(cacheRule.getTtl())
                        .expire_unit(ttlUnit)
                        .priority(i++)
                        .case_ignore("no")
                        .build();
                caches.add(cacheControlRule);
            }
            BsCacheVo bsCacheVo = BsCacheVo.builder()
                    .domains(cdnDomain.getDomainName())
                    .config(BsCacheVo.BsCacheConfig.builder()
                            .cache_rule_list(caches)
                            .build())
                    .build();
            BsDomainOperationApi.saveCacheInfo(bsCacheVo);
        } catch (Exception e) {
            log.error("bs设置域名缓存信息失败->{}", e.getMessage());
            throw new BusinessException(e.getMessage()).setCause(e);
        }

    }

    @Override
    public void saveCacheFollowOriginStatusSwitch(CdnDomain cdnDomain, SettingCacheVo config) throws BusinessException {
        //
    }

    @Override
    public void saveErrorCodeCache(CdnDomain cdnDomain, SettingCacheVo config) throws BusinessException {
        // 状态码缓存时间 白山未使用
    }

    @Override
    public void saveHotlinkPrevention(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        RefererDTO referer = config.getReferer();
        try {
            Integer refererType = referer.getReferer_type();
            if (ObjectUtil.equal(refererType, 0)) {
                BsDomainOperationApi.deleteConfig(cdnDomain.getDomainName(), Arrays.asList("referer"));
            } else {
                BsRefererVo refererVo = BsRefererVo.builder()
                        .domains(cdnDomain.getDomainName())
                        .config(BsRefererVo.BsRefererConfigInner.builder()
                                .referer(BsRefererVo.BsReferer.builder()
                                        .type(refererType)
                                        .list(referer.getReferers())
                                        .allow_empty(referer.getInclude_empty())
                                        .build())
                                .build())
                        .build();
                BsDomainOperationApi.saveRefererInfo(refererVo);
            }
        } catch (Exception e) {
            log.error("bs防盗链配置失败->{}", e.getMessage());
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }

    @Override
    public void saveIpBlackWhiteList(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        try {
            Integer ipType = config.getType();
            if (ObjectUtil.equal(ipType, 0)) {
                BsDomainOperationApi.deleteConfig(cdnDomain.getDomainName(), Arrays.asList("ip_black_list", "ip_white_list"));
            } else if (ObjectUtil.equal(ipType, 1)) {
                BsIpList bsIpList = BsIpList.builder()
                        .domains(cdnDomain.getDomainName())
                        .config(BsIpList.BsIpListInner.builder()
                                .ip_black_list(BsIpList.BsIpListConfig.builder()
                                        .list(config.getIps())
                                        .mode("cover")
                                        .build())
                                .build())
                        .build();
                BsDomainOperationApi.saveIpList(bsIpList);
            } else if (ObjectUtil.equal(ipType, 2)) {
                BsIpList bsIpList = BsIpList.builder()
                        .domains(cdnDomain.getDomainName())
                        .config(BsIpList.BsIpListInner.builder()
                                .ip_white_list(BsIpList.BsIpListConfig.builder()
                                        .list(config.getIps())
                                        .mode("cover")
                                        .build())
                                .build())
                        .build();
                BsDomainOperationApi.saveIpList(bsIpList);
            }
        } catch (Exception e) {
            log.error("易凡IP黑白名单配置失败->{}", e.getMessage());
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }

    @Override
    public void saveUserAgentFilter(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        // TODO 没有这个功能
    }

    @Override
    public void saveUrlAuth(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {

    }

    /**
     * 配置HTTP响应头信息
     *
     * @param cdnDomain
     * @param config
     * @throws BusinessException
     */
    @Override
    public void saveHttpHeader(CdnDomain cdnDomain, SettingHigherVo config) throws BusinessException {
        List<BsRequestHeadVo.RequestHeadInfo> requestHeadInfos = new ArrayList<>();
        int i = 1;
        DomainConfig domainConfig = getDomainConfig(cdnDomain.getDomainName());
        List<DomainBackSourceInfo.BackSourceRequestInfo> originRequestHeaders = domainConfig.getDomainBackSourceInfo().getOrigin_request_header();
        // 回源请求头
        for (DomainBackSourceInfo.BackSourceRequestInfo originRequestHeader : originRequestHeaders) {
            BsRequestHeadVo.RequestHeadInfo requestHeadInfo = new BsRequestHeadVo.RequestHeadInfo();
            requestHeadInfo.setRegex(".*");
            requestHeadInfo.setHead_direction("SER_REQ");
            requestHeadInfo.setOrder(i++);
            String value = originRequestHeader.getValue();
            if (ObjectUtil.equal(originRequestHeader.getAction(), "set")) {
                requestHeadInfo.setHead_op("ADD");

            } else if (ObjectUtil.equal(originRequestHeader.getAction(), "delete")) {
                requestHeadInfo.setHead_op("DEL");
                value = "";
            }
            requestHeadInfo.setHead(originRequestHeader.getName());
            requestHeadInfo.setValue(value);
            requestHeadInfos.add(requestHeadInfo);
        }
        // 添加客户端响应头
        List<ResponseHeaderDTO> httpResponseHeaders = Convert.toList(ResponseHeaderDTO.class, config.getHttpResponseHeaders());
        for (ResponseHeaderDTO responseHeader : httpResponseHeaders) {
            BsRequestHeadVo.RequestHeadInfo requestHeadInfo = new BsRequestHeadVo.RequestHeadInfo();
            requestHeadInfo.setRegex(".*");
            requestHeadInfo.setHead_direction("CLI_REP");
            requestHeadInfo.setOrder(i++);
            String value = responseHeader.getValue();
            if (ObjectUtil.equal(responseHeader.getAction(), "set")) {
                requestHeadInfo.setHead_op("ADD");

            } else if (ObjectUtil.equal(responseHeader.getAction(), "delete")) {
                requestHeadInfo.setHead_op("DEL");
                value = "";
            }
            requestHeadInfo.setHead(responseHeader.getName());
            requestHeadInfo.setValue(value);
            requestHeadInfos.add(requestHeadInfo);
        }
        BsRequestHeadVo requestHeadVo = BsRequestHeadVo.builder()
                .domains(cdnDomain.getDomainName())
                .config(BsRequestHeadVo.BsRequestHeadConfig.builder()
                        .head_control(BsRequestHeadVo.HeadControl.builder()
                                .list(requestHeadInfos)
                                .build())
                        .build())
                .build();
        BsDomainOperationApi.saveRequestHeadInfo(requestHeadVo);
    }

    @Override
    public void saveCustomErrorPageConfiguration(CdnDomain cdnDomain, SettingHigherVo config) throws BusinessException {

    }

    @Override
    public void saveCompress(CdnDomain cdnDomain, SettingHigherVo config) throws BusinessException {
        if (ObjectUtil.equal(config.getCompress().getStatus(), "off")) {
            BsDomainOperationApi.deleteConfig(cdnDomain.getDomainName(), Arrays.asList("compress_response"));
        } else {
            BsDomainOperationApi.compressResponse(cdnDomain.getDomainName());
        }
    }

    @Override
    public DomainConfig getDomainConfig(String domainName) throws BusinessException {
        // 1.通过domainName查询domainId
        List<CdnDomain> cdnDomainList = domainService.queryByDomainNames(Arrays.asList(domainName));
        if (Assert.isEmpty(cdnDomainList)) {
            throw new BusinessException("当前系统域名不存在");
        }
        CdnDomain cdnDomain = cdnDomainList.get(0);
        JSONObject domainInfo = BsDomainOperationApi.query(domainName);
        JSONObject config = domainInfo.getJSONObject("config");
        // 源站配置信息
        JSONObject origin = config.getJSONObject("origin");
        String defaultMaster = origin.getString("default_master");
        String defaultSlave = origin.getString("default_slave");
        String replaceMaster = defaultMaster.replace(",", ";");
        String[] splitMaster = defaultMaster.split(",");
        String masterType = null;
        for (int i = 0; i < splitMaster.length; i++) {
            if (ValidatorUtils.isIPAddress(splitMaster[i])) {
                masterType = OriginTypeDtoEnum.IPADDR.getParam();
            } else {
                masterType = OriginTypeDtoEnum.DOMAIN.getParam();
            }
        }
        DomainBasicInfo.SourceStationStandbyInfo sourceStationStandbyInfo = DomainBasicInfo.SourceStationStandbyInfo.builder().build();
        if (Assert.notEmpty(defaultSlave)) {
            String replaceSlave = defaultSlave.replace(",", ";");
            String[] splitSlave = defaultSlave.split(",");
            String slaveType = null;
            for (int i = 0; i < splitSlave.length; i++) {
                if (ValidatorUtils.isIPAddress(splitSlave[i])) {
                    slaveType = OriginTypeDtoEnum.IPADDR.getParam();
                } else {
                    slaveType = OriginTypeDtoEnum.DOMAIN.getParam();
                }
            }
            sourceStationStandbyInfo = DomainBasicInfo.SourceStationStandbyInfo.builder()
                    .ipOrDomain(replaceSlave)
                    .sourceStationType(slaveType)
                    .build();
        } else {
            sourceStationStandbyInfo.setIpOrDomain("");
        }

        DomainBasicInfo domainBasicInfo = DomainBasicInfo.builder()
                .domainName(cdnDomain.getDomainName())
                .businessType(cdnDomain.getBusinessType())
                .serviceArea(cdnDomain.getServiceArea())
                .cname(cdnDomain.getCnameYifan())
                .domainStatus(DomainStatusDtoEnum.getSelfParam(domainInfo.getString("status"), CdnOperationRoute.BAISHAN))
                .httpsStatus(HttpsStatusDtoEnum.getSelfParam(domainInfo.getString("https"), CdnOperationRoute.BAISHAN))
                .sourceStationPrimaryInfo(DomainBasicInfo.SourceStationPrimaryInfo.builder()
                        .sourceStationType(masterType)
                        .ipOrDomain(replaceMaster)
                        .build())
                .sourceStationStandbyInfo(sourceStationStandbyInfo)
                .createTime(cdnDomain.getCreateTime())
                .updateTime(cdnDomain.getUpdateTime())
                .build();
        String originMode = origin.getString("origin_mode");
        String oriHttps = origin.getString("ori_https");
        Integer port = origin.getInteger("port");
        String originProtocol = "";
        if (ObjectUtil.equal(originMode, "default")) {
            originProtocol = "follow";
        } else if (ObjectUtil.equal(originMode, "http")) {
            originProtocol = "http";
        } else if (ObjectUtil.equal(originMode, "https")) {
            originProtocol = "https";
        } else if (ObjectUtil.equal(originMode, "custom")) {
            if (ObjectUtil.equal(oriHttps, "yes")) {
                originProtocol = "https";
            } else {
                originProtocol = "http";
            }
        }
        JSONObject headControlJsonObj = config.getJSONObject("head_control");
        List<DomainBackSourceInfo.BackSourceRequestInfo> backSourceRequestInfos = new ArrayList<>();
        List<DomainAdvancedInfo.HttpResponseHeader> httpResponseHeaders = new ArrayList<>();
        if (Assert.notEmpty(headControlJsonObj)) {
            JSONArray requestHeadsJsonArr = headControlJsonObj.getJSONArray("list");
            for (Object o : requestHeadsJsonArr) {
                JSONObject requestHead = (JSONObject) o;
                String key = requestHead.getString("key");
                String value = requestHead.getString("value");
                String headOp = requestHead.getString("head_op");
                String action = "";
                if (ObjectUtil.equal(headOp, "ADD")) {
                    action = "set";
                } else if (ObjectUtil.equal(headOp, "DEL")) {
                    action = "delete";
                }
                if (ObjectUtil.equal(requestHead.getString("head_direction"), "SER_REQ")) {
                    DomainBackSourceInfo.BackSourceRequestInfo backSourceRequestInfo = DomainBackSourceInfo.BackSourceRequestInfo.builder()
                            .action(action)
                            .name(key)
                            .value(value)
                            .build();
                    backSourceRequestInfos.add(backSourceRequestInfo);
                } else if (ObjectUtil.equal(requestHead.getString("head_direction"), "CLI_REP")) {
                    DomainAdvancedInfo.HttpResponseHeader httpResponseHeader = DomainAdvancedInfo.HttpResponseHeader.builder()
                            .action(action)
                            .name(key)
                            .value(value)
                            .build();
                    httpResponseHeaders.add(httpResponseHeader);
                }
            }
        }
        // range
        String rangeStatus = "on";
        if (config.containsKey("range_back_source")) {
            rangeStatus = "off";
        }
        DomainBackSourceInfo domainBackSourceInfo = DomainBackSourceInfo.builder()
                .origin_protocol(originProtocol)
                .port(port)
                .origin_request_header(backSourceRequestInfos)
                .origin_range_status(rangeStatus)
                .build();
        DomainHttpsInfo.HttpGetBody httpGetBody = DomainHttpsInfo.HttpGetBody.builder().http2_status("off").build();
        DomainHttpsInfo.ForceRedirect forceRedirect = DomainHttpsInfo.ForceRedirect.builder().status("off").build();
        if (config.containsKey("https")) {
            JSONObject https = config.getJSONObject("https");
            Integer certId = https.getInteger("cert_id");
            JSONObject jsonObject = BsDomainOperationApi.queryCertInfo(certId);
            httpGetBody.setHttps_status("on");
            httpGetBody.setCertificate_name(jsonObject.getString("name"));
            httpGetBody.setCertId(certId);
            if (https.containsKey("http2") && ObjectUtil.equal(https.getString("http2"), "on")) {
                httpGetBody.setHttp2_status("on");
            } else {
                httpGetBody.setHttp2_status("off");
            }
            if (https.containsKey("force_https")) {
                String forceHttps = https.getString("force_https");
                if (ObjectUtil.equal(forceHttps, "301")) {
                    forceRedirect.setStatus("on");
                    forceRedirect.setType("https");
                    forceRedirect.setRedirect_code("301");
                } else if (ObjectUtil.equal(forceHttps, "302")) {
                    forceRedirect.setStatus("on");
                    forceRedirect.setType("https");
                    forceRedirect.setRedirect_code("302");
                }
            }
        } else {
            httpGetBody.setHttps_status("off");
        }
        DomainHttpsInfo domainHttpsInfo = DomainHttpsInfo.builder()
                .https(httpGetBody)
                .force_redirect(forceRedirect)
                .build();
        List<DomainCacheInfo.CacheRule> cacheRules = new ArrayList<>();
        if (config.containsKey("cache_rule_list")) {
            for (Object cacheRule : config.getJSONArray("cache_rule_list")) {
                JSONObject cacheRuleJsonObj = (JSONObject) cacheRule;
                String matchMethod = cacheRuleJsonObj.getString("match_method");
                Integer expire = cacheRuleJsonObj.getInteger("expire");
                String expireUnit = cacheRuleJsonObj.getString("expire_unit").toLowerCase();
                String pattern = cacheRuleJsonObj.getString("pattern");
                if (ObjectUtil.equal(matchMethod, "all")) {
                    matchMethod = "all";
                } else if (ObjectUtil.equal(matchMethod, "ext")) {
                    matchMethod = "file_extension";
                    List<String> collect = Arrays.stream(pattern.split(",")).collect(Collectors.toList());
                    pattern = "." + String.join(";.", collect);
                } else if (ObjectUtil.equal(matchMethod, "dir")) {
                    matchMethod = "catalog";
                } else if (ObjectUtil.equal(matchMethod, "route")) {
                    matchMethod = "full_path";
                }
                if (ObjectUtil.equal(expireUnit, "Y")) {
                    expire *= 365;
                    expireUnit = "d";
                } else if (ObjectUtil.equal(expireUnit, "M")) {
                    expire *= 30;
                    expireUnit = "d";
                } else if (ObjectUtil.equal(expireUnit, "D")) {
                    expireUnit = "d";
                } else if (ObjectUtil.equal(expireUnit, "h")) {
                    expireUnit = "h";
                } else if (ObjectUtil.equal(expireUnit, "i")) {
                    expireUnit = "m";
                } else if (ObjectUtil.equal(expireUnit, "s")) {
                    expireUnit = "s";
                }
                DomainCacheInfo.CacheRule cacheRuleInfo = DomainCacheInfo.CacheRule.builder()
                        .match_type(matchMethod)
                        .ttl(expire)
                        .ttl_unit(expireUnit)
                        .build();
                if (Assert.notEmpty(pattern)) {
                    String replace = pattern.replace(",", ";");
                    cacheRuleInfo.setMatch_value(replace);
                }
                cacheRules.add(cacheRuleInfo);
            }
        }
        DomainCacheInfo domainCacheInfo = DomainCacheInfo.builder()
                .cache_rules(cacheRules)
                .build();
        // referer黑白名单
        DomainVisitInfo.Referer referer = DomainVisitInfo.Referer.builder().build();
        if (config.containsKey("referer")) {
            JSONObject refererJsonObj = config.getJSONObject("referer");
            String type = refererJsonObj.getString("type");
            JSONArray refererList = refererJsonObj.getJSONArray("list");
            referer.setReferer_type(Integer.valueOf(type));
            String refererValue = refererList.stream().map(Object::toString).collect(Collectors.joining(System.lineSeparator()));
            referer.setValue(refererValue);
            referer.setInclude_empty(refererJsonObj.getBoolean("allow_empty"));
        } else {
            referer.setReferer_type(0);
            referer.setInclude_empty(false);
        }
        // ip黑白名单
        DomainVisitInfo.IpFilter ipFilter = DomainVisitInfo.IpFilter.builder().build();
        if (config.containsKey("ip_black_list")) {
            JSONObject ipBlackList = config.getJSONObject("ip_black_list");
            JSONArray list = ipBlackList.getJSONArray("list");
            String ipValue = list.stream().map(Object::toString).collect(Collectors.joining(System.lineSeparator()));
            ipFilter.setType("black");
            ipFilter.setValue(ipValue);
        } else if (config.containsKey("ip_white_list")) {
            JSONObject ipBlackList = config.getJSONObject("ip_white_list");
            JSONArray list = ipBlackList.getJSONArray("list");
            String ipValue = list.stream().map(Object::toString).collect(Collectors.joining(System.lineSeparator()));
            ipFilter.setType("white");
            ipFilter.setValue(ipValue);
        } else {
            ipFilter.setType("off");
        }
        DomainVisitInfo domainVisitInfo = DomainVisitInfo.builder()
                .referer(referer)
                .ip_filter(ipFilter)
                .build();
        DomainAdvancedInfo.Compress compress = DomainAdvancedInfo.Compress.builder().build();
        if (config.containsKey("compress_response")) {
            compress.setStatus("on");
            compress.setType("gzip");
        }
        DomainAdvancedInfo domainAdvancedInfo = DomainAdvancedInfo.builder()
                .http_response_header(httpResponseHeaders)
                .compress(compress)
                .build();
        DomainConfig domainConfig = DomainConfig.builder()
                .domainBasicInfo(domainBasicInfo)
                .domainBackSourceInfo(domainBackSourceInfo)
                .domainHttpsInfo(domainHttpsInfo)
                .domainCacheInfo(domainCacheInfo)
                .domainVisitInfo(domainVisitInfo)
                .domainAdvancedInfo(domainAdvancedInfo)
                .build();
        return domainConfig;
    }
}
