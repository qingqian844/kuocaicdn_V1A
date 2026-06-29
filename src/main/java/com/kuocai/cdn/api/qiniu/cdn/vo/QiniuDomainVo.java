package com.kuocai.cdn.api.qiniu.cdn.vo;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class QiniuDomainVo {

    /**
     * 域名类型: normal(普通域名)/wildcard(泛域名)
     */
    private String type;

    /**
     * 平台类型: web(网页)/download(下载)/vod(点播)/ dynamic(动态加速)
     */
    private String platform;

    /**
     * 地域: china/foreign/global
     */
    private String geoCover;

    /**
     * 协议: http/https
     */
    private String protocol;

    /**
     * 回源参数
     */
    private SourceVo source;

    /**
     * 	IP协议：仅允许ipv4访问，取值为1；同时允许ipv4/ipv6访问，取值为3。不指定IPTypes时，国内/全球域名默认为允许ipv4/ipv6访问，海外域名仅允许ipv4访问。
     */
    private Integer IpTypes;

    /**
     * 缓存配置
     */
    private CacheVo cache;
}
