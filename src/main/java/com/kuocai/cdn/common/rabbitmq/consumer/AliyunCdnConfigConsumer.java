package com.kuocai.cdn.common.rabbitmq.consumer;

import com.kuocai.cdn.common.mongo.entity.AliyunSetCdnDomainConfig;
import com.kuocai.cdn.config.MyRabbitConfig;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.domain.operation.AliyunDomainServiceImpl;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
//@Component
public class AliyunCdnConfigConsumer {
    private final MongoTemplate mongoTemplate;

    private final AliyunDomainServiceImpl aliyunDomainService;

    AliyunCdnConfigConsumer(MongoTemplate mongoTemplate, AliyunDomainServiceImpl aliyunDomainService) {
        this.aliyunDomainService = aliyunDomainService;
        this.mongoTemplate = mongoTemplate;
    }

    @RabbitListener(queues = MyRabbitConfig.ALIYUN_CDN_CONFIG_QUEUE_NAME)
    public void process(Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String id = new String(message.getBody());
        AliyunSetCdnDomainConfig config = mongoTemplate.findById(id, AliyunSetCdnDomainConfig.class);
        if (config != null) {
            try {
                aliyunDomainService.updateBatchCdnDomainConfig(config);
                config.setPromise("resolved");
            } catch (BusinessException e) {
                config.setPromise("rejected");
//                try {
//                    channel.basicNack(deliveryTag, false, true);
//                } catch (IOException ex) {
//                    log.error("消息重新排队失败: {}", ex.getMessage());
//                }
                log.error("批量更新CDN域名配置失败: {}", e.getMessage());
            }
            mongoTemplate.save(config);
            log.info("批量更新CDN域名配置成功");
        } else {
            log.error("未找到对应的CDN域名配置 {}, 请检查", id);
        }
        try {
            channel.basicAck(deliveryTag, false);
        } catch (IOException e) {
            log.error("消息确认失败: {}", e.getMessage());
        }
    }
}
