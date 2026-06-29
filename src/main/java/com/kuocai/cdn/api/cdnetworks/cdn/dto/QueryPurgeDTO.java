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
public class QueryPurgeDTO implements IRequestDTO {
    private String startTime;
    private String endTime;
    private String itemId;
    private String url;
    private String status;
    private String pageNo;
    private String pageSize;

    @Override
    public Map<String, Object> toMap() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("startTime", startTime);
        map.put("endTime", endTime);
        map.put("itemId", itemId);
        map.put("url", url);
        map.put("status", status);
        map.put("pageNo", pageNo);
        map.put("pageSize", pageSize);
        return map;
    }

    @Override
    public String toJson() {
        return JSON.toJSONString(toMap());
    }
}
