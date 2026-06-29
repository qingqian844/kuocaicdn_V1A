package com.kuocai.cdn.schedule;

import com.kuocai.cdn.util.JedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;


/**
 * 清理服务繁忙
 */
@Slf4j
@Component
@Profile("prod")
public class ClearServerBusyTask {

    /**
     * 清理服务繁忙
     */
    @Scheduled(cron = "0 0,30 * * * ?")
    public void updateDomainInfoStatus() {
        Set<String> keys = JedisUtil.keys("ServiceBusy:*");
        JedisUtil.delKeys(keys);
        log.info("清除服务器繁忙标识：[{}]", keys);
    }
}
