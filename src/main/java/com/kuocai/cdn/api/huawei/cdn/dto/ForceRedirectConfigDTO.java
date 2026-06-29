package com.kuocai.cdn.api.huawei.cdn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 强制重定向
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ForceRedirectConfigDTO {

    /**
     * 强制跳转开关（on：打开，off：关闭）
     */
    private String status;

    /**
     * 强制跳转类型（http：强制跳转HTTP，https：强制跳转HTTPS）。
     */
    private String type;

    /**
     * 跳转方式
     */
    private Integer redirect_code;
}
