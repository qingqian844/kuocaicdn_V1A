package com.kuocai.cdn.api.huawei.cdn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 域名配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class DomainConfigsDTO {

    /**
     * 回源请求头改写 该功能将覆盖原有配置（清空之前的配置），在使用此接口时，请上传全量头部信息。
     */
    private List<OriginRequestHeaderDTO> origin_request_header;

    /**
     * http header配置 该功能将覆盖原有配置（清空之前的配置），在使用此接口时，请上传全量头部信息。
     */
    private List<HttpResponseHeaderDTO> http_response_header;

    /**
     * url鉴权
     */
    private UrlAuthDTO url_auth;

    /**
     * 证书设置
     */
    private HttpPutBodyDTO https;

    /**
     * 源站配置
     */
    private List<SourcesConfigDTO> sources;

    /**
     * 回源协议（follow：协议跟随回源，http：HTTP回源(默认)，https：https回源）。
     */
    private String origin_protocol;

    /**
     * 强制重定向
     */
    private ForceRedirectConfigDTO force_redirect;

    /**
     * 智能压缩
     */
    private CompressDTO compress;

    /**
     * 缓存url参数配置
     */
    private CacheUrlParameterFilterDTO cache_url_parameter_filter;

    /**
     * ipv6设置（1：打开；0：关闭）
     */
    private Integer ipv6_accelerate;

    /**
     * 状态码缓存时间
     */
    private List<ErrorCodeCacheDTO> error_code_cache;

    /**
     * Range回源，即分片回源 开启Range回源的前提是您的源站支持Range请求，即HTTP请求头中包含Range字段，否则可能导致回源失败。 开启: on 关闭: off
     */
    private String origin_range_status;

    /**
     * User-Agent黑白名单
     */
    private UserAgentFilterDTO user_agent_filter;

    /**
     * 改写回源URL，最多配置20条。
     */
    private List<OriginRequestUrlRewriteDTO> origin_request_url_rewrite;

    /**
     * 改写高级回源，最多配置20条。
     */
    private List<FlexibleOriginDTO> flexible_origin;

    /**
     * 自定义错误页面
     */
    private List<ErrorCodeRedirectRulesDTO> error_code_redirect_rules;

    /**
     * 加速域名类型
     */
    private String business_type;

    /**
     * 加速域名服务范围
     */
    private String service_area;

    /**
     * 修改回源是否校验ETag
     */
    private String slice_etag_status;

    /**
     * 回源超时时间
     */
    private Integer origin_receive_timeout;

    /**
     * 域名缓存规则配置
     */
    private List<CacheRuleDTO> cache_rules;

    /**
     * 缓存遵循源站
     */
    private String cache_follow_origin_status;

    /**
     * User-Agent黑白名单
     */
    private UserAgentBlackAndWhiteListDTO user_agent_black_and_white_list;

}
