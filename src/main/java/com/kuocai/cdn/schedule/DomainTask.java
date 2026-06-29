package com.kuocai.cdn.schedule;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kuocai.cdn.api.DomainBasicInfo;
import com.kuocai.cdn.api.DomainConfig;
import com.kuocai.cdn.async.SmsAsync;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.CdnDomainService;
import com.kuocai.cdn.service.CdnDomainStatisticsService;
import com.kuocai.cdn.service.SysUserService;
import com.kuocai.cdn.service.domain.operation.AliyunDomainServiceImpl;
import com.kuocai.cdn.service.domain.operation.ICdnPlatformService;
import com.kuocai.cdn.service.factory.CdnPlatformFactory;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.JedisUtil;
import com.kuocai.cdn.util.KuocaiBaseUtil;
import com.kuocai.cdn.util.KuocaiDateUtil;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;


/**
 * 域名相关定时任务类
 */
@Slf4j
@Component
@Profile("prod")
public class DomainTask {

    private final AliyunDomainServiceImpl aliyunDomainService;
    private final SmsAsync smsAsync;
    private final CdnDomainStatisticsService statisticsService;
    private final SysUserService sysUserService;
    private final Executor taskExecutor;

    @Resource
    private CdnDomainService cdnDomainService;

    DomainTask(AliyunDomainServiceImpl aliyunDomainService, SmsAsync smsAsync, CdnDomainStatisticsService statisticsService,
               SysUserService sysUserService, @Qualifier("cdnDomainExecutor") Executor taskExecutor) {
        this.aliyunDomainService = aliyunDomainService;
        this.smsAsync = smsAsync;
        this.statisticsService = statisticsService;
        this.sysUserService = sysUserService;
        this.taskExecutor = taskExecutor;
    }

    @Scheduled(cron = "0/30 * * * * ?")
    public void updateAliyunDomainInfo() {
        log.info("开始更新阿里云加速域名 cname 没有的");
        QueryWrapper<CdnDomain> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("route", "aliyun").eq("cname_aliyun", "");
        List<CdnDomain> cdnDomains = cdnDomainService.queryByWrapper(queryWrapper);
        for (CdnDomain cdnDomain : cdnDomains) {
            taskExecutor.execute(() -> {
                String domainName = cdnDomain.getDomainName();
                log.info("尝试更新域名[{}] cname 开始", domainName);
                try {
                    String cname = aliyunDomainService.getCname(domainName);
                    if (Assert.isEmpty(cname)) {
                        return;
                    }
                    cdnDomain.setCnameAliyun(cname);
                    aliyunDomainService.configDNS(cdnDomain);
                } catch (BusinessException e) {
                    log.info("更新域名[{}] cname 失败：{}", domainName, e.getMessage());
                } catch (TencentCloudSDKException e) {
                    log.info("更新域名[{}] cname dns 失败：{}", domainName, e.getMessage());
                }
            });
        }
    }

    /**
     * 更新加速域名详情状态
     */
//    @Scheduled(cron = "0 */5 * * * ?")
    public void updateNotConfiguringDomainInfoStatus() {
        log.info("开始更新非配置中加速域名详情状态");
        QueryWrapper<CdnDomain> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne("domain_status", "configuring");
        List<CdnDomain> cdnDomains = cdnDomainService.queryByWrapper(queryWrapper);
        List<List<CdnDomain>> partitionList = KuocaiBaseUtil.partition(cdnDomains, 10);
        for (List<CdnDomain> domainList : partitionList) {
            taskExecutor.execute(() -> {
                for (CdnDomain cdnDomain : domainList) {
                    try {
                        ICdnPlatformService cdnPlatformService = CdnPlatformFactory.getCdnPlatform(cdnDomain.getRoute());
                        DomainConfig domainConfig = cdnPlatformService.getDomainConfig(cdnDomain.getDomainName());
                        DomainBasicInfo domainBasicInfo = domainConfig.getDomainBasicInfo();
                        String domainStatus = domainBasicInfo.getDomainStatus();
                        if (ObjectUtil.notEqual(domainStatus, cdnDomain.getDomainStatus())
                                || ObjectUtil.notEqual(domainBasicInfo.getBusinessType(), cdnDomain.getBusinessType())
                                || ObjectUtil.notEqual(domainBasicInfo.getServiceArea(), cdnDomain.getServiceArea())) {
                            log.info("[{}]域名更新状态：{} -> {}", cdnDomain.getDomainName(), cdnDomain.getDomainStatus(), domainStatus);
                            cdnDomain.setDomainName(domainBasicInfo.getDomainName());
                            cdnDomain.setBusinessType(domainBasicInfo.getBusinessType());
                            cdnDomain.setServiceArea(domainBasicInfo.getServiceArea());
                            cdnDomain.setDomainStatus(domainStatus);
                            cdnDomainService.save(cdnDomain);
                        }
                    } catch (BusinessException e) {
                        log.warn("查询域名[{}] - [{}] 状态失败：{}", cdnDomain.getRoute(), cdnDomain.getDomainName(), e.getMessage(), e);
                        // cdnDomainService.deleteExceptionDomain(e.getMessage());
                    }
                }
            });
        }
    }

    /**
     * 更新加速域名详情状态
     */
    @Profile("prod")
    @Scheduled(cron = "0 * * * * *")
    public void updateConfiguringDomainInfoStatus() {
        log.info("开始更新配置中加速域名详情状态");
        List<CdnDomain> cdnDomains = cdnDomainService.queryByObj(CdnDomain.builder().domainStatus("configuring").build());
        List<List<CdnDomain>> partitionList = KuocaiBaseUtil.partition(cdnDomains, 10);
        for (List<CdnDomain> domainList : partitionList) {
            taskExecutor.execute(() -> {
                // 查询域名的状态
                for (CdnDomain cdnDomain : domainList) {
                    try {
                        // // 检查域名是否卡在"配置中"状态超过5分钟
                        // Date updateTime = cdnDomain.getUpdateTime();
                        // if (updateTime != null) {
                        //     long diffMinutes = (System.currentTimeMillis() - updateTime.getTime()) / (60 * 1000);
                        //     // 如果超过5分钟还在"配置中"状态，则自动更新为"online"
                        //     if (diffMinutes >= 5) {
                        //         log.info("[{}]域名配置中状态超过5分钟，自动更新为已启用状态", cdnDomain.getDomainName());
                        //         cdnDomain.setDomainStatus("online");
                        //         cdnDomainService.save(cdnDomain);
                        //         continue;
                        //     }
                        // }

                        // 正常查询域名状态
                        ICdnPlatformService cdnPlatformService = CdnPlatformFactory.getCdnPlatform(cdnDomain.getRoute());
                        DomainConfig domainConfig = cdnPlatformService.getDomainConfig(cdnDomain.getDomainName());
                        DomainBasicInfo domainBasicInfo = domainConfig.getDomainBasicInfo();
                        String domainStatus = domainBasicInfo.getDomainStatus();
                        log.info("从服务商处获取到域名[{}]的原始状态为: {}", cdnDomain.getDomainName(), domainStatus);
                        if (ObjectUtil.notEqual(domainStatus, "configuring")) {
                            log.info("[{}]域名状态发生变更，准备更新：旧状态: {}, 新状态: {}", cdnDomain.getDomainName(), cdnDomain.getDomainStatus(), domainStatus);
                            cdnDomain.setDomainName(domainBasicInfo.getDomainName());
                            cdnDomain.setBusinessType(domainBasicInfo.getBusinessType());
                            cdnDomain.setServiceArea(domainBasicInfo.getServiceArea());
                            cdnDomain.setDomainStatus(domainStatus);
                            cdnDomainService.save(cdnDomain);
                        }
                    } catch (BusinessException e) {
                        String errorMsg = e.getMessage();
                        log.error("查询域名[{}] - [{}] 状态时捕获到业务异常: {}", cdnDomain.getRoute(), cdnDomain.getDomainName(), errorMsg, e);
                        // 如果错误明确指出域名在供应商处不存在，则更新本地数据库状态
                        if (errorMsg != null && errorMsg.contains("账号下无此域名")) {
                            log.info("检测到域名 [{}] 在供应商 [{}] 处不存在，将本地状态更新为 'deleted'", cdnDomain.getDomainName(), cdnDomain.getRoute());
                            // cdnDomain.setDomainStatus("deleted"); // 设置为已删除状态
                            // cdnDomainService.save(cdnDomain);
                        }
                    }
                }
            });
        }
    }

    /**
     * 统计普通用户的域名数据
     */
//    @Scheduled(cron = "0 */5 * * * ?")
    public void statisticalUserData() {
        String startTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd 00:00:00"));
        String endTime = LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd 00:00:00"));
        String type = "Resource";
        List<SysUser> sysUsers = sysUserService.queryAll();
        if (Assert.isEmpty(sysUsers)) {
            return;
        }

        int totalSize = sysUsers.size();
        int partitionSize = Math.max(1, (int) Math.ceil((double) totalSize / 10));

        List<List<SysUser>> partitions = new ArrayList<>();
        for (int i = 0; i < sysUsers.size(); i += partitionSize) {
            partitions.add(sysUsers.subList(i, Math.min(i + partitionSize, sysUsers.size())));
        }
        List<List<SysUser>> collected = partitions.stream()
                .map(subList -> subList.stream().collect(Collectors.toList()))
                .collect(Collectors.toList());
        for (List<SysUser> userPartition : collected) {
            taskExecutor.execute(() -> {
                for (SysUser sysUser : userPartition) {
                    Long userId = sysUser.getId();
                    List<CdnDomain> cdnDomains = cdnDomainService.queryByUserId(userId);
                    if (Assert.isEmpty(cdnDomains)) {
                        continue;
                    }
                    DateTime start = DateUtil.parse(startTime);
                    DateTime end = DateUtil.parse(endTime);
                    try {
                        String key = String.format("Statistics:%d:%s:%s:%s->%s", userId, type, cdnDomains.hashCode(), start.getTime(), end.getTime());
                        JedisUtil.delKey(key);
                        statisticsService.mergeAllPlatForm(cdnDomains, start, end, type, userId);
                    } catch (Exception e) {
                        log.error("统计并缓存用户数据失败，用户：{}，{}", sysUser.getUserName(), e.getMessage());
                        // cdnDomainService.deleteExceptionDomain(e.getMessage());
                    }
                    log.info("统计并缓存用户数据，用户：{}", sysUser.getUserName());
                }
            });
        }
    }

    /**
     * 统计管理员域名数据
     */
    @Scheduled(cron = "0 */20 * * * ?")
    public void statisticalAdminData() throws InterruptedException {
        String startTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd 00:00:00"));
        String endTime = LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd 00:00:00"));
        String type = "Resource";
        List<SysUser> sysUsers = sysUserService.queryAllAdmins();
        for (SysUser sysUser : sysUsers) {
            Thread.sleep(30000);
            taskExecutor.execute(() -> {
                Long userId = sysUser.getId();
                List<CdnDomain> cdnDomains = cdnDomainService.queryAll();
                DateTime start = DateUtil.parse(startTime);
                DateTime end = DateUtil.parse(endTime);
                try {
                    String key = String.format("Statistics:%d:%s:%s:%s->%s", userId, type, cdnDomains.hashCode(), start.getTime(), end.getTime());
                    JedisUtil.delKey(key);
                    statisticsService.mergeAllPlatForm(cdnDomains, start, end, type, userId);
                } catch (Exception e) {
                    String message = e.getMessage();
                    if (message != null && message.startsWith("您没有权限操作域名:")) {
                        String domainName = message.replace("您没有权限操作域名: ", "");
                        CdnDomain cdnDomain = cdnDomainService.queryByDomainName(domainName);
                        cdnDomainService.deleteById(cdnDomain.getId());
                    }
                }
                log.info("统计并缓存用户数据，用户：{}", sysUser.getUserName());
            });
        }
    }

    /**
     * 删除长时间未使用的域名（6天）提醒
     */
    @Scheduled(cron = "0 0/10 * * * ?")
    public void notifyLongTimeNoUseDomain() {
        List<CdnDomain> cdnDomains = cdnDomainService.queryByObj(CdnDomain.builder().domainStatus("offline").build());
        for (CdnDomain cdnDomain : cdnDomains) {
            if (!KuocaiDateUtil.isOverDays(cdnDomain.getUpdateTime(), 6)) {
                continue;
            }
            String domainName = cdnDomain.getDomainName();
            String cacheKey = "Notify:" + domainName;
            if (JedisUtil.exists(cacheKey)) {
                log.info("今日已完成删除通知：{}", domainName);
                continue;
            }
            try {
                // 发送邮件提醒
                Date updateTime = cdnDomain.getUpdateTime();
                Long userId = cdnDomain.getUserId();
                String deleteTime = KuocaiDateUtil.addDaysToDate(updateTime, 7);
                smsAsync.notifyLongTimeNoUseDomain(userId, domainName, deleteTime);
                log.info("删除提醒成功，域名信息：{}", domainName);
                // 缓存一天时间
                JedisUtil.setStr(cacheKey, "", 86400);
            } catch (Exception e) {
                log.error("删除提醒失败，{}", e.getMessage());
            }
        }
    }

    /**
     * 删除长时间未使用的域名（7天）
     */
    @Scheduled(cron = "0 0/10 * * * ?")
    public void deleteLongTimeNoUseDomain() {
        List<CdnDomain> cdnDomains = cdnDomainService.queryByObj(CdnDomain.builder().domainStatus("offline").build());
        for (CdnDomain cdnDomain : cdnDomains) {
            if (!KuocaiDateUtil.isOverDays(cdnDomain.getUpdateTime(), 7)) {
                continue;
            }
            try {
                ICdnPlatformService iCdnPlatformService = CdnPlatformFactory.getCdnPlatform(cdnDomain.getRoute());
                iCdnPlatformService.delete(cdnDomain);
                // 删除域名信息
                cdnDomainService.deleteById(cdnDomain.getId());
                log.info("删除加速域名成功，域名信息：{}", cdnDomain.getDomainName());
            } catch (Exception e) {
                log.error("删除加速域名失败，{}", e.getMessage());
            }
        }
    }

}
