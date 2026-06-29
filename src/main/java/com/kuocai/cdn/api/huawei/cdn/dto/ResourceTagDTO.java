package com.kuocai.cdn.api.huawei.cdn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 资源标签
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ResourceTagDTO {

    /**
     * 键。长度1~128个字符, 可用 UTF-8 格式表示的字母(包含中文)、数字和空格，以及以下字符： _ . : = + - @
     */
    private String key;

    /**
     * 值。长度0~255个字符, 可用 UTF-8 格式表示的字母(包含中文)、数字和空格，以及以下字符： _ . : / = + - @
     */
    private String value;
}
