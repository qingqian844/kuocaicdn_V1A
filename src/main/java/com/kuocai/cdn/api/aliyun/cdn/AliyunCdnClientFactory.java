package com.kuocai.cdn.api.aliyun.cdn;

import com.aliyun.cdn20180510.Client;
import com.aliyun.teaopenapi.models.Config;
import com.kuocai.cdn.api.aliyun.cdn.properties.AliyunCdn;
import com.kuocai.cdn.util.Assert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Slf4j
@Configuration
public class AliyunCdnClientFactory {

    private static volatile Client client;
    private static volatile String clientAccessKeyId;
    private static volatile String clientAccessKeySecret;

    @Bean(name = "aliyunCdnClient")
    @DependsOn("preloadComponent")
    public static Client initClient() throws Exception {
        log.info("==========> init aliyun cdn client <==========");
        return getClient();
    }

    public static Client getClient() throws Exception {
        if (client == null || !isCurrentCredentialClient()) {
            synchronized (AliyunCdnClientFactory.class) {
                if (client == null || !isCurrentCredentialClient()) {
                    client = createClient();
                }
            }
        }
        return client;
    }

    public static void applyConfiguration(String accessKeyId, String accessKeySecret) {
        AliyunCdn.AccessKeyId = normalize(accessKeyId);
        AliyunCdn.AccessKeySecret = normalize(accessKeySecret);
        resetClient();
    }

    public static void resetClient() {
        synchronized (AliyunCdnClientFactory.class) {
            client = null;
            clientAccessKeyId = null;
            clientAccessKeySecret = null;
        }
    }

    public static Client createClient() throws Exception {
        if (Assert.isEmpty(AliyunCdn.AccessKeyId) || Assert.isEmpty(AliyunCdn.AccessKeySecret)) {
            log.error("==========> init aliyun cdn client failed: AccessKeyId or AccessKeySecret is empty <==========");
            return null;
        }
        Config config = new Config();
        config.setType("access_key");
        config.setAccessKeyId(AliyunCdn.AccessKeyId);
        config.setAccessKeySecret(AliyunCdn.AccessKeySecret);
        config.setEndpoint(AliyunCdn.Endpoint);
        config.setProtocol("HTTP");
        Client createdClient = new Client(config);
        clientAccessKeyId = AliyunCdn.AccessKeyId;
        clientAccessKeySecret = AliyunCdn.AccessKeySecret;
        return createdClient;
    }

    private static boolean isCurrentCredentialClient() {
        return normalize(AliyunCdn.AccessKeyId).equals(clientAccessKeyId)
                && normalize(AliyunCdn.AccessKeySecret).equals(clientAccessKeySecret);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
