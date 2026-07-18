package com.kuocai.cdn.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainBackSourceInfo {


    /**
     * 回源方式 http  https follow
     */
    private String origin_protocol;

    /**
     * 白山云使用
     */
    private Integer port;

    /**
     * 回源超时时间 5-60
     */
    private String origin_receive_timeout;

    /**
     * Range回源 on/off
     */
    private String origin_range_status;

    /**
     * 回源是否校验ETag on/off
     */
    private String slice_etag_status;

    private String upstream_follow_redirect_status;

    private Integer upstream_follow_redirect_max_times;

    /**
     * 回源URL改写
     */
    private List<BackSourceUrlChange> origin_request_url_rewrite;

    /**
     * 高级回源
     */
    private List<BackSourceAdvancedInfo> flexible_origin;

    /**
     * 回源请求头
     */
    private List<BackSourceRequestInfo> origin_request_header;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BackSourceUrlChange {

        /**
         * 回源URL改写规则的优先级。
         */
        private Integer priority;

        /**
         * 匹配类型， all：所有文件， file_path：URI路径， wildcard：通配符。 full_path: 全路径
         */
        private String match_type;

        /**
         * 需要替换的URI。 以正斜线（/）开头的URI，不含http(s)://头及域名。
         * 长度不超过512个字符。 支持通配符*匹配，如：/test/* /*.mp4。 匹配方式为“所有文件”时，不支持配置参数。
         */
        private String source_url;

        /**
         * 以正斜线（/）开头的URI，不含http(s)://头及域名。 长度不超过256个字符。 通配符 * 可通过$n捕获（n=1,2,3...，例如：/newtest/$1/$2.jpg）
         */
        private String target_url;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BackSourceAdvancedInfo {

        /**
         * 回源URL改写规则的优先级。
         */
        private Integer priority;

        /**
         * 匹配类型， all：所有文件， file_path：URI路径， wildcard：通配符。 full_path: 全路径
         */
        private String match_type;

        /**
         * file_extension（文件后缀）： 支持所有格式的文件类型。 输入首字符为“.”，以“;”进行分隔。
         * 输入的文件后缀名总数不能超过20个。 file_path（目录路径）：输入要求以“/”作为首字符，以“;”进行分隔，输入的目录路径总数不能超过20个。
         */
        private String match_pattern;

        /**
         *
         */
        private List<BackSources> back_sources;


        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class BackSources {
            private String sources_type;

            private String ip_or_domain;

            private String obs_bucket_type;
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BackSourceRequestInfo {

        private String name;

        private String value;

        /**
         * 回源请求头设置类型。delete：删除，set：
         * 设置。同一个请求头字段只允许删除或者设置。设置：若原始回源请求中不存在该字段，先执行新增再执行设置。
         */
        private String action;

    }

}
