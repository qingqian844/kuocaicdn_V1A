package com.kuocai.cdn.service.domain.operation;

import com.kuocai.cdn.api.DomainConfig;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.vo.*;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;

public interface ICdnPlatformService {

    /**
     * 域名基础信息设置
     **/
    CdnDomain create(Long userId, String domainName, String businessType, String serviceArea, String originType, String ipOrDomain) throws BusinessException, InterruptedException;

    default CdnDomain create(Long userId, String domainName, String businessType, String serviceArea, String originType, String ipOrDomain,
                             String originProtocol, Integer httpPort, Integer httpsPort, String originHost, Integer originWeight) throws BusinessException, InterruptedException {
        return create(userId, domainName, businessType, serviceArea, originType, ipOrDomain);
    }

    CdnDomain configDNS(CdnDomain cdnDomain) throws TencentCloudSDKException, BusinessException;

    void save(CdnDomain cdnDomain, String businessType, String serviceArea) throws BusinessException;

    void disable(CdnDomain cdnDomain) throws BusinessException;

    void enable(CdnDomain cdnDomain) throws BusinessException;

    void delete(CdnDomain cdnDomain) throws BusinessException;


    void ipv6(CdnDomain cdnDomain, Integer status) throws BusinessException;

    void saveSourceStationConfig(CdnDomain cdnDomain, CdnDomainSourcesVo config) throws BusinessException;

    void change(CdnDomain cdnDomain) throws BusinessException;

    /**
     * 域名回源信息设置
     **/
    // 修改回源配置类型
    void saveOriginProtocol(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException;

    //修改回源URL改写
    void saveOriginRequestUrlRewrite(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException;

    //修改高级回源
    void saveAdvancedReturnSource(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException;

    //修改Range回源
    void saveRangeSwitch(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException;

    //修改回源是否校验ETag
    void saveRangeVerifyETag(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException;

    void saveOriginHost(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException;

    //修改回源超时时间
    void saveRangeTimeOut(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException;

    //修改回源请求头
    void saveOriginRequestHeader(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException;

    /**
     * 域名HTTPS信息配置
     **/
    // Https配置和TLS版本配置和HTTP/2配置和OCSP Stapling配置
    void httpsConfiguration(CdnDomain cdnDomain, DomainHttpsSettingVo config) throws BusinessException;

    void httpsConfigurationOther(CdnDomain cdnDomain, DomainHttpsSettingVo config) throws BusinessException;

    // 强制跳转
    void forcedToJump(CdnDomain cdnDomain, DomainHttpsSettingVo config, String redirectCode) throws BusinessException;

    /**
     * 域名缓存信息配置
     **/
    // 保存缓存规则
    void saveCacheRules(CdnDomain cdnDomain, SettingCacheVo config) throws BusinessException;

    // 缓存遵循源站 未使用
    void saveCacheFollowOriginStatusSwitch(CdnDomain cdnDomain, SettingCacheVo config) throws BusinessException;

    // 状态码缓存时间
    void saveErrorCodeCache(CdnDomain cdnDomain, SettingCacheVo config) throws BusinessException;

    /**
     * 域名访问信息配置
     **/
    // 保存域名管理访问配置-防盗链信息
    void saveHotlinkPrevention(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException;

    // 保存域名管理访问配置-IP黑白名单信息
    void saveIpBlackWhiteList(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException;

    // 保存域名管理访问配置-User-Agent黑白名单信息
    void saveUserAgentFilter(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException;

    void saveUrlAuth(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException;

    /**
     * 域名高级信息配置
     **/
    // 保存HTTP header配置信息
    void saveHttpHeader(CdnDomain cdnDomain, SettingHigherVo config) throws BusinessException;

    /**
     * 保存自定义错误页面配置信息
     * 备注：暂未使用
     */

    void saveCustomErrorPageConfiguration(CdnDomain cdnDomain, SettingHigherVo config) throws BusinessException;

    // 保存智能压缩
    void saveCompress(CdnDomain cdnDomain, SettingHigherVo config) throws BusinessException;

    DomainConfig getDomainConfig(String domainName) throws BusinessException;
}
