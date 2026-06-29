package com.kuocai.cdn.api.cdnetworks.cdn.dto;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreatePrefetchDTO implements IRequestDTO {
    private String name;

    private List<FileList> fileList;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FileList implements IRequestDTO {
        private String url;

        // headers are not used (name - value)

        @Override
        public Map<String, Object> toMap() {
            HashMap<String, Object> map = new HashMap<>(1);
            map.put("url", url);
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
        map.put("name", name);
        List<Map<String, Object>> fileListMap = new ArrayList<>();
        for (FileList fileListElement : fileList) {
            fileListMap.add(fileListElement.toMap());
        }
        map.put("fileList", fileListMap);
        return map;
    }

    @Override
    public String toJson() {
        return JSON.toJSONString(toMap());
    }
}
