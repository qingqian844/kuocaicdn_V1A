package com.kuocai.cdn.api.cdnetworks.cdn.dto;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateInnerRedirectDTO implements IRequestDTO {
    private String domainName = "";

    private List<RewriteRuleSetting> rewriteRuleSettings;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class RewriteRuleSetting implements IRequestDTO {
        private String dataId;
        private String pathPattern;
        private String customPattern;
        private String directory;
        private String fileType;
        private String customFileType;
        private String exceptPathPattern;
        private String ignoreLetterCase;
        private String publishType;
        private String priority;
        private String beforeValue;
        private String afterValue;
        private String rewriteType;
        private String requestHeader;
        private String exceptionRequestHeader;

        @Override
        public Map<String, Object> toMap() {
            HashMap<String, Object> map = new HashMap<>();
            map.put("data-id", dataId);
            map.put("path-pattern", pathPattern);
            map.put("custom-pattern", customPattern);
            map.put("directory", directory);
            map.put("file-type", fileType);
            map.put("custom-file-type", customFileType);
            map.put("except-path-pattern", exceptPathPattern);
            map.put("ignore-letter-case", ignoreLetterCase);
            map.put("publish-type", publishType);
            map.put("priority", priority);
            map.put("before-value", beforeValue);
            map.put("after-value", afterValue);
            map.put("rewrite-type", rewriteType);
            map.put("request-header", requestHeader);
            map.put("exception-request-header", exceptionRequestHeader);
            return map;
        }

        @Override
        public String toJson() {
            return JSON.toJSONString(toMap());
        }
    }

    public static RewriteRuleSetting createRewriteToHttpsRule(String code) {
        return RewriteRuleSetting.builder()
                .pathPattern(".*").ignoreLetterCase("true").publishType("Cache").beforeValue("^http://([^/]+/.*)").afterValue(String.format("%s:https://$1", code)).rewriteType("before").priority("10")
                .build();
    }

    public static RewriteRuleSetting createRewriteToHttpRule(String code) {
        return RewriteRuleSetting.builder()
                .pathPattern(".*").ignoreLetterCase("true").publishType("Cache").beforeValue("^https://([^/]+/.*)").afterValue(String.format("%s:http://$1", code)).rewriteType("before").priority("10")
                .build();
    }

    @Override
    public Map<String, Object> toMap() {
        HashMap<String, Object> map = new HashMap<>();
        List<Map<String, Object>> rewriteRuleSettings = this.rewriteRuleSettings.stream().map(RewriteRuleSetting::toMap).collect(toList());
        map.put("rewrite-rule-settings", rewriteRuleSettings);
        return map;
    }

    @Override
    public String toJson() {
        return JSON.toJSONString(toMap());
    }
}
