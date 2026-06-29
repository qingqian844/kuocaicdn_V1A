package com.kuocai.cdn.api.baidu.cdn;

import com.baidubce.BceClientConfiguration;
import com.baidubce.auth.DefaultBceCredentials;
import com.baidubce.services.cdn.CdnClient;
import com.kuocai.cdn.api.baidu.cdn.properties.BaiduCdn;
import com.kuocai.cdn.util.Assert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Slf4j
@Configuration
public class BaiduCdnClientFactory {

    private static volatile CdnClient client;

    @Bean(name = "baiduCdnClient")
    @DependsOn("preloadComponent")
    public static CdnClient initClient() {
        log.info("==========> 初始化百度CDN客户端 <==========");
        if (client == null) {
            synchronized (BaiduCdnClientFactory.class) {
                if (client == null) {
                    client = createClient();
                }
            }
        }
        return client;
    }

    public static CdnClient createClient() {
        if (Assert.isEmpty(BaiduCdn.AccessKeyId) || Assert.isEmpty(BaiduCdn.SecretAccessKy)) {
            log.error("==========> 百度CDN客户端初始化失败，AccessKeyId 或 SecretAccessKy 为空 <==========");
            return null;
        }
        BceClientConfiguration config = new BceClientConfiguration()
                .withCredentials(new DefaultBceCredentials(BaiduCdn.AccessKeyId, BaiduCdn.SecretAccessKy))
                .withEndpoint(BaiduCdn.EndPoint);
        return new CdnClient(config);
    }
}
