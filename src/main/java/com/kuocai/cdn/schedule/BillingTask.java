package com.kuocai.cdn.schedule;

import com.kuocai.cdn.service.FlowBillingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("prod")
public class BillingTask {

    private final FlowBillingService flowBillingService;

    BillingTask(FlowBillingService flowBillingService) {
        this.flowBillingService = flowBillingService;
    }

    /**
     * 每 10 分钟执行一次的流量计费预处理
     */
    @Scheduled(cron = "0 0/10 * * * ?")
    public void flowBillingPre() {
        log.info("Flow billing pre task start");
        flowBillingService.flowBillingHourPre();
        log.info("Flow billing pre task end");
    }

    /**
     * 一小时一次的流量计费 每个小时的第15分钟执行
     */
    @Scheduled(cron = "0 15 * * * ?")
    public void flowBillingHour() {
        log.info("Flow billing hour task start");
        flowBillingService.flowBillingHour();
        log.info("Flow billing hour task end");
    }

    @Scheduled(cron = "0 0/20 * * * ?")
    public void flowBillingRetry() {
        log.info("Flow billing retry task start");
        flowBillingService.retryPendingBilling();
        log.info("Flow billing retry task end");
    }

    /**
     * 一小时一次的流量计费补单 每个小时的第2分钟执行
     */
    // @Scheduled(cron = "0 15 * * * ?")
    public void flowBillingHourRetry() {
        log.info("Flow billing hour retry task start");
        flowBillingService.flowBillingLastHour();
        log.info("Flow billing hour retry task end");
    }

    /**
     * 半分钟一次的流量计费补单
     */
    // @Scheduled(cron = "0/30 * * * * ?")
    public void flowBillingLastTwoHour() {
        log.info("Flow billing minute retry task start");
        flowBillingService.flowBillingLastTwoHour();
        log.info("Flow billing minute retry task end");
    }
}
