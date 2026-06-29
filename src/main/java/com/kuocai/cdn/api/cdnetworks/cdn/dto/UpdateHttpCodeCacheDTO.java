package com.kuocai.cdn.api.cdnetworks.cdn.dto;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateHttpCodeCacheDTO implements IRequestDTO {
    private String domainName = "";

    private List<HttpCodeCacheRule> httpCodeCacheRules;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HttpCodeCacheRule implements IRequestDTO {
        private String[] httpCodes;
        private String cacheTtl;
        private String dataId;

        @Override
        public Map<String, Object> toMap() {
            HashMap<String, Object> map = new HashMap<>(5);
            List<String> httpCodes = Arrays.asList(this.httpCodes);
            map.put("http-codes", httpCodes);
            map.put("cache-ttl", cacheTtl);
            map.put("data-id", dataId);
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
        List<Map<String, Object>> httpCodeCacheRules = this.httpCodeCacheRules.stream().map(HttpCodeCacheRule::toMap).collect(toList());
        map.put("http-code-cache-rules", httpCodeCacheRules);
        return map;
    }

    @Override
    public String toJson() {
        return JSON.toJSONString(toMap());
    }
}
