package com.kuocai.cdn.api.kingsoft.cdn.model;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class HttpHeadersConfig {

    @JSONField(name = "HttpHeaderRules")
    private List<HttpHeaderRule> httpHeaderRules;

    @Data
    @ToString
    public static class HttpHeaderRule {


        @JSONField(name = "HeaderKey")
        private String headerKey;

        @JSONField(name = "HeaderValue")
        private String headerValue;

        public String getHeader() {
            return this.headerKey;
        }

        public String getValue() {
            return this.headerValue;
        }

        public String getAction() {
            return "set";
        }
    }
}