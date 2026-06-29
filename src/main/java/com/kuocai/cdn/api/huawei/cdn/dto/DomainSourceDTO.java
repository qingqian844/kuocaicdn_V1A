package com.kuocai.cdn.api.huawei.cdn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 源站域名或源站IP
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class DomainSourceDTO {

    /**
     * 加速域名id
     */
    private String domain_id;

    /**
     * 源站IP
     */
    private String ip_or_domain;

    /**
     * 源站类型
     * 参考常量类 OriginType
     */
    private String origin_type;

    /**
     * 主备状态
     * 参考常量类 ActiveStandby
     */
    private Integer active_standby;

    /**
     * 是否开启Obs静态网站托管(0表示关闭,1表示则为开启)，源站类型为obs_bucket时传递。
     * 常量类参考 ObsWebHosting
     */
    private Integer enable_obs_web_hosting;
}
