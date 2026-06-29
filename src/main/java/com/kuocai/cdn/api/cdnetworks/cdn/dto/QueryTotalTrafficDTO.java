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
public class QueryTotalTrafficDTO implements IRequestDTO {
    // Params
    private String datefrom;
    private String dateto;
    // hourly or daily
    private String type = "hourly";

    // Body
    private DomainList domainList;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DomainList implements IRequestDTO {
        private List<String> domainName;

        public static Map<String, Object> NullOrMap(DomainList domainList) {
            if (domainList == null) {
                return null;
            }
            return domainList.toMap();
        }

        @Override
        public Map<String, Object> toMap() {
            HashMap<String, Object> map = new HashMap<>(2);
            map.put("domain-name", domainName);
            return map;
        }

        @Override
        public String toJson() {
            return JSON.toJSONString(toMap());
        }
    }

    @Override
    public Map<String, Object> toMap() {
        HashMap<String, Object> map = new HashMap<>(2);
        map.put("domain-list", DomainList.NullOrMap(domainList));
        return map;
    }

    @Override
    public String toJson() {
        return JSON.toJSONString(toMap());
    }
}
