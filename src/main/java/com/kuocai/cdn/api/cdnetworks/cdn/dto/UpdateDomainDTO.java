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
public class UpdateDomainDTO implements IRequestDTO {

    private String version = "1.0.0";

    private String domain = "";

    private String serviceAreas = "";

    private String cnameLabel = "";

    private String comment = "";

    private String cacheHost = "";

    private String headerOfClientip = "X-Forwarded-For";

    private OriginConfig originConfig;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OriginConfig implements IRequestDTO {
        private String originIps = "";

        private OriginConfigAdvOriginConfigs advOriginConfigs = new OriginConfigAdvOriginConfigs();

        private String defaultOriginHostHeader = "";

        private String originPort = "";

        @Override
        public Map<String, Object> toMap() {
            HashMap<String, Object> map = new HashMap<>(6);
            map.put("origin-ips", originIps);
            map.put("advOrigin-configs", advOriginConfigs.toMap());
            map.put("default-origin-host-header", defaultOriginHostHeader);
            map.put("origin-port", originPort);
            return map;
        }

        @Override
        public String toJson() {
            return JSON.toJSONString(toMap());
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OriginConfigAdvOriginConfigs implements IRequestDTO {
        private String detectUrl = "";
        private String detectPeriod = "";
        private List<OriginConfigAdvOriginConfigsAdvOriginConfig> advOriginConfig = new ArrayList<>();

        @Override
        public Map<String, Object> toMap() {
            HashMap<String, Object> map = new HashMap<>(3);
            map.put("detect-url", detectUrl);
            map.put("detect-period", detectPeriod);
            List<Map<String, Object>> advOriginConfigMap = new ArrayList<>();
            for (OriginConfigAdvOriginConfigsAdvOriginConfig config : advOriginConfig) {
                advOriginConfigMap.add(config.toMap());
            }
            map.put("adv-origin-config", advOriginConfigMap);
            return map;
        }

        @Override
        public String toJson() {
            return JSON.toJSONString(toMap());
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OriginConfigAdvOriginConfigsAdvOriginConfig implements IRequestDTO {
        private String masterIps = "";
        private String backupIps = "";

        @Override
        public Map<String, Object> toMap() {
            HashMap<String, Object> map = new HashMap<>(3);
            map.put("master-ips", masterIps);
            map.put("backup-ips", backupIps);
            return map;
        }

        @Override
        public String toJson() {
            return JSON.toJSONString(toMap());
        }
    }

    private LiveConfig liveConfig;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LiveConfig implements IRequestDTO {
        private String originIps = "";
        private String originPushHost = "";

        @Override
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>(3);
            map.put("origin-ips", originIps);
            map.put("origin-push-host", originPushHost);
            return map;
        }

        @Override
        public String toJson() {
            return JSON.toJSONString(toMap());
        }
    }

    private Ssl ssl;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Ssl implements IRequestDTO {
        private String useSsl = "";
        private String useForSni = "";
        private String sslCertificateId = "";

        @Override
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>(4);
            map.put("use-ssl", useSsl);
            map.put("use-for-sni", useForSni);
            map.put("ssl-certificate-id", sslCertificateId);
            return map;
        }

        @Override
        public String toJson() {
            return JSON.toJSONString(toMap());
        }
    }

    private CacheBehavior[] cacheBehaviors;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CacheBehavior implements IRequestDTO {
        private String pathPattern = "";
        private String priority = "";
        private String cacheTtl = "";
        private String ignoreCacheControl = "";

        @Override
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>(6);
            map.put("path-pattern", pathPattern);
            map.put("priority", priority);
            map.put("cache-ttl", cacheTtl);
            map.put("ignore-cache-control", ignoreCacheControl);
            return map;
        }

        @Override
        public String toJson() {
            return JSON.toJSONString(toMap());
        }
    }

    private List<PublishPoint> publishPoints;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PublishPoint implements IRequestDTO {
        private String uri = "";

        @Override
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>(2);
            map.put("uri", uri);
            return map;
        }

        @Override
        public String toJson() {
            return JSON.toJSONString(toMap());
        }
    }

    @Override
    public Map<String, Object> toMap() {
        HashMap<String, Object> map = new HashMap<>(1);
        map.put("version", version);
        map.put("service-areas", serviceAreas);
        map.put("cname-label", cnameLabel);
        map.put("comment", comment);
        map.put("cache-host", cacheHost);
        map.put("header-of-clientip", headerOfClientip);
        map.put("origin-config", null == originConfig ? null : originConfig.toMap());
        map.put("live-config", null == liveConfig ? null : liveConfig.toMap());
        map.put("ssl", null == ssl ? null : ssl.toMap());
        if (null == cacheBehaviors) {
            map.put("cache-behaviors", null);
        } else {
            List<Map<String, Object>> cacheBehaviorsMap = new ArrayList<>();
            for (CacheBehavior behavior : cacheBehaviors) {
                cacheBehaviorsMap.add(behavior.toMap());
            }
            map.put("cache-behaviors", cacheBehaviorsMap);
        }
        if (null == publishPoints) {
            map.put("publish-points", null);
        } else {
            List<Map<String, Object>> publishPointsMap = new ArrayList<>();
            for (PublishPoint point : publishPoints) {
                publishPointsMap.add(point.toMap());
            }
            map.put("publish-points", publishPointsMap);
        }
        return map;
    }

    @Override
    public String toJson() {
        return JSON.toJSONString(toMap());
    }
}
