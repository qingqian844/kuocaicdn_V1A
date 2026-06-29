package com.kuocai.cdn.common.rabbitmq.consumer;

import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.common.mongo.entity.Access;
import com.kuocai.cdn.config.MyRabbitConfig;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;

//@Component
public class AccessConsumer {

    private final MongoTemplate mongoTemplate;

    AccessConsumer(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @RabbitListener(queues = MyRabbitConfig.ACCESS_QUEUE_NAME)
    public void process(Message message, Channel channel) {
        boolean isSuccessful = true;
        Access access = JSONObject.parseObject(message.getBody(), Access.class);
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            isSuccessful = false;
        }
        if (isSuccessful) {
            mongoTemplate.save(access);
        }
    }
}
