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
public class AddCertDTO implements IRequestDTO {
    private String name;
    private String certificate;
    private String privateKey;
    private String csrId;
    private String comment;

    @Override
    public Map<String, Object> toMap() {
        HashMap<String, Object> map = new HashMap<>(8);
        map.put("name", name);
        map.put("certificate", certificate);
        map.put("privateKey", privateKey);
        map.put("csrId", csrId);
        map.put("comment", comment);
        return map;
    }

    @Override
    public String toJson() {
        return JSON.toJSONString(toMap());
    }
}
