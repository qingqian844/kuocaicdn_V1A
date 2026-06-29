package com.kuocai.cdn.api.huawei.cdn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 回源HOST对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class OriginHostBodyDTO {

    /**
     * 回源类型
     * 常量类参考 OriginHostType
     */
    private String origin_host_type;

    /**
     * 自定义回源域名，origin_host_type为 customize时传入该参数。
     */
    private String customize_domain;

}
