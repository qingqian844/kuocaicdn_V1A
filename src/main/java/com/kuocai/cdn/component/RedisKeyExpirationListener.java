package com.kuocai.cdn.component;

import com.kuocai.cdn.constant.KuoCaiConstants;
import com.kuocai.cdn.service.TransactionOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * description: 编写一个类交由spring管理并继承KeyExpirationEventMessageListener
 *
 * @author bo
 * @version 1.0
 * @date 2023/4/9 19:36
 */
@Component
@Slf4j
public class RedisKeyExpirationListener extends KeyExpirationEventMessageListener {

    @Resource
    private TransactionOrderService transactionOrderService;
public RedisKeyExpirationListener(RedisMessageListenerContainer listenerContainer) {
        super(listenerContainer);
    }

    /**
     * description: 选定为db5
     *
     * @author bo
     * @version 1.0
     * @date 2023/4/9 20:58
     */
    @Override
    protected void doRegister(RedisMessageListenerContainer listenerContainer) {
        listenerContainer.addMessageListener(this, new PatternTopic("__keyevent@0__:expired"));
    }

    /**
     * description: 监听过期key
     *
     * @author bo
     * @version 1.0
     * @date 2023/4/9 20:57
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();
        // 判断是否是想要监听的过期订单key
        if (expiredKey.startsWith(KuoCaiConstants.ORDER_REDIS_PREFIX)) {
            // 根据过期redisKey获取订单号
            String transactionOrderId = expiredKey.substring(KuoCaiConstants.ORDER_REDIS_PREFIX.length());
            transactionOrderService.updateTransactionOrderStatusByRedis(Long.valueOf(transactionOrderId));
        }

        // 判断是否想要监听的过期流量包key
    }
}


