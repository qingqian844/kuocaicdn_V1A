package com.kuocai.cdn.api.kingsoft.cdn.model;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class ReferProtectionConfig {

    @JSONField(name = "Enable")
    private String enable;

    @JSONField(name = "ReferType")
    private String referType;

    @JSONField(name = "ReferList")
    private String referList;

    @JSONField(name = "AllowEmpty")
    private String allowEmpty;
} 