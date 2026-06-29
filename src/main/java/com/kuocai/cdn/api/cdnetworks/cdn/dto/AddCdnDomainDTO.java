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
public class AddCdnDomainDTO implements IRequestDTO {
    private String version = "1.0.0";

    private String contractId = "";

    private String itemId = "";

    private String referencedDomainName = "";

    private String configFormId = "";

    /**
     * 是否纯海外加速
     */
    private boolean accelerateNoChina = false;

    // Cdn-Src-Ip or X-Forwarded-For
    private String headerOfClientip = "X-Forwarded-For";

    private String domainName = "";

    private AddCdnDomainRequestOriginConfig originConfig = null;

    private String comment = "";

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class AddCdnDomainRequestOriginConfig implements IRequestDTO {
        /**
         * 源站域名或 IP
         * <p>
         * PS: 域名只能一个 IP 可以多个 IP 用分号分隔
         */
        private String originIps = "";

        /**
         * 回源 host
         * <p>
         * Origin HOST 用于更改返回源 HTTP 请求标头中的 HOST 字段。
         * 注意：必须是域名或IP格式。对于域名格式，每个段用点分隔，不超过62个字符，总长度不应超过128个字符。
         */
        private String defaultOriginHostHeader = "";

        private String originPort = "80";

        public static AddCdnDomainRequestOriginConfig NullToDefault(AddCdnDomainRequestOriginConfig config) {
            if (null == config) {
                return AddCdnDomainRequestOriginConfig.builder().originIps("").defaultOriginHostHeader("").originPort("80").build();
            }
            return config;
        }

        @Override
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>(4);
            map.put("origin-ips", originIps);
            map.put("default-origin-host-header", defaultOriginHostHeader);
            map.put("origin-port", originPort);
            return map;
        }

        @Override
        public String toJson() {
            return JSON.toJSONString(toMap());
        }
    }

    @Override
    public Map<String, Object> toMap() {
        HashMap<String, Object> map = new HashMap<>(14);
        map.put("version", version);
        map.put("contract-id", contractId);
        map.put("item-id", itemId);
        map.put("referenced-domain-name", referencedDomainName);
        map.put("config-form-id", configFormId);
        map.put("accelerate-no-china", accelerateNoChina);
        map.put("header-of-clientip", headerOfClientip);
        map.put("domain-name", domainName);
        map.put("origin-config", AddCdnDomainRequestOriginConfig.NullToDefault(originConfig).toMap());
        map.put("comment", comment);
        return map;
    }

    @Override
    public String toJson() {
        return JSON.toJSONString(toMap());
    }
}
