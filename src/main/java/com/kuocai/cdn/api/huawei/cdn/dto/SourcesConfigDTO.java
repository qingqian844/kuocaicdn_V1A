package com.kuocai.cdn.api.huawei.cdn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 源站配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class SourcesConfigDTO {

    /**
     * OriginType
     */
    private String origin_addr;

    /**
     * 源站类型
     * 常量类参考 OriginType
     */
    private String origin_type;

    /**
     * 源站优先级（70：主，30：备）。
     * 常量类参考 SourcePriority
     */
    private Integer priority;

    /**
     * 是否开启Obs静态网站托管，源站类型为obs_bucket时传递(off：关闭，on：开启)。
     */
    private String obs_web_hosting_status;

    /**
     * HTTP端口，默认80。
     */
    private Integer http_port;

    /**
     * 回源HOST，默认加速域名。
     */
    private Integer https_port;

    /**
     * 回源HOST，默认加速域名。
     */
    private String host_name;
}
