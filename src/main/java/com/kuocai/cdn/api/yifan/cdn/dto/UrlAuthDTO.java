package com.kuocai.cdn.api.yifan.cdn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * url鉴权
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Accessors(chain = true)
public class UrlAuthDTO {
    /**
     * 有效期
     */
    private Long expireTime;
    /**
     * key
     */
    private String key;
    /**
     * 开关状态
     */
    private String status;
}
