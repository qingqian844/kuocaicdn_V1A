package com.kuocai.cdn.api.huawei.cdn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 状态码缓存时间
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ErrorCodeCacheDTO {

    /**
     * 允许配置的错误码: 400, 403, 404, 405, 414, 500, 501, 502, 503, 504
     */
    private Integer code;

    /**
     * 错误码缓存时间，单位为秒，范围0-31,536,000(一年默认为365天)
     */
    private Integer ttl;
}
