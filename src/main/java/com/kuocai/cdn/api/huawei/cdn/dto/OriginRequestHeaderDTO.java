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
public class OriginRequestHeaderDTO {

    /**
     * 设置回源请求头参数。
     * 格式要求：长度1~64，由数字，大小写字母，中划线-组成。
     */
    public String name;

    /**
     * 设置回源请求头参数的值。
     * 当为删除动作时，可不填。
     * 格式要求：长度1~512。不支持中文，不支持变量配置，如：$client_ip,$remote_port等。
     */
    public String value;

    /**
     * 回源请求类设置类型
     * 常量类参考 OriginRequestHeaderAction
     */
    public String action;

}
