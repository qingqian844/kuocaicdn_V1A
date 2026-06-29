package com.kuocai.cdn.config;


import com.kuocai.cdn.util.Assert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @author xiaobo
 * @date 2022/7/25
 */

@Slf4j
@Configuration
public class JedisConfig {

    @Value("${redis.host}")
    private String host;

    @Value("${redis.port}")
    private int port;

    @Value("${redis.password}")
    private String password;

    @Value("${redis.timeout}")
    private int timeout;

    @Value("${redis.pool.max-total}")
    private int maxTotal;

    @Value("${redis.pool.max-wait}")
    private long maxWait;

    @Value("${redis.pool.max-idle}")
    private int maxIdle;

    @Value("${redis.pool.db}")
    private int db;

    @Bean
    public JedisPool jedisPool() {
        try {
            JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
            jedisPoolConfig.setMaxTotal(maxTotal);
            jedisPoolConfig.setMaxIdle(maxIdle);
            jedisPoolConfig.setMaxWaitMillis(maxWait);
            jedisPoolConfig.setTestOnBorrow(true);       // 检查连接可用性, 确保获取的redis实例可用
            String pwd = Assert.isEmpty(password) ? null : password;
            JedisPool jedisPool = new JedisPool(jedisPoolConfig, host, port, timeout, pwd, db);
            log.info("初始化Redis连接池JedisPool成功!地址: " + host + ":" + port);
            return jedisPool;
        } catch (Exception e) {
            log.error("初始化Redis连接池JedisPool异常:" + e.getMessage());
        }
        return null;
    }

    /**
     * description: 创建jedis连接工厂
     *
     * @author bo
     * @version 1.0
     * @date 2023/4/9 19:33
     */
    @Bean
    public JedisConnectionFactory jedisConnectionFactory() {
        RedisStandaloneConfiguration redisStandaloneConfiguration =
                new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setHostName(host);
        redisStandaloneConfiguration.setDatabase(db);
        redisStandaloneConfiguration.setPassword(RedisPassword.of(password));
        redisStandaloneConfiguration.setPort(port);
        return new JedisConnectionFactory(redisStandaloneConfiguration);
    }

    /**
     * description: 创建redis监听容器
     *
     * @author bo
     * @version 1.0
     * @date 2023/4/9 19:33
     */
    @Bean
    public RedisMessageListenerContainer container(JedisConnectionFactory jedisConnectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(jedisConnectionFactory);
        return container;
    }


}
