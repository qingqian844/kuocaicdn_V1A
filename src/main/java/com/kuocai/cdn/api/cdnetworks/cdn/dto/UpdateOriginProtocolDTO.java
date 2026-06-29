package com.kuocai.cdn.api.cdnetworks.cdn.dto;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateOriginProtocolDTO implements IRequestDTO {
    private String domain = "";

    private String protocol = "";

    private String port = "";

    public static UpdateOriginProtocolDTO Follow() {
        return UpdateOriginProtocolDTO.builder().protocol(null).port(null).build();
    }

    public static UpdateOriginProtocolDTO Https() {
        return UpdateOriginProtocolDTO.builder().protocol("https").port("443").build();
    }

    public static UpdateOriginProtocolDTO Http() {
        return UpdateOriginProtocolDTO.builder().protocol("http").port("80").build();
    }

    @Override
    public Map<String, Object> toMap() {
        HashMap<String, Object> map = new HashMap<>(2);
        HashMap<String, Object> dataMap = new HashMap<>(5);
        dataMap.put("protocol", protocol);
        dataMap.put("port", port);
        map.put("backToOriginRewriteRule", dataMap);
        return map;
    }

    @Override
    public String toJson() {
        return JSON.toJSONString(toMap());
    }
}
