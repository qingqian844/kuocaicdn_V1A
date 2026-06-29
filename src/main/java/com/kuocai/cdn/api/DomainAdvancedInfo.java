package com.kuocai.cdn.api;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DomainAdvancedInfo {

    private List<HttpResponseHeader> http_response_header;

    private List<ErrorCodeRedirectRules> error_code_redirect_rules;

    private List<ErrorPage> error_pages;

    private Compress compress;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HttpResponseHeader {
        /**
         * 设置HTTP响应头参数。
         * 取值："Content-Disposition", "Content-Language", "Access-Control-Allow-Origin","Access-Control-Allow-Methods", "Access-Control-Max-Age", "Access-Control-Expose-Headers"或自定义头部。
         * 格式要求：长度1~100，以字母开头，可以使用字母、数字和短横杠。
         */
        private String name;

        /**
         * 设置HTTP响应头参数的值。自定义HTTP响应头参数长度范围1~256，支持字母、数字和特定字符（.-_*#!&+|^~'"/:;,=@?<>）。
         */
        private String value;

        /**
         * 设置http响应头操作类型，取值“set/delete”。set代表设置，delete代表删除。
         */
        private String action;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ErrorCodeRedirectRules {

        /**
         * 重定向的错误码，当前支持以下状态码
         * 4xx:400, 403, 404, 405, 414, 416, 451 5xx:500, 501, 502, 503, 504
         */
        private Integer error_code;

        /**
         * 重定向状态码，取值为301或302。
         */
        private String target_code;

        /**
         * 重定向的目标链接
         */
        private String target_link;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ErrorPage {
        /**
         * 错误HTTP状态码
         * 支持状态码: 400,403,404,405,406,414,416,500,501,502,503,504
         */
        private String errorHttpCode;
        
        /**
         * 自定义错误页面URL
         * 必须以 https:// 或 http:// 开头
         */
        private String customPageUrl;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Compress {

        /**
         * 智能压缩开关（on：开启 off：关闭）。
         */
        private String status;

        /**
         * 智能压缩类型（gzip：gzip压缩，br：br压缩）。
         */
        private String type;

        /**
         * 压缩格式，内容总长度不可超过200个字符， 使用","分隔，每组内容不可超过50个字符， 开启状态下，首次传空时默认值为.js,.html,.css,.xml,.json,.shtml,.htm，否则为上次设置的结果。
         */
        private String file_type;
    }
}
