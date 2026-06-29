package com.kuocai.cdn.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 域名全量信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainConfig {

    /**
     * 域名基础信息
     */
    private DomainBasicInfo domainBasicInfo;

    /**
     * 回源配置信息
     */
    private DomainBackSourceInfo domainBackSourceInfo;

    /**
     * HTTPS配置信息
     */
    private DomainHttpsInfo domainHttpsInfo;

    /**
     * 缓存配置i信息
     */
    private DomainCacheInfo domainCacheInfo;

    /**
     * 访问配置信息
     */
    private DomainVisitInfo domainVisitInfo;

    /**
     * 高级配置信息
     */
    private DomainAdvancedInfo domainAdvancedInfo;
}
