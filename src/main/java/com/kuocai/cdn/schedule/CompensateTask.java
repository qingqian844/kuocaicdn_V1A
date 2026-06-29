package com.kuocai.cdn.schedule;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.service.CdnDomainService;
import com.kuocai.cdn.service.FlowBillingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Component
@Profile("prod")
public class CompensateTask {
    @Autowired
    private FlowBillingService flowBillingService;

    @Resource
    private CdnDomainService cdnDomainService;
    /**
     * 补执行22-24点的计费任务 - 手动触发
     * 注释掉@Scheduled，需要时手动取消注释
     */
//     @Scheduled(cron = "0 6 1 * * ?") // 每天凌晨1点执行
    public void flowBilling22To24Hours() {
        String targetDate = "2025-08-17"; // 修改为需要补执行的日期

        log.info("=== 开始补执行22-24点计费任务 - 目标日期: {} ===", targetDate);

        try {
            flowBillingService.flowBillingFor22To24Hours(targetDate);
            log.info("=== 22-24点计费补执行完成 ===");
        } catch (Exception e) {
            log.error("22-24点计费补执行失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 补执行指定时间段的计费任务 - 手动触发
     */
    // @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public void flowBillingSpecificHours() {
        String targetDate = "2025-08-17"; // 修改为需要补执行的日期
        int startHour = 22; // 开始小时 (22点)
        int endHour = 24;   // 结束小时 (24点，即到午夜)

        log.info("=== 开始补执行指定时间段计费任务 - 日期: {}, 时间: {}:00-{}:00 ===",
                targetDate, startHour, endHour);

        try {
            flowBillingService.flowBillingForSpecificHours(targetDate, startHour, endHour);
            log.info("=== 指定时间段计费补执行完成 ===");
        } catch (Exception e) {
            log.error("指定时间段计费补执行失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 补执行8月8日13点到8月18日20点的计费任务
     * 针对在这个时间段内purchase_flow_detail表中一条数据都没有的用户
     * 设置为2025年8月17日10:55执行一次
     */
//     @Scheduled(cron = "0 0 13 18 8 ?") // 2025年8月18日10:55执行
    public void flowBillingForAug8To18() {
        log.info("=== 开始补执行8月8日13点到8月18日20点的计费任务 ===");

        try {
            flowBillingService.flowBillingForAug8To18();
            log.info("=== 8月8日13点到8月18日20点计费补执行完成 ===");
        } catch (Exception e) {
            log.error("8月8日13点到8月18日20点计费补执行失败: {}", e.getMessage(), e);
        }
    }


    // @Scheduled(cron = "0 */1 * * * ?") // 注释掉，避免频繁执行
    public void flowBillingHourDebug() {
        Long targetUserId = 1781308141746372610L;

        log.info("=== 开始调试用户 {} 的计费逻辑 ===", targetUserId);

        // 1. 获取该用户的所有域名
        List<CdnDomain> allDomains = cdnDomainService.queryAll();
        List<CdnDomain> userDomains = allDomains.stream()
                .filter(domain -> targetUserId.equals(domain.getUserId()))
                .collect(Collectors.toList());

        log.info("用户 {} 拥有的域名数量: {}", targetUserId, userDomains.size());
        for (CdnDomain domain : userDomains) {
            log.info("域名: {}, ID: {}, 状态: {}, 路由: {}",
                    domain.getDomainName(), domain.getId(), domain.getDomainStatus(), domain.getRoute());
        }

        if (userDomains.isEmpty()) {
            log.warn("用户 {} 没有任何域名，无法进行计费测试", targetUserId);
            return;
        }

        // 2. 模拟计费逻辑中的分组操作
        Map<Long, List<CdnDomain>> userIdMaps = userDomains.stream()
                .collect(Collectors.groupingBy(CdnDomain::getUserId));

        log.info("分组后的用户映射: {}", userIdMaps.keySet());

        // 3. 获取时间范围（上一小时）
        DateTime currentHour = DateUtil.beginOfHour(DateUtil.date());
        DateTime lastHour = DateUtil.offsetHour(currentHour, -1);

        log.info("计费时间范围: {} -> {}", lastHour, currentHour);

        // 4. 模拟线程池执行逻辑（但在测试中直接执行）
        for (Map.Entry<Long, List<CdnDomain>> userIdEntry : userIdMaps.entrySet()) {
            Long userId = userIdEntry.getKey();
            List<CdnDomain> userCdnDomains = userIdEntry.getValue();

            log.info("=== 开始处理用户 {} 的计费 ===", userId);
            log.info("用户域名列表: {}", userCdnDomains.stream()
                    .map(CdnDomain::getDomainName)
                    .collect(Collectors.joining(", ")));

            try {
                // 调用实际的计费方法
                flowBillingService.userFlowBilling(userId, userCdnDomains, lastHour, currentHour);
                log.info("用户 {} 计费处理完成", userId);
            } catch (Exception e) {
                log.error("用户 {} 计费处理失败: {}", userId, e.getMessage(), e);
            }
        }


    }

    /*
     * 使用说明：
     *
     * 1. 补执行22-24点计费任务：
     *    - 修改 flowBilling22To24Hours() 方法中的 targetDate 为需要补执行的日期
     *    - 取消注释 @Scheduled 注解，或者通过测试方法手动调用
     *
     * 2. 补执行指定时间段计费任务：
     *    - 修改 flowBillingSpecificHours() 方法中的参数：
     *      * targetDate: 目标日期
     *      * startHour: 开始小时（包含）
     *      * endHour: 结束小时（不包含）
     *    - 取消注释 @Scheduled 注解，或者通过测试方法手动调用
     *
     * 3. 补执行8月8日13点到8月18日20点计费任务：
     *    - 针对在这个时间段内purchase_flow_detail表中一条数据都没有的用户
     *    - 取消注释 flowBillingForAug8To18() 方法的 @Scheduled 注解
     *    - 模拟时间从13:30逐小时到20:30
     *
     * 4. 调试单个用户计费：
     *    - 修改 flowBillingHourDebug() 方法中的 targetUserId
     *    - 取消注释 @Scheduled 注解进行定时调试
     *
     * 注意：使用完毕后请重新注释掉 @Scheduled 注解，避免影响生产环境
     */
}
