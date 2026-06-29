package com.kuocai.cdn.api.cdnetworks.cdn.dto;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateAntiHotlinkingDTO implements IRequestDTO {
    private String domainName = "";

    private List<VisitControlRule> visitControlRules;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VisitControlRule implements IRequestDTO {
        private String dataId;
        private String pathPattern;
        private String exceptPathPattern;
        private String customPattern = "all";
        private String fileType;
        private String customFileType;
        private String specifyUrlPattern;
        private String directory;
        private String exceptFileType;
        private String exceptCustomFileType;
        private String exceptDirectory;
        private String controlAction = "403";
        private String rewriteTo;
        private String priority = "10";
        private IpControlRule ipControlRule;
        private RefererControlRule refererControlRule;
        private UaControlRule uaControlRule;
        private AdvanceControlRules advanceControlRules;

        @Override
        public Map<String, Object> toMap() {
            HashMap<String, Object> map = new HashMap<>(14);
            map.put("data-id", dataId);
            map.put("path-pattern", pathPattern);
            map.put("except-path-pattern", exceptPathPattern);
            map.put("custom-pattern", customPattern);
            map.put("file-type", fileType);
            map.put("custom-file-type", customFileType);
            map.put("specify-url-pattern", specifyUrlPattern);
            map.put("directory", directory);
            map.put("except-file-type", exceptFileType);
            map.put("except-custom-file-type", exceptCustomFileType);
            map.put("except-directory", exceptDirectory);
            map.put("control-action", controlAction);
            map.put("rewrite-to", rewriteTo);
            map.put("priority", priority);
            map.put("ip-control-rule", IpControlRule.NullOrMap(ipControlRule));
            map.put("referer-control-rule", RefererControlRule.NullOrMap(refererControlRule));
            map.put("ua-control-rule", UaControlRule.NullOrMap(uaControlRule));
            map.put("advance-control-rules", AdvanceControlRules.NullOrMap(advanceControlRules));
            return map;
        }

        @Override
        public String toJson() {
            return JSON.toJSONString(toMap());
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class IpControlRule implements IRequestDTO {
        private String forbiddenIps;
        private String allowedIps;

        public static Map<String, Object> NullOrMap(IpControlRule rule) {
            if (null == rule) {
                return null;
            }
            return rule.toMap();
        }

        @Override
        public Map<String, Object> toMap() {
            HashMap<String, Object> map = new HashMap<>(3);
            map.put("forbidden-ips", forbiddenIps);
            map.put("allowed-ips", allowedIps);
            return map;
        }

        @Override
        public String toJson() {
            return JSON.toJSONString(toMap());
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class RefererControlRule implements IRequestDTO {
        private String allowNullReferer;
        private String validReferer;
        private String validUrl;
        private String validDomain;
        private String invalidReferer;
        private String invalidUrl;
        private String invalidDomain;

        public static Map<String, Object> NullOrMap(RefererControlRule rule) {
            if (null == rule) {
                return null;
            }
            return rule.toMap();
        }

        @Override
        public Map<String, Object> toMap() {
            HashMap<String, Object> map = new HashMap<>(7);
            map.put("allow-null-referer", allowNullReferer);
            map.put("valid-referer", validReferer);
            map.put("valid-url", validUrl);
            map.put("valid-domain", validDomain);
            map.put("invalid-referer", invalidReferer);
            map.put("invalid-url", invalidUrl);
            map.put("invalid-domain", invalidDomain);
            return map;
        }

        @Override
        public String toJson() {
            return JSON.toJSONString(toMap());
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class UaControlRule implements IRequestDTO {
        private String validUserAgents;
        private String invalidUserAgents;

        public static Map<String, Object> NullOrMap(UaControlRule rule) {
            if (null == rule) {
                return null;
            }
            return rule.toMap();
        }

        @Override
        public Map<String, Object> toMap() {
            HashMap<String, Object> map = new HashMap<>(2);
            map.put("valid-user-agents", validUserAgents);
            map.put("invalid-user-agents", invalidUserAgents);
            return map;
        }

        @Override
        public String toJson() {
            return JSON.toJSONString(toMap());
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class AdvanceControlRules implements IRequestDTO {
        private String invalidVisitorRegion;
        private String validVisitorRegion;

        public static Map<String, Object> NullOrMap(AdvanceControlRules rule) {
            if (null == rule) {
                return null;
            }
            return rule.toMap();
        }

        @Override
        public Map<String, Object> toMap() {
            HashMap<String, Object> map = new HashMap<>(2);
            map.put("invalid-visitor-region", invalidVisitorRegion);
            map.put("valid-visitor-region", validVisitorRegion);
            return map;
        }

        @Override
        public String toJson() {
            return JSON.toJSONString(toMap());
        }
    }

    @Override
    public Map<String, Object> toMap() {
        HashMap<String, Object> map = new HashMap<>(2);
        List<Map<String, Object>> rules = new ArrayList<>();
        if (null != visitControlRules) {
            for (VisitControlRule rule : visitControlRules) {
                rules.add(rule.toMap());
            }
        }
        map.put("visit-control-rules", rules);
        return map;
    }

    @Override
    public String toJson() {
        String jsonString = JSON.toJSONString(toMap());
        System.out.println(jsonString);
        return jsonString;
    }
}
