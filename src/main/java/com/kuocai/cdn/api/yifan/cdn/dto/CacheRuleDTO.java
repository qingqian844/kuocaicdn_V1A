package com.kuocai.cdn.api.yifan.cdn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 域名缓存配置
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Accessors(chain = true)
public class CacheRuleDTO {
    /**
     * 是否开启压缩
     */
    private Integer compress;
    /**
     * 是否跟随源站
     */
    private Boolean followOrigin;
    /**
     * 是否忽略url参数
     */
    private Boolean ignoreUrlParameter;
    /**
     * 缓存规则
     */
    private List<CacheRuleDetail> rules;

    /**
     * 缓存规则
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Accessors(chain = true)
    class CacheRuleDetail {
        /**
         * 规则内容
         */
        private String content;
        /**
         * 此条配置的权重值, 默认值1，数值越大，优先级越高。取值范围为1-100，权重值不能相同。
         */
        private Long priority;
        /**
         * 0：全部类型，表示匹配所有文件，默认值。1：文件类型，表示按文件后缀匹配。2：文件夹类型，表示按目录匹配。  3：文件全路径类型，表示按文件全路径匹配。
         */
        private Long ruleType;
        /**
         * ttl
         */
        private Long ttl;
        /**
         * 1:秒,2:分钟,3:小时,4:天
         */
        private Long ttlType;
    }
}



