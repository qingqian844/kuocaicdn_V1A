package com.kuocai.cdn.common.rabbitmq.consumer;

import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.common.mongo.entity.Logs;
import com.kuocai.cdn.config.MyRabbitConfig;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;

//@Component
public class LogsConsumer {

    private final MongoTemplate mongoTemplate;

    LogsConsumer(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @RabbitListener(queues = MyRabbitConfig.LOGS_QUEUE_NAME)
    public void process(Message message, Channel channel) {
        boolean isSuccessful = true;
        Logs logs = JSONObject.parseObject(message.getBody(), Logs.class);
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            isSuccessful = false;
        }
        if (isSuccessful) {
            mongoTemplate.save(logs);
        }
    }
}
