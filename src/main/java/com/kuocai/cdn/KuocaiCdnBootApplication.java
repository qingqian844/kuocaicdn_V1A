package com.kuocai.cdn;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 括彩云CND程序启动类
 *
 * @author XUEW
 * @date 下午9:04 2023/2/12
 */
@EnableAsync
@SpringBootApplication
@EnableScheduling
@MapperScan("com.kuocai.cdn.dao")
public class KuocaiCdnBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(KuocaiCdnBootApplication.class, args);
    }
}
