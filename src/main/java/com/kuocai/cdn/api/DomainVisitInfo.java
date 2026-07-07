package com.kuocai.cdn.api;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import com.kuocai.cdn.vo.EdgeOneSecurityPolicyVo;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DomainVisitInfo {

    private Referer referer;

    private IpFilter ip_filter;

    private UserAgentFilter user_agent_filter;

    private UrlAuth url_auth;

    private EdgeOneSecurityPolicyVo edgeone_security_policy;


    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Referer {

        /**
         * Referer黑白名单类型 off：关闭Referer黑白名单; black：Referer黑名单; white：Referer白名单;
         */
        private String type;

        /**
         * 0 关闭
         * 1 黑
         * 2 白
         */
        private Integer referer_type;

        /**
         * 域名或IP地址，以“换行”进行分割，域名、IP地址可以混合输入，支持泛域名添加。域名、IP地址总数不超过400个，取值范围1-65535。
         */
        private String value;

        /**
         * 是否包含空Referer。如果是黑名单并开启该选项，则表示无referer不允许访问。如果是白名单并开启该选项，则表示无referer允许访问。默认值false。
         */
        private Boolean include_empty;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class IpFilter {

        /**
         * IP黑白名单类型 off：关闭IP黑白名单; black：IP黑名单; white：IP白名单;
         */
        private String type;

        /**
         * 配置IP黑白名单，当type=off时，非必传。多条规则用“,”分割。
         */
        private String value;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserAgentFilter {

        /**
         * UA黑白名单类型 off：关闭UA黑白名单; black：UA黑名单; white：UA白名单;
         */
        private String type;

        /**
         * 配置UA黑白名单，当type=off时，非必传。最多配置10条规则，单条规则不超过100个字符，多条规则用“,”分割。
         */
        private String value;

        /**
         * 是否包含空Referer。如果是黑名单并开启该选项，则表示无referer不允许访问。如果是白名单并开启该选项，则表示无referer允许访问。默认值false。
         */
        private List<String> ua_list;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UrlAuth {
        private String status;
        private String type;
        private String primary_key;
        private String secondary_key;
        private Long expire_time;
    }
}
