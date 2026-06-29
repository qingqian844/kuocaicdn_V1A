package com.kuocai.cdn.api.yifan.cdn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Accessors(chain = true)
public class HotlinkPreventionDTO {

    /**
     * 关闭 0
     * 黑名单 1
     * 白名单 2
     */
    private Integer refererType;

    /**
     * 配置规则 分号拼接
     */
    private String refererList;

    /**
     * 包含空referer
     */
    private Boolean includeEmpty;
}
