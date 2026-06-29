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
public class UpdateDomainCertDTO implements IRequestDTO {
    private String domain = "";

    private String certificateId = "";

    private String tlsVersion;

    private String enableOcsp;

    private String cipherSuites;

    @Override
    public Map<String, Object> toMap() {
        HashMap<String, Object> map = new HashMap<>(8);
//        map.put("domain", domain);
        map.put("certificateId", certificateId);
        map.put("TLSVersion", tlsVersion);
        map.put("enableOCSP", enableOcsp);
        map.put("cipherSuites", cipherSuites);
        return map;
    }

    @Override
    public String toJson() {
        return JSON.toJSONString(toMap());
    }
}
