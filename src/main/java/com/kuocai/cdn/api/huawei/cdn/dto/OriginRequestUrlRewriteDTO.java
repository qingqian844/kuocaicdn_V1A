package com.kuocai.cdn.api.huawei.cdn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 改写回源URL，最多配置20条。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class OriginRequestUrlRewriteDTO {

    /**
     * 回源URL改写规则的优先级。 优先级设置具有唯一性，不支持多条回源URL改写规则设置同一优先级，且优先级不能输入为空。 多条规则下，不同规则中的相同资源内容，CDN按照优先级高的规则执行URL改写。 取值为1~100之间的整数，数值越大优先级越高。
     */
    private Integer priority;

    /**
     * 匹配类型 all：所有文件 file_path：URL路径 wildcard：通配符
     */
    private String match_type;

    /**
     * 需要替换的URL。 以正斜线（/）开头的URL，不含http(s)://头及域名。 长度不超过512个字符。
     */
    private String source_url;

    /**
     * 以正斜线（/）开头的URL，不含http(s)://头及域名。 长度不超过256个字符。 通配符 * 可通过$n捕获（n=1,2,3...，例如：/newtest/$1/$2.jpg）。
     */
    private String target_url;
}
