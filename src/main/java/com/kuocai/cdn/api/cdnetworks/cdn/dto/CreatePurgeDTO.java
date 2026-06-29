package com.kuocai.cdn.api.cdnetworks.cdn.dto;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreatePurgeDTO implements IRequestDTO {
    private List<String> urls;
    private List<String> dirs;
    private String urlAction = "delete";
    private String dirAction = "delete";

    @Override
    public Map<String, Object> toMap() {
        HashMap<String, Object> map = new HashMap<>(5);
        map.put("urls", urls);
        map.put("dirs", dirs);
        map.put("url-action", urlAction);
        map.put("dir-action", dirAction);
        return map;
    }

    @Override
    public String toJson() {
        return JSON.toJSONString(toMap());
    }
}
