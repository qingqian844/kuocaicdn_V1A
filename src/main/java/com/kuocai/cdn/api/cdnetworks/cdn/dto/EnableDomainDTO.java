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
public class EnableDomainDTO implements IRequestDTO {

    /**
     * 域名 ID
     */
    private String domainId = "";

    @Override
    public Map<String, Object> toMap() {
        HashMap<String, Object> map = new HashMap<>(2);
        map.put("domain-id", domainId);
        return map;
    }

    @Override
    public String toJson() {
        return JSON.toJSONString(toMap());
    }
}
