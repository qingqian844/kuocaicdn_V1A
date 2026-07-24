package com.kuocai.cdn.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainBasicInfo {

    /**
     * 加速域名
     */
    private String domainName;

    /**
     * 域名状态
     */
    private String domainStatus;

    /**
     * 供应商返回的配置失败原因。
     */
    private String failureReason;

    /**
     * https是否开启状态 1/0
     */
    private String httpsStatus;

    /**
     * 域名cname
     */
    private String cname;

    /**
     * 业务类型
     * web download video
     */
    private String businessType;

    /**
     * 服务范围
     * mainland_china中国大陆、outside_mainland_china中国境外、global全球。
     */
    private String serviceArea;


    /**
     * IPv6开关 1/0
     */
    private String isIpv6;

    private Date createTime;

    private Date updateTime;

    private SourceStationPrimaryInfo sourceStationPrimaryInfo;

    private SourceStationStandbyInfo sourceStationStandbyInfo;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SourceStationPrimaryInfo {

        private String sourceStationType;

        /**
         * ip或者域名 使用分号拼接
         */
        private String ipOrDomain;

        /**
         * 回源端口HTTP
         */
        private String httpPort;

        /**
         * 回源端口HTTPS
         */
        private String httpsPort;

        /**
         * 回源HOST
         */
        private String sourceHost;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SourceStationStandbyInfo {

        private String sourceStationType;

        /**
         * ip或者域名 使用分号拼接
         */
        private String ipOrDomain;

        /**
         * 回源端口HTTP
         */
        private String httpPort;

        /**
         * 回源端口HTTPS
         */
        private String httpsPort;

        /**
         * 回源HOST
         */
        private String sourceHost;
    }
}
