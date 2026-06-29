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
public class BsDomainVo {

    private String domain;

    private String area;

    private String type;

    private BsBackSourceConfig config;


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class BsBackSourceConfig {

        /**
         * 回源参数
         */
        private BsOrigin origin;

        /**
         * 回源host
         */
        private BsOriginHost origin_host;

    }
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class BsOrigin {

        /**
         * 主回源, 可填ip, 域名, 多个以逗号(,)分隔
         * 例如: "133.233.133.33,133.233.22.33", 主源/备源最多6个ip
         */
        private String default_master;

        /**
         * 备回源, 可填ip, 域名, 多个以逗号(,)分隔
         * 例如: "133.233.133.33,133.233.22.33"
         * 注：主备源不可设置为一样, 不可与主源存在重复
         */
        private String default_slave;

        /**
         * 回源方式：
         * default：以用户访问的协议和端口回源
         * http：http协议80端口回源
         * https：https协议443端口回源
         * custom：自定义协议和端口回源，需要设置ori_https和port参数。
         * 不填充时，默认值为default。
         */
        private String origin_mode;

        /**
         * 当origin_mode=custom时, 需要设置。
         * 是否https协议回源：
         * yes：是
         * no：否
         */
        private String ori_https;

        /**
         * 当origin_mode=custom时, 需要设置。
         * 回源端口，可选值范围：（0-65535）
         */
        private Integer port;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class BsOriginHost {

        /**
         * 	回源host
         */
        private String host;
    }
}
