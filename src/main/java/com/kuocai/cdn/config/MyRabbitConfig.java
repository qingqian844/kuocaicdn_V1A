package com.kuocai.cdn.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyRabbitConfig {
    public static final String EXCHANGE_NAME = "boot.direct";

    public static final String LOGS_QUEUE_NAME = "direct.logs";
    public static final String ACCESS_QUEUE_NAME = "direct.access";
    public static final String ALIYUN_CDN_CONFIG_QUEUE_NAME = "direct.aliyun.cdn.config";

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue logsQueue() {
        return new Queue(LOGS_QUEUE_NAME, true, false, false);
    }

    @Bean
    public Queue accessQueue() {
        return new Queue(ACCESS_QUEUE_NAME, true, false, false);
    }

//    @Bean
    public Queue aliyunCdnConfigQueue() {
        return new Queue(ALIYUN_CDN_CONFIG_QUEUE_NAME, true, false, false);
    }

    @Bean
    public Binding logsQueueBinding(Queue logsQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(logsQueue).to(directExchange).with(LOGS_QUEUE_NAME);
    }

    @Bean
    public Binding accessQueueBinding(Queue accessQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(accessQueue).to(directExchange).with(ACCESS_QUEUE_NAME);
    }

//    @Bean
    public Binding aliyunCdnConfigQueueBinding(Queue aliyunCdnConfigQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(aliyunCdnConfigQueue).to(directExchange).with(ALIYUN_CDN_CONFIG_QUEUE_NAME);
    }
}
