package com.kuocai.cdn.api.cdnetworks.cdn.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryCompressionDTO implements IRequestDTO {

    private String domainName = "";

    @Override
    public Map<String, Object> toMap() {
        HashMap<String, Object> map = new HashMap<>(2);
        map.put("domain-name", domainName);
        return map;
    }

    @Override
    public String toJson() {
        return "";
    }
}
