package com.kuocai.cdn.api.kingsoft.cdn.model;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class IpProtectionConfig {

    @JSONField(name = "Enable")
    private String enable;

    @JSONField(name = "IpType")
    private String ipType;

    @JSONField(name = "IpList")
    private String ipList;
} 