package com.kuocai.cdn.api.yifan.cdn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 回原请求头
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Accessors(chain = true)
public class RequestHeaderDTO {
    /**
     * 操作类型
     */
    private String action;
    /**
     * 名称
     */
    private String name;
    /**
     * 值
     */
    private String value;
}

