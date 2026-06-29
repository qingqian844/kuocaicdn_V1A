package com.kuocai.cdn.schedule;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.CdnDomainService;
import com.kuocai.cdn.service.CdnDomainStatisticsService;
import com.kuocai.cdn.service.FlowBillingService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.JedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 流量接单定时任务
 */
@Slf4j
@Component
@Profile("prod")
public class FlowBillingTask {

    private final FlowBillingService flowBillingService;

    FlowBillingTask(FlowBillingService flowBillingService) {
        this.flowBillingService = flowBillingService;
    }

    @Resource
    private CdnDomainService cdnDomainService;

    @Resource
    private CdnDomainStatisticsService cdnDomainStatisticsService;

    /**
     * 刷新统计数据
     */
    @Scheduled(cron = "0 */30 * * * ?")
    public void refreshStatistics() {
        log.info("刷新统计数据开始...");
        cdnDomainStatisticsService.refreshStatisticsCache();
        log.info("刷新统计数据完成");
    }

    /**
     * 每小时对用户的域名流量进行出单
     *
     * @throws BusinessException
     */
    // @Scheduled(cron = "0 30 * * * ?")
    public void flowBillingLogic() {
        log.info("对域名进行统计出单");
        List<CdnDomain> cdnDomains = cdnDomainService.queryAll();
        if (Assert.isEmpty(cdnDomains)) {
            log.info("系统没有加速域名");
            return;
        }
        Map<Long, List<CdnDomain>> userIdMaps = cdnDomains.stream().collect(Collectors.groupingBy(CdnDomain::getUserId));
        for (Map.Entry<Long, List<CdnDomain>> userIdEntry : userIdMaps.entrySet()) {
            Long userId = userIdEntry.getKey();
            // 查询当前用户的已购买流量包
            List<CdnDomain> userCdnDomains = userIdEntry.getValue();
            String domainNames = userCdnDomains.stream().map(CdnDomain::getDomainName).sorted().collect(Collectors.joining(","));
            // 获取这些域名的流量信息
            DateTime start = getStartHourTime();
            DateTime end = getEndHourTime();
            JSONObject resource;
            try {
                String key = String.format("Statistics:%d:%s:%s:%s->%s", userId, "Resource", domainNames.hashCode(), start.getTime(), end.getTime());
                JedisUtil.delKey(key);
                resource = cdnDomainStatisticsService.mergeAllPlatForm(userCdnDomains, start, end, "Resource", userId);
            } catch (Exception e) {
                log.error("查询用户统计流量信息失败，用户ID:{}, 域名信息:{}，上次时间:{}，当前时间:{}", userId, domainNames, start, getNowHourTime());
                continue;
            }
            Long totalUse = FlowBillingService.extractUsedFlow(resource);
            log.info("[{}，{}]期间，用户[{}]消耗流量[{}]B", start, end, userId, totalUse);
            if (ObjectUtil.equal(totalUse, 0L)) {
                continue;
            }
            try {
                flowBillingService.flowStatement(userCdnDomains, totalUse, userId, start, end);
            } catch (Exception e) {
                log.error("对用户扣款失败，具体错误信息:[{}]", e.getMessage());
            }
        }
    }

    private DateTime getStartHourTime() {
        return DateUtil.offsetHour(getNowHourTime(), -1);
    }

    private DateTime getEndHourTime() {
//        return DateUtil.offsetHour(getNowHourTime(), -1);
        return getNowHourTime();
    }

    private DateTime getNowHourTime() {
        return DateUtil.beginOfHour(new DateTime());
    }
}
