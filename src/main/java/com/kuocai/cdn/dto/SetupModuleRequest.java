package com.kuocai.cdn.dto;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class SetupModuleRequest {
    private Boolean enabled;
    private JSONObject config;
}
