package com.kuocai.cdn.service.domain.operation;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import com.kuocai.cdn.api.DomainCacheInfo;
import com.kuocai.cdn.api.DomainConfig;
import com.kuocai.cdn.api.huawei.cdn.dto.CacheRuleDTO;
import com.kuocai.cdn.api.tencent.dns.CreateRecordResponse;
import com.kuocai.cdn.api.tencent.dns.TencentApi;
import com.kuocai.cdn.api.tencent.dns.dto.CreateRecordDTO;
import com.kuocai.cdn.api.tencent.dns.properties.TencentDns;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.enumeration.domainmerage.route.CdnOperationRoute;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.MergeRouteUtil;
import com.kuocai.cdn.vo.*;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 华为火山云加速域名(CdnDomain)服务
 */

@Slf4j
@Service
public class HuaweiVolCenGineDomainServiceImpl extends BaseService<CdnDomain> implements ICdnPlatformService {

    @Resource
    private HuaweiDomainServiceImpl huawei;

    @Resource
    private VolCenGineDomainServiceImpl volCenGine;

    @Override
    public CdnDomain create(Long userId, String domainName, String businessType, String serviceArea, String originType, String ipOrDomain) throws BusinessException, InterruptedException {
        CdnDomain volCenGineDomain = volCenGine.create(userId, domainName, businessType, serviceArea, originType, ipOrDomain);
        // 创建的时候设置时间是为了和华为云保持一致
        CdnDomain huaweiDomain = huawei.create(userId, domainName, businessType, serviceArea, originType, ipOrDomain);
        huaweiDomain.setCnameVolcengine(volCenGineDomain.getCnameVolcengine());
        huaweiDomain.setRoute(CdnOperationRoute.HUAWEI_VOLCENGINE.getRoute());
        return huaweiDomain;
    }

    /**
     * 配置腾讯dns解析
     *
     * @param cdnDomain
     * @return
     * @throws TencentCloudSDKException
     */
    @Override
    public CdnDomain configDNS(CdnDomain cdnDomain) throws TencentCloudSDKException, BusinessException {
        String domainName = cdnDomain.getDomainName();
        CdnOperationRoute currentRoute = MergeRouteUtil.getCurrentRoute();
        if (Assert.isEmpty(currentRoute)) {
            log.error("火山华为云融合时间调度异常，域名:{}", cdnDomain.getDomainName());
            throw new BusinessException("系统异常，请联系系统管理员");
        }
        String route = currentRoute.getRoute();
        String cname = "";
        if (ObjectUtil.equal(route, CdnOperationRoute.HUAWEI.getRoute())) {
            cname = cdnDomain.getCnameHuawei();
        } else {
            cname = cdnDomain.getCnameVolcengine();
        }
        CreateRecordDTO createRecordRequest = new CreateRecordDTO();
        createRecordRequest.setDomain(TencentDns.LOCAL_DOMAIN_NAME).setSubDomain(domainName + "." + RandomUtil.randomString(8)).setValue(cname);
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

    @Override
    public void save(CdnDomain cdnDomain, String businessType, String serviceArea) throws BusinessException {
        volCenGine.save(cdnDomain, businessType, serviceArea);
        huawei.save(cdnDomain, businessType, serviceArea);
    }

    @Override
    public void disable(CdnDomain cdnDomain) throws BusinessException {
        volCenGine.disable(cdnDomain);
        huawei.disable(cdnDomain);
    }

    @Override
    public void enable(CdnDomain cdnDomain) throws BusinessException {
        volCenGine.enable(cdnDomain);
        huawei.enable(cdnDomain);
    }

    @Override
    public void delete(CdnDomain cdnDomain) throws BusinessException {
        volCenGine.delete(cdnDomain);
        huawei.delete(cdnDomain);
    }

    @Override
    public void ipv6(CdnDomain cdnDomain, Integer status) throws BusinessException {
        volCenGine.ipv6(cdnDomain, status);
        huawei.ipv6(cdnDomain, status);
    }

    @Override
    public void saveSourceStationConfig(CdnDomain cdnDomain, CdnDomainSourcesVo config) throws BusinessException {
        volCenGine.saveSourceStationConfig(cdnDomain, config);
        huawei.saveSourceStationConfig(cdnDomain, config);
    }

    @Override
    public void change(CdnDomain cdnDomain) throws BusinessException {
        volCenGine.change(cdnDomain);
        huawei.change(cdnDomain);
    }

    @Override
    public void saveOriginProtocol(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        volCenGine.saveOriginProtocol(cdnDomain, domainOriginSettingVo);
        huawei.saveOriginProtocol(cdnDomain, domainOriginSettingVo);
    }

    @Override
    public void saveOriginRequestUrlRewrite(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        volCenGine.saveOriginRequestUrlRewrite(cdnDomain, domainOriginSettingVo);
        huawei.saveOriginRequestUrlRewrite(cdnDomain, domainOriginSettingVo);
    }

    @Override
    public void saveAdvancedReturnSource(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        volCenGine.saveAdvancedReturnSource(cdnDomain, domainOriginSettingVo);
        huawei.saveAdvancedReturnSource(cdnDomain, domainOriginSettingVo);
    }

    @Override
    public void saveRangeSwitch(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        volCenGine.saveRangeSwitch(cdnDomain, domainOriginSettingVo);
        huawei.saveRangeSwitch(cdnDomain, domainOriginSettingVo);
    }

    @Override
    public void saveRangeVerifyETag(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        volCenGine.saveRangeVerifyETag(cdnDomain, domainOriginSettingVo);
        huawei.saveRangeVerifyETag(cdnDomain, domainOriginSettingVo);
    }

    @Override
    public void saveOriginHost(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {

    }

    @Override
    public void saveRangeTimeOut(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        volCenGine.saveRangeTimeOut(cdnDomain, domainOriginSettingVo);
        huawei.saveRangeTimeOut(cdnDomain, domainOriginSettingVo);
    }

    @Override
    public void saveOriginRequestHeader(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        volCenGine.saveOriginRequestHeader(cdnDomain, domainOriginSettingVo);
        huawei.saveOriginRequestHeader(cdnDomain, domainOriginSettingVo);
    }

    @Override
    public void httpsConfiguration(CdnDomain cdnDomain, DomainHttpsSettingVo config) throws BusinessException {
        volCenGine.httpsConfiguration(cdnDomain, config);
        huawei.httpsConfiguration(cdnDomain, config);
    }

    @Override
    public void httpsConfigurationOther(CdnDomain cdnDomain, DomainHttpsSettingVo config) throws BusinessException {
        volCenGine.httpsConfigurationOther(cdnDomain, config);
        huawei.httpsConfigurationOther(cdnDomain, config);
    }

    @Override
    public void forcedToJump(CdnDomain cdnDomain, DomainHttpsSettingVo config,String redirectCode) throws BusinessException {
        volCenGine.forcedToJump(cdnDomain, config,redirectCode);
        huawei.forcedToJump(cdnDomain, config,redirectCode);
    }

    @Override
    public void saveCacheRules(CdnDomain cdnDomain, SettingCacheVo config) throws BusinessException {
        if (config.getCacheRules().size() > 50) {
            throw new BusinessException("缓存规则数量超出限制").log();
        }
        volCenGine.saveCacheRules(cdnDomain, config);
        huawei.saveCacheRules(cdnDomain, config);
    }

    @Override
    public void saveCacheFollowOriginStatusSwitch(CdnDomain cdnDomain, SettingCacheVo config) throws BusinessException {
        volCenGine.saveCacheFollowOriginStatusSwitch(cdnDomain, config);
        huawei.saveCacheFollowOriginStatusSwitch(cdnDomain, config);
    }

    @Override
    public void saveErrorCodeCache(CdnDomain cdnDomain, SettingCacheVo config) throws BusinessException {
        volCenGine.saveErrorCodeCache(cdnDomain, config);
        huawei.saveErrorCodeCache(cdnDomain, config);
    }

    @Override
    public void saveHotlinkPrevention(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        volCenGine.saveHotlinkPrevention(cdnDomain, config);
        huawei.saveHotlinkPrevention(cdnDomain, config);
    }

    @Override
    public void saveIpBlackWhiteList(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        volCenGine.saveIpBlackWhiteList(cdnDomain, config);
        huawei.saveIpBlackWhiteList(cdnDomain, config);
    }

    @Override
    public void saveUserAgentFilter(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        volCenGine.saveUserAgentFilter(cdnDomain, config);
        huawei.saveUserAgentFilter(cdnDomain, config);
    }

    @Override
    public void saveUrlAuth(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {

    }

    @Override
    public void saveHttpHeader(CdnDomain cdnDomain, SettingHigherVo config) throws BusinessException {
        volCenGine.saveHttpHeader(cdnDomain, config);
        huawei.saveHttpHeader(cdnDomain, config);
    }

    @Override
    public void saveCustomErrorPageConfiguration(CdnDomain cdnDomain, SettingHigherVo config) throws BusinessException {
        volCenGine.saveCustomErrorPageConfiguration(cdnDomain, config);
        huawei.saveCustomErrorPageConfiguration(cdnDomain, config);
    }

    @Override
    public void saveCompress(CdnDomain cdnDomain, SettingHigherVo config) throws BusinessException {
        volCenGine.saveCompress(cdnDomain, config);
        huawei.saveCompress(cdnDomain, config);
    }

    /**
     * 查询域名信息
     *
     * @param domainName
     * @return
     * @throws BusinessException
     */
    @Override
    public DomainConfig getDomainConfig(String domainName) throws BusinessException {
        DomainConfig volCenGineDomainConfig = volCenGine.getDomainConfig(domainName);
        DomainConfig huaweiDomainConfig = huawei.getDomainConfig(domainName);
        List<DomainCacheInfo.CacheRule> cacheRuleObj = huaweiDomainConfig.getDomainCacheInfo().getCache_rules();
        DomainConfig result = new DomainConfig();
        if (ObjectUtil.equal(volCenGineDomainConfig.getDomainBasicInfo().getDomainStatus(), "configuring")) {
            result = volCenGineDomainConfig;
        } else if (ObjectUtil.equal(huaweiDomainConfig.getDomainBasicInfo().getDomainStatus(), "configuring")) {
            result = huaweiDomainConfig;
        } else {
            result = volCenGineDomainConfig;
        }
        result.getDomainCacheInfo().setCache_rules(cacheRuleObj);
        return result;
    }
}
