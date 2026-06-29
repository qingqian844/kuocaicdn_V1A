package com.kuocai.cdn.api.huawei.cdn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 原域名所有配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class BatchCopyConfigsDTO {

    /**
     * 目标域名列表,多个域名以逗号（半角）分隔,域名数最大10个。
     */
    private String target_domain;

    /**
     * 原域名
     */
    private String source_domain;

    /**
     * 需要复制的域名配置项
     * 多个配置项以逗号（半角）分隔，支持复制的配置项：
     * originRequestHeader（回源请求头）
     * httpResponseHeader（HTTP header配置）
     * cacheUrlParamsConfig（URL参数）
     * urlAuth（URL鉴权配置）
     * userAgentBlackAndWhiteList（User-Agent黑白名单）
     * ipv6Accelerate（IPv6开关）
     * rangeStatus（Range回源）
     * cacheRules（缓存规则）
     * followOrigin（缓存遵循源站）
     * privateBucketRetrieval（私有桶回源）
     * follow302Status（回源跟随）
     * sources（源站配置）
     * compress（智能压缩）
     * referer（防盗链）
     * ipBlackAndWhiteList（IP黑白名单）
     */
    private List<String> config_list;
}
