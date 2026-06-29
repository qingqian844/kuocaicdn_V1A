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
public class UpdateHttp2SettingsDTO implements IRequestDTO {
    private String domainName = "";

    private Http2Settings http2Settings;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Http2Settings implements IRequestDTO {
        private String enableHttp2 = "";
        private String backToOriginProtocol = "";

        public static Http2Settings NullToDefault(Http2Settings settings) {
            if (null == settings) {
                return Http2Settings.builder().enableHttp2("false").backToOriginProtocol("http1.1").build();
            }
            return settings;
        }

        public static Http2Settings EnableHttp2() {
            return Http2Settings.builder().enableHttp2("true").backToOriginProtocol("http1.1").build();
        }

        public static Http2Settings DisableHttp2() {
            return Http2Settings.builder().enableHttp2("false").backToOriginProtocol("http1.1").build();
        }

        @Override
        public Map<String, Object> toMap() {
            HashMap<String, Object> map = new HashMap<>(6);
            map.put("enableHttp2", enableHttp2);
            map.put("backToOriginProtocol", backToOriginProtocol);
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
        map.put("http2Settings", Http2Settings.NullToDefault(http2Settings).toMap());
        return map;
    }

    @Override
    public String toJson() {
        return JSON.toJSONString(toMap());
    }
}
