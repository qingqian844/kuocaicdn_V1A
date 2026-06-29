package com.kuocai.cdn.api.qiniu.cdn.vo;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RespHeaderVo {

    private List<ResponseHeaderControl> responseHeaderControls;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public class ResponseHeaderControl{

        /**
         * 对响应头的进行操作的类型，可选"set"、“del”，目前不支持"add"
         */
        private String op;

        /**
         * 匹配响应头的key，可选值：
         * Content-Type,Cache-Control,Content-Disposition,
         * Content-Language,Expires,Access-Control-Allow-Origin,
         * Access-Control-Allow-Methods,Access-Control-Allow-Headers,
         * Access-Control-Max-Age,Access-Control-Expose-Headers,Access-Control-Allow-Credentials。
         */
        private String key;

        /**
         * 响应头的value，在op为"set"时有效
         */
        private String value;
    }
}
