package com.kuocai.cdn.api.baishan.cdn.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BsCacheVo {

    private String token;

    private String domains;

    private BsCacheConfig config;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class BsCacheConfig {
        private List<BsCacheRule> cache_rule_list;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class BsCacheRule {

        /**
         * 缓存内容的类型:
         * ext:文件名后缀
         * dir:目录
         * route:完整路径匹配
         * all: 缓存所有
         */
        private String match_method;

        /**
         * 匹配形式，多个以逗号分隔, 例如:
         * match_method=ext时，可以填jpg,png,gif
         * match_method=dir时，可以填/product/index,/user/index
         * match_method=route时，可以填/index.html,/user/get?index
         * match_method=all, 可以填写.*
         */
        private String pattern;

        /**
         * 过期时间
         * 缓存时间。与expire_unit组合生效, 最大缓存时间不超过2年。
         * 当expire=0时, 对指定pattern禁止缓存。
         */
        private int expire;

        /**
         * 过期单位
         * 缓存时间的时间单位:
         * Y:年
         * M:月
         * D:日
         * h:小时
         * i:分
         * s:秒
         * 不填充时，默认值为s。
         */
        private String expire_unit;

        /**
         * 优先级。数值越小的pattern优先级越高，优先生效。
         */
        private int priority;

        /**
         * 忽略pattern的大小写：
         * yes:忽略
         * no:不忽略。
         * 不填充时，默认值为yes。
         */
        private String case_ignore;

        /**
         * 问号后参数处理方式:
         * do_nothing:不处理
         * cache_back_source_remove:缓存/回源均去除
         * 不填充时, 默认值为do_nothing。
         */
        private String query_params_op;

        /**
         * 忽略源站响应头中的不缓存信息，比如Cache-Control:no-cache等:
         * no:不忽略
         * yes:忽略
         * 不填充时，默认值为no。
         */
        private String ignore_no_cache_headers;

        /**
         * 遵循源站缓存时间:
         * no:不遵循
         * yes:遵循
         * 不填充时, 默认值为no。
         */
        private String follow_expired;
    }
}
