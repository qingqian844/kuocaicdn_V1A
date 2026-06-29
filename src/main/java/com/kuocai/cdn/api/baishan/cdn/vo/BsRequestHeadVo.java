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
public class BsRequestHeadVo {

    private String domains;

    private String token;

    private BsRequestHeadConfig config;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class BsRequestHeadConfig {

        private HeadControl head_control;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class HeadControl {

       private List<RequestHeadInfo> list;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class RequestHeadInfo {

        /**
         * 默认使用.*
         */
        private String regex;

        /**
         * 操作内容,ADD,DEL,ALT
         */
        private String head_op;

        /**
         * 方向,SER_REQ回源请求头
         */
        private String head_direction;

        /**
         * HTTP名称
         */
        private String head;

        /**
         * 如果为DEL,非必填
         */
        private String value;

        /**
         * 优先级
         */
        private int order;
    }

}
