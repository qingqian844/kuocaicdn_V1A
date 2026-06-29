package com.kuocai.cdn.api.huawei.cdn.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 缓存配置对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CacheRuleDTO {

    /**
     * 缓存规则类型
     * 常量类参考 RuleType
     */
    private String match_type;

    /**
     * 缓存匹配设置
     * 当rule_type为0时，为空。
     * 当rule_type为1时，为文件后缀，输入首字符为“.”，以“;”进行分隔，如.jpg;.zip;.exe，并且输入的文件名后缀总数不超过20个。
     * 当rule_type为2时，为目录，输入要求以“/”作为首字符，以“;”进行分隔，如/test/folder01;/test/folder02，并且输入的目录路径总数不超过20个。
     * 当rule_type为3时，为全路径，输入要求以“/”作为首字符，支持匹配指定目录下的具体文件，或者带通配符“*”的文件，如/test/index.html或/test/*.jpg。
     */
    private String match_value;

    /**
     * 缓存时间。最大支持365天。
     */
    private Integer ttl;

    /**
     * 缓存时间单位
     * 常量类参考 TtlType
     */
    private String ttl_unit;

    /**
     * 此条配置的权重值, 默认值1，数值越大，优先级越高。
     * 取值范围为1-100，权重值不能相同。
     */
    private Integer priority;

    /**
     * 设置URL参数
     */
    private String url_parameter_type;

    /**
     * 设置URL参数值
     */
    private String url_parameter_value;

    /**
     * 是否遵循源站
     */
    private String follow_origin;
}
