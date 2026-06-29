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
public class BsIpList {

    private String domains;

    private String token;

    private BsIpListInner config;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class BsIpListInner {

        private BsIpListConfig ip_white_list;

        private BsIpListConfig ip_black_list;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class BsIpListConfig {

        /**
         * ip黑名单列表。ip格式支持/8，/16，/24的网段格式，网段间的ip不能交叉重复；最多可设置500个ip格式，多个ip格式以逗号分隔；
         * ip黑名单不能与ip白名单共存, 设置了ip黑名单, ip白名单功能将被清除。
         */
        private List<String> list;

        /**
         * append：追加模式
         * cover:覆盖模式，默认cover
         */
        private String mode;

    }
}
