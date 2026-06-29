package com.kuocai.cdn.api.cdnetworks.cdn.dto;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateOriginDTO implements IRequestDTO {
    private String domain = "";

    private String originIps = "";

    private String originHost = "";

    private String originPort = "";

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>(2);
        Map<String, Object> originConfigMap = new HashMap<>(6);
        originConfigMap.put("origin-ips", originIps);
        originConfigMap.put("origin-host", originHost);
        originConfigMap.put("origin-port", originPort);
        map.put("origin-config", originConfigMap);
        return map;
    }

    @Override
    public String toJson() {
        return JSON.toJSONString(toMap());
    }
}
