package com.kuocai.cdn.api.huawei.cdn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * Referer过滤规则
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class RefererDTO {

    /**
     * Referer类型。取值：0代表不设置Referer过滤；1代表黑名单；2代表白名单。默认取值为0。
     * 常量类参考 RefererType
     */
    private Integer referer_type;

    /**
     * 请输入域名或IP地址，以“;”进行分割，域名、IP地址可以混合输入，支持泛域名添加。
     * 输入的域名、IP地址总数不超过100个。
     * 当设置防盗链时，此项必填。
     */
    private String referer_list;

    /**
     * 前段使用，后端进行拼接 对应各个平台
     */
    private List<String> referers;

    /**
     * 是否包含空Referer。
     * 如果是黑名单并开启该选项，则表示无referer不允许访问。
     * 如果是白名单并开启该选项，则表示无referer允许访问。
     * 默认值false。
     */
    private Boolean include_empty;
}
