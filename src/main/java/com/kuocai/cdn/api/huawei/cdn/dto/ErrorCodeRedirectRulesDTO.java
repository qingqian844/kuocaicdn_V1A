package com.kuocai.cdn.api.huawei.cdn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 自定义错误页面
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ErrorCodeRedirectRulesDTO {

    /**
     * 重定向的错误码，当前支持以下状态码 4xx:400, 403, 404, 405, 414, 416, 451 5xx:500, 501, 502, 503, 504
     */
    private String error_code;

    /**
     * 重定向状态码，取值为301或302
     */
    private String target_code;

    /**
     * 重定向的目标链接
     */
    private String target_link;
}
