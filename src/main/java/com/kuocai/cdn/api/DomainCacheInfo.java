package com.kuocai.cdn.api;

import com.kuocai.cdn.vo.IgnoreQueryStringDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DomainCacheInfo {

    private List<CacheRule> cache_rules;

    private List<ErrorCodeCache> error_code_cache;

    private IgnoreQueryStringDTO ignore_query_string;


    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CacheRule {

        /**
         * 匹配所有文件 all， 按文件后缀匹配 file_extension， 按目录匹配 catalog， 全路径匹配 full_path， 按首页匹配 home_page。
         */
        private String match_type;
        /**
         * 缓存匹配设置。
         * 当match_type为all时，为空。
         * 当match_type为file_extension时，为文件后缀，输入首字符为“.”，以“,”进行分隔， 如.jpg,.zip,.exe，并且输入的文件名后缀总数不超过20个。
         * 当match_type为catalog时，为目录，输入要求以“/”作为首字符， 以“,”进行分隔，如/test/folder01,/test/folder02，并且输入的目录路径总数不超过20个。
         * 当match_type为full_path时，为全路径，输入要求以“/”作为首字符，支持匹配指定目录下的具体文件，或者带通配符“*”的文件， 如/test/index.html或/test/*.jpg。
         * 当match_type为home_page时，为空。
         */
        private String match_value;
        /**
         * 缓存过期时间。最大支持365天。
         */
        private Integer ttl;
        /**
         * 缓存过期时间单位。s：秒；m：分；h：小时；d：天
         */
        private String ttl_unit;
        /**
         * 此条配置的优先级, 默认值1，数值越大，优先级越高。取值范围为1-100，优先级不能相同
         */
        private Integer priority;
        /**
         * 缓存遵循源站开关（on：打开，off：关闭）
         */
        private String follow_origin;
        /**
         * 忽略指定URL参数： del_params， 保留指定URL参数： reserve_params， 忽略全部URL参数： ignore_url_params， 使用完整URL参数： full_url。
         */
        private String url_parameter_type;
        /**
         * 最多设置10条，以","分隔
         */
        private String url_parameter_value;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ErrorCodeCache {

        /**
         * 允许配置的错误码: 400, 403, 404, 405, 414, 500, 501, 502, 503, 504
         */
        private Integer code;

        /**
         * 错误码缓存时间，单位为秒，范围0-31,536,000(一年默认为365天)
         */
        private Integer ttl;
    }
}
