package com.kuocai.cdn;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

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
public class KuocaiCdnBootApplication implements SchedulingConfigurer {

    @Value("${spring.task.scheduling.pool.size:8}")
    private int schedulingPoolSize;

    public static void main(String[] args) {
        SpringApplication.run(KuocaiCdnBootApplication.class, args);
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setTaskScheduler(taskScheduler());
    }

    @Bean(name = "taskScheduler", destroyMethod = "shutdown")
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(schedulingPoolSize);
        taskScheduler.setThreadNamePrefix("scheduling-");
        taskScheduler.setWaitForTasksToCompleteOnShutdown(true);
        taskScheduler.setAwaitTerminationSeconds(30);
        taskScheduler.initialize();
        return taskScheduler;
    }
}
