package com.kuocai.cdn.api.yifan.cdn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 域名http响应头设置
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Accessors(chain = true)
public class ResponseHeaderDTO {
    /**
     * 操作类型
     * delete/set
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
