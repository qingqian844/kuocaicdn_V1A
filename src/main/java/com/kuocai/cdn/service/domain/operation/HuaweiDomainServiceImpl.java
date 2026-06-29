package com.kuocai.cdn.service.domain.operation;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.kuocai.cdn.api.*;
import com.kuocai.cdn.api.huawei.cdn.DomainConfigureApi;
import com.kuocai.cdn.api.huawei.cdn.DomainOperationApi;
import com.kuocai.cdn.api.huawei.cdn.constant.ServiceArea;
import com.kuocai.cdn.api.huawei.cdn.constant.SourcePriority;
import com.kuocai.cdn.api.huawei.cdn.dto.*;
import com.kuocai.cdn.api.tencent.dns.CreateRecordResponse;
import com.kuocai.cdn.api.tencent.dns.TencentApi;
import com.kuocai.cdn.api.tencent.dns.dto.CreateRecordDTO;
import com.kuocai.cdn.api.tencent.dns.properties.TencentDns;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.entity.CdnDomainSources;
import com.kuocai.cdn.enumeration.domainmerage.CdnRoute;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.exception.CdnHuaweiException;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.vo.*;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 华为加速域名(CdnDomain)服务实现
 */
@Slf4j
@Service
public class HuaweiDomainServiceImpl extends BaseService<CdnDomain> implements ICdnPlatformService {


    /**
     * 创建加速域名
     *
     * @param domainName   加速域名
     * @param businessType 业务类型
     * @param serviceArea  服务范围
     * @param originType   源站类别
     * @param ipOrDomain   源站
     * @return 域名信息
     */
    @Transactional(rollbackFor = {Exception.class})
    @Override
    public CdnDomain create(Long userId, String domainName, String businessType, String serviceArea, String originType, String ipOrDomain) throws BusinessException {
        // 封装源站信息
        DomainSourceDTO sourceDTO = DomainSourceDTO.builder()
                .ip_or_domain(ipOrDomain)
                .origin_type(originType)
                .active_standby(1)
                .build();
        List<DomainSourceDTO> sourceDTOS = new ArrayList<>();
        sourceDTOS.add(sourceDTO);
        // 封装域名信息
        DomainBodyDTO bodyDTO = DomainBodyDTO.builder()
                .domain_name(domainName)
                .business_type(businessType)
                .service_area(serviceArea)
                .sources(sourceDTOS)
                .build();
        JSONObject jsonObject = null;
        CdnDomain cdnDomain = null;
        try {
            jsonObject = DomainOperationApi.createDomain(bodyDTO);
            JSONObject domain = jsonObject.getJSONObject("domain");
            //保存域名信息
            cdnDomain = convertDomain(domain);
            // 创建的时候将华为的缓存规则重制
            SettingCacheVo cacheVo = new SettingCacheVo();
            cacheVo.setCacheRules(Arrays.asList(new CacheRuleDTO().setMatch_type("all").setPriority(1).setTtl(30).setTtl_unit("d")));
            Thread.sleep(300);
            saveCacheRules(cdnDomain, cacheVo);
        } catch (CdnHuaweiException | InterruptedException e) {
            log.error("创建加速域名失败，用户：{}，域名：{}，业务类型：{}，服务范围：{}，源站类别：{}，源站：{}",
                    userId, domainName, businessType, serviceArea, originType, ipOrDomain);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
        cdnDomain.setUserId(userId);
        cdnDomain.setRoute(CdnRoute.HUAWEI.getCode());
        return save(cdnDomain);
    }

    @Override
    public CdnDomain configDNS(CdnDomain cdnDomain) throws TencentCloudSDKException, BusinessException {
        String domainName = cdnDomain.getDomainName();
        String cnameHuawei = cdnDomain.getCnameHuawei();
        CreateRecordDTO createRecordRequest = new CreateRecordDTO();
        createRecordRequest.setDomain(TencentDns.LOCAL_DOMAIN_NAME).setSubDomain(domainName + "." + RandomUtil.randomString(8)).setValue(cnameHuawei);
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
        if (Assert.isEmpty(recordResponse.getRecordId())) {
            log.error("dns解析失败，域名：{}", cdnDomain.getDomainName());
            throw new BusinessException("dns解析失败");
        }
        return null;
    }

    /**
     * 域名结果转换
     *
     * @param domain 响应的域名信息
     * @return 域名实体
     */
    public CdnDomain convertDomain(JSONObject domain) {
        return CdnDomain.builder()
                .domainId(domain.getString("id"))
                .domainName(domain.getString("domain_name"))
                .businessType(domain.getString("business_type"))
                .serviceArea(domain.getString("service_area"))
                .domainStatus(domain.getString("domain_status"))
                .cnameHuawei(domain.getString("cname"))
                .build();
    }

    public void save(CdnDomain cdnDomain, String businessType, String serviceArea) throws CdnHuaweiException {
        // 切换的类型
        DomainConfigsDTO param = new DomainConfigsDTO();
        param.setBusiness_type(businessType);
        String currenServiceArea = cdnDomain.getServiceArea();
        //  当前为国内加速
        if (ObjectUtil.equal(currenServiceArea, ServiceArea.MAINLAND_CHINA)) {
            if (ObjectUtil.equal(serviceArea, ServiceArea.MAINLAND_CHINA) || ObjectUtil.equal(serviceArea, ServiceArea.GLOBAL)) {
                param.setService_area(serviceArea);
            } else {
                throw new CdnHuaweiException("国内不能直接切换国外");
            }
        }
        //  当前为国外加速
        if (ObjectUtil.equal(currenServiceArea, ServiceArea.OUTSIDE_MAINLAND_CHINA)) {
            if (ObjectUtil.equal(serviceArea, ServiceArea.OUTSIDE_MAINLAND_CHINA) || ObjectUtil.equal(serviceArea, ServiceArea.GLOBAL)) {
                param.setService_area(serviceArea);
            } else {
                throw new CdnHuaweiException("国外不能直接切换国内");
            }
        }
        if (ObjectUtil.equal(currenServiceArea, ServiceArea.GLOBAL)) {
            param.setService_area(serviceArea);
        }
        try {
            DomainConfigureApi.updateDomainConfigs(cdnDomain.getDomainName(), param);
        } catch (Exception e) {
            String error = "修改域名全量配置接口失败！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }

    /**
     * 停用加速域名
     *
     * @param cdnDomain 加速域名
     * @return 域名信息
     */
    @Transactional(rollbackFor = {Exception.class})
    public void disable(CdnDomain cdnDomain) throws BusinessException {
        JSONObject jsonObject = null;
        try {
            jsonObject = DomainOperationApi.disableDomain(cdnDomain.getDomainId());
        } catch (CdnHuaweiException e) {
            log.error("停用加速域名失败，域名信息：{}", cdnDomain);
            throw new CdnHuaweiException(e.getMessage()).setCause(e).log();
        }
    }

    /**
     * 启用加速域名
     *
     * @param cdnDomain 加速域名
     * @return 域名信息
     */
    @Transactional(rollbackFor = {Exception.class})
    public void enable(CdnDomain cdnDomain) throws BusinessException {
        JSONObject jsonObject = null;
        try {
            jsonObject = DomainOperationApi.enableDomain(cdnDomain.getDomainId());
        } catch (CdnHuaweiException e) {
            log.error("启用加速域名失败，域名信息：{}", cdnDomain);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }


    /**
     * 删除加速域名
     *
     * @param cdnDomain 加速域名
     * @return 域名信息
     */
    @Transactional(rollbackFor = {Exception.class})
    public void delete(CdnDomain cdnDomain) throws BusinessException {
        try {
            DomainOperationApi.deleteDomain(cdnDomain.getDomainId());
        } catch (CdnHuaweiException e) {
            log.error("删除加速域名失败，域名信息：{}", cdnDomain);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }

    }

    @Override
    public void ipv6(CdnDomain cdnDomain, Integer status) throws BusinessException {
        String domainName = cdnDomain.getDomainName();
        DomainConfigsDTO param = new DomainConfigsDTO();
        param.setIpv6_accelerate(status);
        // 修改华为云IPV6
        DomainConfigureApi.updateDomainConfigs(domainName, param);
    }

    /**
     * 修改源站配置
     *
     * @param cdnDomain
     * @param config
     * @throws BusinessException
     */
    @Override
    public void saveSourceStationConfig(CdnDomain cdnDomain, CdnDomainSourcesVo config) throws BusinessException {
        CdnDomainSources voMain = config.getMain();
        CdnDomainSources voBack = config.getBack();
        DomainConfigsDTO configs = new DomainConfigsDTO();
        List<SourcesConfigDTO> params = covert2SourcesConfigDTO(new ArrayList<>(), voMain);
        if (Assert.notEmpty(voBack)) {
            params = covert2SourcesConfigDTO(params, voBack);
        }
        configs.setSources(params);
        // 修改域名的源站信息
        DomainConfigureApi.updateDomainConfigs(cdnDomain.getDomainName(), configs);
    }

    public List<SourcesConfigDTO> covert2SourcesConfigDTO(List<SourcesConfigDTO> params, CdnDomainSources source) {
        List<String> mainIpOrDmains = Arrays.asList(source.getIpOrDomain().split(";"));
        for (String mainIpOrDomain : mainIpOrDmains) {
            SourcesConfigDTO param = SourcesConfigDTO.builder()
                    .origin_addr(mainIpOrDomain)
                    .origin_type(source.getOriginType()).http_port(source.getHttpPort()).https_port(source.getHttpsPort()).host_name(source.getHostName()).build();
//            // 如果开启类型为 OBS Bucket源站
//            if (ObjectUtil.equal(sourcesVo.getOriginType(), OriginType.OBS_BUCKET)) {
//                param.setObs_web_hosting_status("on");
//            } else {
//                param.setObs_web_hosting_status("off");
//            }
            if (ObjectUtil.equal(source.getActiveStandby(), 1)) {
                param.setPriority(SourcePriority.MAIN);
            } else {
                param.setPriority(SourcePriority.BACK);
            }
            params.add(param);
        }
        return params;
    }

    @Override
    public void change(CdnDomain cdnDomain) throws BusinessException {
        // 获取加速域名的主源站备源站信息
        DomainConfig domainConfig = getDomainConfig(cdnDomain.getDomainName());
        DomainBasicInfo.SourceStationPrimaryInfo sourceStationPrimaryInfo = domainConfig.getDomainBasicInfo().getSourceStationPrimaryInfo();
        DomainBasicInfo.SourceStationStandbyInfo sourceStationStandbyInfo = domainConfig.getDomainBasicInfo().getSourceStationStandbyInfo();

        if (Assert.isEmpty(sourceStationStandbyInfo.getIpOrDomain())) {
            throw new BusinessException("加速域名未配置备源站");
        }
        // 转换参数
        List<String> mainIps = Arrays.asList(sourceStationPrimaryInfo.getIpOrDomain().split(";"));
        List<String> standbyIps = Arrays.asList(sourceStationStandbyInfo.getIpOrDomain().split(";"));
        List<SourceWithPortDTO> params = new ArrayList<>();
        for (String mainIp : mainIps) {
            // active_standby 0备战 1主站
            SourceWithPortDTO mainSource = SourceWithPortDTO.builder().ip_or_domain(mainIp)
                    .origin_type(sourceStationPrimaryInfo.getSourceStationType()).active_standby(0).build();
            params.add(mainSource);
        }
        for (String standbyIp : standbyIps) {
            SourceWithPortDTO backSource = SourceWithPortDTO.builder().ip_or_domain(standbyIp)
                    .origin_type(sourceStationStandbyInfo.getSourceStationType()).active_standby(1).build();
            params.add(backSource);
        }
        JSONObject jsonObject = DomainConfigureApi.updateOrigin(cdnDomain.getDomainId(), params);
        log.info(jsonObject.toJSONString());
    }

    @Override
    public void saveOriginProtocol(CdnDomain cdnDomain, DomainOriginSettingVo config) throws BusinessException {
        DomainConfigsDTO param = DomainConfigsDTO.builder().origin_protocol(config.getOriginProtocol()).build();
        DomainConfigureApi.updateDomainConfigs(cdnDomain.getDomainName(), param);
    }

    @Override
    public void saveOriginRequestUrlRewrite(CdnDomain cdnDomain, DomainOriginSettingVo config) throws BusinessException {
        DomainConfigsDTO param = DomainConfigsDTO.builder().origin_request_url_rewrite(config.getOriginRequestUrlRewriteDTOS()).build();
        DomainConfigureApi.updateDomainConfigs(cdnDomain.getDomainName(), param);
    }

    @Override
    public void saveAdvancedReturnSource(CdnDomain cdnDomain, DomainOriginSettingVo config) throws BusinessException {
        DomainConfigsDTO param = DomainConfigsDTO.builder().flexible_origin(config.getFlexibleOrigins()).build();
        DomainConfigureApi.updateDomainConfigs(cdnDomain.getDomainName(), param);
    }

    @Override
    public void saveRangeSwitch(CdnDomain cdnDomain, DomainOriginSettingVo config) throws BusinessException {
        DomainConfigureApi.updateRangeSwitch(cdnDomain.getDomainId(), config.getStatus());
    }

    @Override
    public void saveRangeVerifyETag(CdnDomain cdnDomain, DomainOriginSettingVo config) throws BusinessException {
        DomainConfigsDTO param = DomainConfigsDTO.builder().slice_etag_status(config.getStatus()).build();
        DomainConfigureApi.updateDomainConfigs(cdnDomain.getDomainName(), param);
    }

    @Override
    public void saveOriginHost(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {

    }

    @Override
    public void saveRangeTimeOut(CdnDomain cdnDomain, DomainOriginSettingVo config) throws BusinessException {
        DomainConfigsDTO param = DomainConfigsDTO.builder().origin_receive_timeout(config.getOriginReceiveTimeOut()).build();
        DomainConfigureApi.updateDomainConfigs(cdnDomain.getDomainName(), param);
    }

    @Override
    public void saveOriginRequestHeader(CdnDomain cdnDomain, DomainOriginSettingVo config) throws BusinessException {
        DomainConfigsDTO param = DomainConfigsDTO.builder().origin_request_header(config.getOriginRequestHeader()).build();
        DomainConfigureApi.updateDomainConfigs(cdnDomain.getDomainName(), param);
    }


    @Override
    public void httpsConfiguration(CdnDomain cdnDomain, DomainHttpsSettingVo config) throws BusinessException {
        DomainConfigsDTO param = DomainConfigsDTO.builder().https(config.getHttps()).build();
        DomainConfigureApi.updateDomainConfigs(cdnDomain.getDomainName(), param);
    }

    @Override
    public void httpsConfigurationOther(CdnDomain cdnDomain, DomainHttpsSettingVo config) throws BusinessException {
        DomainConfigsDTO param = DomainConfigsDTO.builder().https(config.getHttps()).build();
        DomainConfigureApi.updateDomainConfigs(cdnDomain.getDomainName(), param);
    }

    @Override
    public void forcedToJump(CdnDomain cdnDomain, DomainHttpsSettingVo config,String redirectCode) throws BusinessException {
        DomainConfigsDTO param = DomainConfigsDTO.builder().force_redirect(config.getForceRedirect()).build();
        DomainConfigureApi.updateDomainConfigs(cdnDomain.getDomainName(), param);
    }

    @Override
    public void saveCacheRules(CdnDomain cdnDomain, SettingCacheVo config) throws BusinessException {
        List<CacheRuleDTO> cacheRules = config.getCacheRules();
        for (int i = 1; i <= cacheRules.size(); i++) {
            CacheRuleDTO cacheRuleDTO = cacheRules.get(i - 1);
            cacheRuleDTO.setPriority(cacheRules.size() - i + 1);
            cacheRules.set(i-1, cacheRuleDTO);
        }
        DomainConfigsDTO param = DomainConfigsDTO.builder().cache_rules(config.getCacheRules()).build();
        DomainConfigureApi.updateDomainConfigs(cdnDomain.getDomainName(), param);
    }

    @Override
    public void saveCacheFollowOriginStatusSwitch(CdnDomain cdnDomain, SettingCacheVo config) throws BusinessException {
        DomainConfigsDTO param = DomainConfigsDTO.builder().cache_follow_origin_status(config.getCacheFollowOriginStatus()).build();
        DomainConfigureApi.updateDomainConfigs(cdnDomain.getDomainName(), param);
    }

    @Override
    public void saveErrorCodeCache(CdnDomain cdnDomain, SettingCacheVo config) throws BusinessException {
        DomainConfigsDTO param = DomainConfigsDTO.builder().error_code_cache(config.getErrorCodeCache()).build();
        DomainConfigureApi.updateDomainConfigs(cdnDomain.getDomainName(), param);
    }

    @Override
    public void saveHotlinkPrevention(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        RefererDTO referer = config.getReferer();
        String refererList = String.join(";", referer.getReferers());
        referer.setReferer_list(refererList);
        DomainConfigureApi.updateReferer(cdnDomain.getDomainId(), config.getReferer());
    }

    @Override
    public void saveIpBlackWhiteList(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        DomainConfigureApi.updateIpAcl(cdnDomain.getDomainId(), config.getType(), config.getIps());
    }

    @Override
    public void saveUserAgentFilter(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        DomainConfigsDTO param = DomainConfigsDTO.builder().user_agent_black_and_white_list(config.getUserAgentBlackAndWhiteListDTO()).build();
        DomainConfigureApi.updateDomainConfigs(cdnDomain.getDomainName(), param);
    }

    @Override
    public void saveUrlAuth(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {

    }


    @Override
    public void saveHttpHeader(CdnDomain cdnDomain, SettingHigherVo config) throws BusinessException {
        DomainConfigsDTO param = DomainConfigsDTO.builder().http_response_header(config.getHttpResponseHeaders()).build();
        DomainConfigureApi.updateDomainConfigs(cdnDomain.getDomainName(), param);
    }

    @Override
    public void saveCustomErrorPageConfiguration(CdnDomain cdnDomain, SettingHigherVo config) throws BusinessException {
        DomainConfigsDTO param = DomainConfigsDTO.builder().error_code_redirect_rules(config.getErrorCodeRedirectRules()).build();
        DomainConfigureApi.updateDomainConfigs(cdnDomain.getDomainName(), param);
    }

    @Override
    public void saveCompress(CdnDomain cdnDomain, SettingHigherVo config) throws BusinessException {
        DomainConfigsDTO param = DomainConfigsDTO.builder().compress(config.getCompress()).build();
        DomainConfigureApi.updateDomainConfigs(cdnDomain.getDomainName(), param);
    }

    /**
     * 获取域名的详细配置
     *
     * @param domainName 域名
     * @return 域名配置
     */
    public DomainConfig getDomainConfig(String domainName) throws BusinessException {
        JSONObject jsonObject = null;
        JSONObject domainInfo = null;
        try {
            jsonObject = DomainConfigureApi.getDomainConfigs(domainName);
            domainInfo = DomainOperationApi.getDomainDetailByDomainName(domainName);
        } catch (CdnHuaweiException e) {
            log.error("获取域名的详细配置失败，域名：{}", domainName);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
        JSONObject jsonObjectConfig = jsonObject.getJSONObject("configs");
        // 解析主源站
        JSONObject domainConfig = domainInfo.getJSONObject("domain");
        // 解析域名基础信息
        String serviceRegion = domainConfig.getString("service_area");
        String serviceType = domainConfig.getString("business_type");
        String Status = domainConfig.getString("domain_status");
        String cname = domainConfig.getString("cname");
        String domain = domainConfig.getString("domain_name");
        Date createTime = domainConfig.getDate("create_time");
        Date updateTime = domainConfig.getDate("update_time");
        String httpsStatus = domainConfig.getString("https_status");
        String ipv6Status = jsonObjectConfig.getString("ipv6_accelerate");
        // 处理源站信息
        JSONArray originLines = domainConfig.getJSONArray("sources");
        DomainBasicInfo.SourceStationPrimaryInfo sourceStationPrimaryInfo = DomainBasicInfo.SourceStationPrimaryInfo.builder().build();
        DomainBasicInfo.SourceStationStandbyInfo sourceStationStandbyInfo = DomainBasicInfo.SourceStationStandbyInfo.builder().build();
        List<String> primaryIps = new ArrayList<>();
        List<String> standardIps = new ArrayList();
        for (int i = 0; i < originLines.size(); i++) {
            JSONObject originLine = originLines.getJSONObject(i);
            String sourceStationType = originLine.getString("origin_type");
            if (ObjectUtil.equal(originLine.getString("priority"), "70")) {
                primaryIps.add(originLine.getString("origin_addr"));
                sourceStationPrimaryInfo.setSourceStationType(sourceStationType);
                sourceStationPrimaryInfo.setHttpPort(originLine.getString("http_port"));
                sourceStationPrimaryInfo.setHttpsPort(originLine.getString("https_port"));
                sourceStationPrimaryInfo.setSourceHost(originLine.getString("host_name"));
            } else if (ObjectUtil.equal(originLine.getString("priority"), "30")) {
                standardIps.add(originLine.getString("origin_addr"));
                sourceStationStandbyInfo.setSourceStationType(sourceStationType);
                sourceStationStandbyInfo.setHttpPort(originLine.getString("http_port"));
                sourceStationStandbyInfo.setHttpsPort(originLine.getString("https_port"));
                sourceStationStandbyInfo.setSourceHost(originLine.getString("host_name"));
            }
        }
        sourceStationPrimaryInfo.setIpOrDomain(String.join(";", primaryIps));
        sourceStationStandbyInfo.setIpOrDomain(String.join(";", standardIps));

        // 处理回源配置信息
        DomainBackSourceInfo domainBackSourceInfo = new DomainBackSourceInfo();
        // 回源协议
        String originProtocol = jsonObjectConfig.getString("origin_protocol");
        String originReceiveTimeout = jsonObjectConfig.getString("origin_receive_timeout");
        String originRangeStatus = jsonObjectConfig.getString("origin_range_status");
        String sliceEtagStatus = jsonObjectConfig.getString("slice_etag_status");
        // 回源URL改写信息
        List<DomainBackSourceInfo.BackSourceUrlChange> backSourceUrlChanges = new ArrayList<>();

        JSONArray originRequestUrlRewrites = jsonObjectConfig.getJSONArray("origin_request_url_rewrite");
        backSourceUrlChanges = JSONArray.parseObject(originRequestUrlRewrites.toString(), new TypeReference<List<DomainBackSourceInfo.BackSourceUrlChange>>() {
        });
        // 高级回源信息
        List<DomainBackSourceInfo.BackSourceAdvancedInfo> backSourceAdvancedInfos = new ArrayList<>();
        JSONArray flexibleOrigins = jsonObjectConfig.getJSONArray("flexible_origin");
        if (Assert.notEmpty(flexibleOrigins)) {
            backSourceAdvancedInfos = JSONArray.parseObject(flexibleOrigins.toString(), new TypeReference<List<DomainBackSourceInfo.BackSourceAdvancedInfo>>() {
            });
        }

        List<DomainBackSourceInfo.BackSourceRequestInfo> backSourceRequestInfos = new ArrayList<>();
        JSONArray originRequestHeaders = jsonObjectConfig.getJSONArray("origin_request_header");
        backSourceRequestInfos = JSONArray.parseObject(originRequestHeaders.toString(), new TypeReference<List<DomainBackSourceInfo.BackSourceRequestInfo>>() {
        });
        domainBackSourceInfo.setOrigin_protocol(originProtocol);
        domainBackSourceInfo.setOrigin_receive_timeout(originReceiveTimeout);
        domainBackSourceInfo.setOrigin_range_status(originRangeStatus);
        domainBackSourceInfo.setSlice_etag_status(sliceEtagStatus);
        domainBackSourceInfo.setOrigin_request_url_rewrite(backSourceUrlChanges);
        domainBackSourceInfo.setFlexible_origin(backSourceAdvancedInfos);
        domainBackSourceInfo.setOrigin_request_header(backSourceRequestInfos);

        // 域名HTTPS信息
        DomainHttpsInfo domainHttpsInfo = new DomainHttpsInfo();
        JSONObject https = jsonObjectConfig.getJSONObject("https");
        JSONObject forceRedirect = jsonObjectConfig.getJSONObject("force_redirect");
        DomainHttpsInfo.HttpGetBody httpGetBody = JSONObject.parseObject(https.toJSONString(), DomainHttpsInfo.HttpGetBody.class);
        DomainHttpsInfo.ForceRedirect forceRedirectObj = JSONObject.parseObject(forceRedirect.toJSONString(), DomainHttpsInfo.ForceRedirect.class);
        httpGetBody.setCertificate_value(null);
        domainHttpsInfo.setHttps(httpGetBody);
        domainHttpsInfo.setForce_redirect(forceRedirectObj);

        // 域名缓存信息
        DomainCacheInfo domainCacheInfo = new DomainCacheInfo();
        JSONArray cacheRules = jsonObjectConfig.getJSONArray("cache_rules");
        JSONArray errorCodeCache = jsonObjectConfig.getJSONArray("error_code_cache");
        if (Assert.notEmpty(cacheRules)) {
            List<DomainCacheInfo.CacheRule> cacheRuleObj = JSONArray.parseObject(cacheRules.toJSONString(), new TypeReference<List<DomainCacheInfo.CacheRule>>() {
            });
            domainCacheInfo.setCache_rules(cacheRuleObj);
        }
        if (Assert.notEmpty(errorCodeCache)) {
            List<DomainCacheInfo.ErrorCodeCache> errorCodeCacheObj = JSONArray.parseObject(errorCodeCache.toJSONString(), new TypeReference<List<DomainCacheInfo.ErrorCodeCache>>() {
            });
            domainCacheInfo.setError_code_cache(errorCodeCacheObj);
        }

        // 域名访问信息
        DomainVisitInfo domainVisitInfo = new DomainVisitInfo();
        JSONObject referer = jsonObjectConfig.getJSONObject("referer");
        JSONObject ipFilter = jsonObjectConfig.getJSONObject("ip_filter");
        JSONObject userAgentFilter = jsonObjectConfig.getJSONObject("user_agent_filter");
        DomainVisitInfo.Referer refererObj = JSONObject.parseObject(referer.toJSONString(), DomainVisitInfo.Referer.class);
        DomainVisitInfo.IpFilter ipFilterObj = JSONObject.parseObject(ipFilter.toJSONString(), DomainVisitInfo.IpFilter.class);
        DomainVisitInfo.UserAgentFilter userAgentFilterObj = JSONObject.parseObject(userAgentFilter.toJSONString(), DomainVisitInfo.UserAgentFilter.class);
        refererObj.setValue(refererObj.getValue().replace(",", System.lineSeparator()));
        domainVisitInfo.setReferer(refererObj);
        ipFilterObj.setValue(ipFilterObj.getValue().replace(",", System.lineSeparator()));
        domainVisitInfo.setIp_filter(ipFilterObj);
        userAgentFilterObj.setValue(userAgentFilterObj.getValue().replace(",", System.lineSeparator()));
        domainVisitInfo.setUser_agent_filter(userAgentFilterObj);

        // 域名高级信息
        DomainAdvancedInfo domainAdvancedInfo = new DomainAdvancedInfo();
        JSONArray httpResponseHeader = jsonObjectConfig.getJSONArray("http_response_header");
        JSONArray errorCodeRedirectRules = jsonObjectConfig.getJSONArray("error_code_redirect_rules");
        JSONObject compress = jsonObjectConfig.getJSONObject("compress");
        if (Assert.notEmpty(httpResponseHeader)) {
            List<DomainAdvancedInfo.HttpResponseHeader> httpResponseHeaderObj = JSONArray.parseObject(httpResponseHeader.toJSONString(), new TypeReference<List<DomainAdvancedInfo.HttpResponseHeader>>() {
            });
            domainAdvancedInfo.setHttp_response_header(httpResponseHeaderObj);
        }
        if (Assert.notEmpty(errorCodeRedirectRules)) {
            List<DomainAdvancedInfo.ErrorCodeRedirectRules> errorCodeRedirectRulesObj = JSONArray.parseObject(errorCodeRedirectRules.toJSONString(), new TypeReference<List<DomainAdvancedInfo.ErrorCodeRedirectRules>>() {
            });
            domainAdvancedInfo.setError_code_redirect_rules(errorCodeRedirectRulesObj);
        }
        DomainAdvancedInfo.Compress compressObj = JSONObject.parseObject(compress.toJSONString(), DomainAdvancedInfo.Compress.class);
        domainAdvancedInfo.setCompress(compressObj);


        return DomainConfig.builder()
                .domainBasicInfo(DomainBasicInfo.builder().
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
    }


}
