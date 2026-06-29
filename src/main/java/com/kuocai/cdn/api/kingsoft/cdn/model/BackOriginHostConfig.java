package com.kuocai.cdn.api.kingsoft.cdn.model;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class BackOriginHostConfig {

    @JSONField(name = "BackOriginHost")
    private String backOriginHost;
} 