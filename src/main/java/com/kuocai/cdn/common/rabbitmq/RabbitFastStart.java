package com.kuocai.cdn.common.rabbitmq;

import com.rabbitmq.client.ConnectionFactory;

public class RabbitFastStart {
    public static ConnectionFactory getConnectionFactory(String vhost) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("rabbitmq");
        factory.setPort(5672);
        factory.setUsername("admin");
        factory.setPassword("FDn5pGUPBHUtVq672");
        factory.setVirtualHost(vhost);
        factory.setAutomaticRecoveryEnabled(true);
        return factory;
    }
}
