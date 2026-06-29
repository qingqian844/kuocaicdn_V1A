package com.kuocai.cdn.api.huawei.cdn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 缓存配置对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CacheConfigRequestDTO {

    /**
     * 是否忽略url中的参数
     */
    private Boolean ignore_url_parameter;

    /**
     * 缓存规则是否遵循源站
     */
    private Boolean follow_origin;

    /**
     * GZIP压缩
     */
    private CompressRequestDTO compress;

    /**
     * 缓存规则
     */
    private List<RulesDTO> rules;
}
