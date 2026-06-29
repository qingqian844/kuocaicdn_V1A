package com.kuocai.cdn.api.cdnetworks.cdn.dto;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateHttpHeaderDTO implements IRequestDTO {
    private String domainName = "";

    private List<HeaderModifyRule> headerModifyRules;

    private String priority = "10";
    private String exceptFileType;
    private String exceptDirectory;
    private String exceptRequestMethod;
    private String exceptRequestHeader;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HeaderModifyRule implements IRequestDTO {
        private String dataId;
        private String pathPattern;
        private String exceptPathPattern;
        private String customPattern;
        private String fileType;
        private String customFileType;
        private String directory;
        private String specifyUrl;
        private String requestMethod;
        private String headerDirection;
        private String action;
        private String allowRegexp;
        private String headerName;
        private String headerValue;
        private String requestHeader;

        public static Map<String, Object> NullOrMap(HeaderModifyRule rule) {
            if (null == rule) {
                return null;
            }
            return rule.toMap();
        }

        @Override
        public Map<String, Object> toMap() {
            HashMap<String, Object> map = new HashMap<>(16);
            map.put("data-id", dataId);
            map.put("path-pattern", pathPattern);
            map.put("except-path-pattern", exceptPathPattern);
            map.put("custom-pattern", customPattern);
            map.put("file-type", fileType);
            map.put("custom-file-type", customFileType);
            map.put("directory", directory);
            map.put("specify-url", specifyUrl);
            map.put("request-method", requestMethod);
            map.put("header-direction", headerDirection);
            map.put("action", action);
            map.put("allow-regexp", allowRegexp);
            map.put("header-name", headerName);
            map.put("header-value", headerValue);
            map.put("request-header", requestHeader);
            return map;
        }

        @Override
        public String toJson() {
            return JSON.toJSONString(toMap());
        }
    }

    @Override
    public Map<String, Object> toMap() {
        HashMap<String, Object> map = new HashMap<>(6);
        List<Map<String, Object>> rules = new ArrayList<>();
        if (null != headerModifyRules) {
            for (HeaderModifyRule rule : headerModifyRules) {
                rules.add(HeaderModifyRule.NullOrMap(rule));
            }
        }
        map.put("header-modify-rules", rules);
        map.put("priority", priority);
        map.put("except-file-type", exceptFileType);
        map.put("except-directory", exceptDirectory);
        map.put("except-request-method", exceptRequestMethod);
        map.put("except-request-header", exceptRequestHeader);
        return map;
    }

    @Override
    public String toJson() {
        String jsonString = JSON.toJSONString(toMap());
        System.out.println(jsonString);
        return jsonString;
    }
}
