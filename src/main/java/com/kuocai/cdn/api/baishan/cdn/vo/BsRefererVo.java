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
public class BsRefererVo {

    private String domains;

    private String token;

    private BsRefererVo.BsRefererConfigInner config;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class BsRefererConfigInner {

        private BsRefererVo.BsReferer referer;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class BsReferer {

        /**
         * 防盗链类型
         * 1:referer黑名单
         * 2:referer白名单
         */
        private Integer type;

        /**
         * referer列表，最多可设置200个，多个以逗号分隔；不支持正则；referer为泛域名时，请以*.开头, 例如:*.example2.com，包括任意的匹配主机头和空主机头。
         */
        private List<String> list;

        /**
         * 允许referer为空的访问
         * true:允许
         * false:不允许
         * 不填充时，默认为true。
         */
        private Boolean allow_empty;
    }
}
