package com.kuocai.cdn.api.kingsoft.cdn.model;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class CacheRuleConfig {

    @JSONField(name = "CacheRules")
    private List<CacheRule> cacheRules;

    @Data
    @ToString
    public static class CacheRule {
        @JSONField(name = "CacheRuleType")
        private String cacheRuleType;

        @JSONField(name = "Value")
        private String value;

        @JSONField(name = "CacheTime")
        private Integer cacheTime;

        @JSONField(name = "RespectOrigin")
        private String respectOrigin;

        @JSONField(name = "CacheEnable")
        private String cacheEnable;
    }
} 