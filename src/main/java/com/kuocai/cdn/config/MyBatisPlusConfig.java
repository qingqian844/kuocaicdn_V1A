package com.kuocai.cdn.config;

import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableTransactionManagement
@Configuration
public class MyBatisPlusConfig {
    /**
     * MP插件注册拦截器
     *
     * @return
     */
    @Bean
    public PaginationInnerInterceptor paginationInnerInterceptor() {
        // 这里就是分页插件的配置了，由于由@Configuration注解，所以是自动注入的，自动应用
        return new PaginationInnerInterceptor();
    }
}
