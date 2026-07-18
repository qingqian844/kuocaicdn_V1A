package com.kuocai.cdn.service;

import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.util.JedisUtil;
import io.minio.MinioClient;
import org.bson.Document;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;

@Service
public class SetupDiagnosticsService {

    private final DataSource dataSource;
    private final MongoTemplate mongoTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final MinioClient minioClient;

    public SetupDiagnosticsService(DataSource dataSource, MongoTemplate mongoTemplate,
                                   RabbitTemplate rabbitTemplate, MinioClient minioClient) {
        this.dataSource = dataSource;
        this.mongoTemplate = mongoTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.minioClient = minioClient;
    }

    public JSONObject diagnose() {
        JSONObject result = new JSONObject(true);
        result.put("mysql", check(() -> {
            try (Connection connection = dataSource.getConnection()) {
                if (!connection.isValid(3)) {
                    throw new IllegalStateException("连接无效");
                }
            }
        }));
        result.put("redis", check(() -> {
            if (!"PONG".equalsIgnoreCase(JedisUtil.ping())) {
                throw new IllegalStateException("未收到 PONG");
            }
        }));
        result.put("mongodb", check(() -> mongoTemplate.getDb().runCommand(new Document("ping", 1))));
        result.put("rabbitmq", check(() -> rabbitTemplate.execute(channel -> {
            if (!channel.isOpen()) {
                throw new IllegalStateException("通道未打开");
            }
            return true;
        })));
        result.put("minio", check(() -> minioClient.listBuckets()));
        return result;
    }

    private JSONObject check(CheckedAction action) {
        JSONObject item = new JSONObject(true);
        try {
            action.run();
            item.put("ok", true);
            item.put("message", "连接正常");
        } catch (Exception e) {
            item.put("ok", false);
            item.put("message", compactMessage(e));
        }
        return item;
    }

    private String compactMessage(Exception e) {
        Throwable current = e;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null ? current.getClass().getSimpleName() : message;
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }
}
