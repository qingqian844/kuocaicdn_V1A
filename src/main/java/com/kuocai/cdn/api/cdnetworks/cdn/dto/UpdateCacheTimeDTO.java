package com.kuocai.cdn.api.cdnetworks.cdn.dto;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCacheTimeDTO implements IRequestDTO {
    private String domainName = "";

    private List<CacheTimeBehavior> cacheTimeBehaviors;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CacheTimeBehavior implements IRequestDTO {
        private String dataId = "";
        private String pathPattern = "";
        private String exceptPathPattern = "";
        private String customPattern = "";
        private String fileType = "";
        private String customFileType = "";
        private String specifyUrlPattern = "";
        private String directory = "";
        private String cacheTtl = "";
        private String ignoreCacheControl = "";
        private String isRespectServer = "";
        private String ignoreLetterCase = "";
        private String reloadManage = "";
        private String priority = "";
        private String ignoreAuthenticationHeader = "false";

        @Override
        public Map<String, Object> toMap() {
            HashMap<String, Object> map = new HashMap<>(18);
            map.put("data-id", dataId);
            map.put("path-pattern", pathPattern);
            map.put("except-path-pattern", exceptPathPattern);
            map.put("custom-pattern", customPattern);
            map.put("file-type", fileType);
            map.put("custom-file-type", customFileType);
            map.put("specify-url-pattern", specifyUrlPattern);
            map.put("directory", directory);
            map.put("cache-ttl", cacheTtl);
            map.put("ignore-cache-control", ignoreCacheControl);
            map.put("is-respect-server", isRespectServer);
            map.put("ignore-letter-case", ignoreLetterCase);
            map.put("reload-manage", reloadManage);
            map.put("priority", priority);
            map.put("ignore-authentication-header", ignoreAuthenticationHeader);
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
        List<Map<String, Object>> cacheTimeBehaviors = this.cacheTimeBehaviors.stream().map(CacheTimeBehavior::toMap).collect(toList());
        map.put("cache-time-behaviors", cacheTimeBehaviors);
        return map;
    }

    @Override
    public String toJson() {
        return JSON.toJSONString(toMap());
    }
}
