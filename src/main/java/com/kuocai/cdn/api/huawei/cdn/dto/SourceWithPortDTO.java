package com.kuocai.cdn.api.huawei.cdn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 源站信息
 * 源站域名或源站IP，源站为IP类型时，仅支持IPv4；
 * 如需传入多个源站IP，以多个源站对象传入，除IP其他参数请保持一致，主源站最多支持15个源站IP对象，备源站最多支持15个源站IP对象；
 * 源站为域名类型时仅支持1个源站对象。不支持IP源站和域名源站混用。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class SourceWithPortDTO {

    /**
     * 加速域名id
     */
    private String domain_id;

    /**
     * 源站IP（非内网IP）或者域名
     */
    private String ip_or_domain;

    /**
     * 源站类型
     * 常量类参考 OriginType
     */
    private String origin_type;

    /**
     * 主备状态
     * 常量类参考 ActiveStandby
     */
    private Integer active_standby;

    /**
     * 是否开启Obs静态网站托管，源站类型为obs_bucket时传递。
     * 常量类参考 ObsWebHosting
     */
    private Integer enable_obs_web_hosting;

    /**
     * HTTP端口，默认80
     */
    private Integer http_port;

    /**
     * HTTPS端口，默认443
     */
    private Integer https_port;
}
