package com.kuocai.cdn.config;

import io.minio.MinioClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author xiaobo
 * @date 2022/8/5
 */
@Data
@Configuration
@Slf4j
public class MinIoClientConfig {
    @Value("${minio.endpoint}")
    private String endpoint;
    @Value("${minio.accessKey}")
    private String accessKey;
    @Value("${minio.secretKey}")
    private String secretKey;

    /**
     * 注入minio 客户端
     *
     * @return MinioClient
     */
    @Bean
    public MinioClient minioClient() {

        try {
            return MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
        } catch (Exception e) {
            e.printStackTrace();
            log.info("-----创建Minio客户端失败-----");
            return null;
        }
    }

}

