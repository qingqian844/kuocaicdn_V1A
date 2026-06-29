package com.kuocai.cdn.service.domain.operation.support;

import com.kuocai.cdn.api.DomainConfig;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.service.domain.operation.ICdnPlatformService;
import com.kuocai.cdn.vo.*;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;

public abstract class AbstractUnsupportedCdnPlatformService extends BaseService<CdnDomain> implements ICdnPlatformService {

    protected BusinessException unsupported(String action) {
        return new BusinessException(getPlatformName() + "暂不支持" + action);
    }

    protected abstract String getPlatformName();

    @Override
    public CdnDomain configDNS(CdnDomain cdnDomain) throws TencentCloudSDKException, BusinessException {
        throw unsupported("自动配置 DNS");
    }

    @Override
    public void save(CdnDomain cdnDomain, String businessType, String serviceArea) throws BusinessException {
        throw unsupported("保存基础配置");
    }

    @Override
    public void disable(CdnDomain cdnDomain) throws BusinessException {
        throw unsupported("停用域名");
    }

    @Override
    public void enable(CdnDomain cdnDomain) throws BusinessException {
        throw unsupported("启用域名");
    }

    @Override
    public void delete(CdnDomain cdnDomain) throws BusinessException {
        throw unsupported("删除域名");
    }

    @Override
    public void ipv6(CdnDomain cdnDomain, Integer status) throws BusinessException {
        throw unsupported("IPv6 配置");
    }

    @Override
    public void saveSourceStationConfig(CdnDomain cdnDomain, CdnDomainSourcesVo config) throws BusinessException {
        throw unsupported("源站配置");
    }

    @Override
    public void change(CdnDomain cdnDomain) throws BusinessException {
        throw unsupported("主备源切换");
    }

    @Override
    public void saveOriginProtocol(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        throw unsupported("回源协议配置");
    }

    @Override
    public void saveOriginRequestUrlRewrite(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        throw unsupported("回源 URL 改写");
    }

    @Override
    public void saveAdvancedReturnSource(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        throw unsupported("高级回源");
    }

    @Override
    public void saveRangeSwitch(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        throw unsupported("Range 回源");
    }

    @Override
    public void saveRangeVerifyETag(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        throw unsupported("ETag 校验");
    }

    @Override
    public void saveOriginHost(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        throw unsupported("回源 HOST 配置");
    }

    @Override
    public void saveRangeTimeOut(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        throw unsupported("回源超时配置");
    }

    @Override
    public void saveOriginRequestHeader(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        throw unsupported("回源请求头配置");
    }

    @Override
    public void httpsConfiguration(CdnDomain cdnDomain, DomainHttpsSettingVo config) throws BusinessException {
        throw unsupported("HTTPS 配置");
    }

    @Override
    public void httpsConfigurationOther(CdnDomain cdnDomain, DomainHttpsSettingVo config) throws BusinessException {
        throw unsupported("HTTPS 扩展配置");
    }

    @Override
    public void forcedToJump(CdnDomain cdnDomain, DomainHttpsSettingVo config, String redirectCode) throws BusinessException {
        throw unsupported("强制跳转配置");
    }

    @Override
    public void saveCacheRules(CdnDomain cdnDomain, SettingCacheVo config) throws BusinessException {
        throw unsupported("缓存规则配置");
    }

    @Override
    public void saveCacheFollowOriginStatusSwitch(CdnDomain cdnDomain, SettingCacheVo config) throws BusinessException {
        throw unsupported("缓存遵循源站配置");
    }

    @Override
    public void saveErrorCodeCache(CdnDomain cdnDomain, SettingCacheVo config) throws BusinessException {
        throw unsupported("状态码缓存配置");
    }

    @Override
    public void saveHotlinkPrevention(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        throw unsupported("防盗链配置");
    }

    @Override
    public void saveIpBlackWhiteList(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        throw unsupported("IP 黑白名单配置");
    }

    @Override
    public void saveUserAgentFilter(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        throw unsupported("User-Agent 黑白名单配置");
    }

    @Override
    public void saveUrlAuth(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        throw unsupported("URL 鉴权配置");
    }

    @Override
    public void saveHttpHeader(CdnDomain cdnDomain, SettingHigherVo config) throws BusinessException {
        throw unsupported("HTTP Header 配置");
    }

    @Override
    public void saveCustomErrorPageConfiguration(CdnDomain cdnDomain, SettingHigherVo config) throws BusinessException {
        throw unsupported("自定义错误页配置");
    }

    @Override
    public void saveCompress(CdnDomain cdnDomain, SettingHigherVo config) throws BusinessException {
        throw unsupported("智能压缩配置");
    }

    @Override
    public DomainConfig getDomainConfig(String domainName) throws BusinessException {
        throw unsupported("配置详情查询");
    }
}
