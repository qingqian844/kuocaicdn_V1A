package com.kuocai.cdn.api.yifan.cdn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;


/**
 * 强制重定向
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Accessors(chain = true)
public class ForceRedirectDTO {
    /**
     * 开关状态
     */
    private String status;
    /**
     * 重定向类型
     */
    private String type;
}


