package com.kuocai.cdn.api.huawei.cdn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 回源请求头
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class HttpResponseHeaderDTO {

    /**
     * 设置HTTP响应头参数。
     * 取值："Content-Disposition", "Content-Language", "Access-Control-Allow-Origin","Access-Control-Allow-Methods", "Access-Control-Max-Age", "Access-Control-Expose-Headers"或自定义头部。
     * 格式要求：长度1~100，以字母开头，可以使用字母、数字和短横杠。
     */
    public String name;

    /**
     * 设置HTTP响应头参数的值。
     * 自定义HTTP响应头参数长度范围1~256，支持字母、数字和特定字符（.-_*#!%&+|^~'"/:;,=@?）。
     */
    public String value;

    /**
     * 设置http响应头操作类型
     * 常量类参考 HttpResponseHeaderAction
     */
    public String action;

}
