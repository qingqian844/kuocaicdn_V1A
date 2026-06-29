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
public class DeleteDomainDTO implements IRequestDTO {

    /**
     * 域名
     */
    String domainName = "";

    /**
     * 域名ID
     */
    String domainId = "";

    @Override
    public Map<String, Object> toMap() {
        HashMap<String, Object> map = new HashMap<>(3);
        map.put("domain-name", domainName);
        map.put("domain-id", domainId);
        return map;
    }

    @Override
    public String toJson() {
        // 实际不需要
        return "";
    }
}
