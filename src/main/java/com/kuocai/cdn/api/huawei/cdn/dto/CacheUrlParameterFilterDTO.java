package com.kuocai.cdn.api.huawei.cdn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 缓存url参数配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CacheUrlParameterFilterDTO {

    /**
     * 参数值，多个参数使用分号分隔
     */
    private String status;

    /**
     * 缓存URL参数操作类型（full_url：缓存所有参数，ignore_url_params：忽略所有参数，del_params：忽略指定URL参数，reserve_params：保留指定URL参数）。
     */
    private String type;
}
