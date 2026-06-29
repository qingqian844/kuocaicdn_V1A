package com.kuocai.cdn.api.huawei.cdn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 改写高级回源返回
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class BackSourceDTO {

    /**
     * 源站IP（非内网IP）或者域名
     */
    private String ip_or_domain;

    /**
     * 源站类型
     * 常量类参考 OriginType
     */
    private String sources_type;
}
