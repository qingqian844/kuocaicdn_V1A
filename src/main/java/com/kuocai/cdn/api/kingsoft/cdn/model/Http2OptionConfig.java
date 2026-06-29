package com.kuocai.cdn.api.kingsoft.cdn.model;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class Http2OptionConfig {

    @JSONField(name = "Enable")
    private String enable;
} 