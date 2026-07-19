package com.kuocai.cdn.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ObjectUtil;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.enumeration.domainmerage.CdnRoute;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.exception.CdnHuaweiException;
import com.kuocai.cdn.entity.CdnDomainRouteBinding;
import com.kuocai.cdn.service.domain.cacheset.ICdnCacheSettingPlatformService;
import com.kuocai.cdn.service.factory.CdnCacheSettingPlatformFactory;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.BrowserUtils;
import com.kuocai.cdn.util.JedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CdnDomainCacheService {

    private static final class CacheRouteTarget {
        private final String route;

        private CacheRouteTarget(String route) {
            this.route = route;
        }
    }

    @Resource
    private CacheTaskService cacheTaskService;

    @Resource
    private CdnDomainService cdnDomainService;

    @Resource
    private CdnDomainRouteBindingService routeBindingService;
    // 抢购票
    public synchronized String purchaseTicket(String prefix, List<String> urls, String taskType, String type, Long loginUserId, String route) throws CdnHuaweiException {
        if (Assert.isEmpty(urls)) {
            return "";
        }
        return purchaseTicketByDomainRoute(prefix, urls, taskType, type, loginUserId);
    }

    private String purchaseTicketByDomainRoute(String prefix, List<String> urls, String taskType,
                                               String type, Long loginUserId) throws CdnHuaweiException {
        Map<String, List<String>> grouped = new LinkedHashMap<>();
        Map<String, CacheRouteTarget> targets = new LinkedHashMap<>();
        StringBuilder failed = new StringBuilder();
        for (String url : urls) {
            try {
                String domainName = BrowserUtils.getDomainByUrl(url);
                CdnDomain domain = cdnDomainService.queryByDomainName(domainName);
                if (domain == null || Assert.isEmpty(domain.getRoute())) {
                    appendFailed(failed, url);
                } else if (CdnRoute.isMultiCdn(domain.getRoute())) {
                    List<CdnDomainRouteBinding> bindings = routeBindingService.listActiveByDomainId(domain.getId());
                    if (bindings.isEmpty()) {
                        appendFailed(failed, url);
                        continue;
                    }
                    for (CdnDomainRouteBinding binding : bindings) {
                        String key = binding.getRoute();
                        grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(url);
                        targets.putIfAbsent(key, new CacheRouteTarget(binding.getRoute()));
                    }
                } else {
                    String key = domain.getRoute();
                    grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(url);
                    targets.putIfAbsent(key, new CacheRouteTarget(domain.getRoute()));
                }
            } catch (Exception e) {
                appendFailed(failed, url);
            }
        }
        for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
            CacheRouteTarget target = targets.get(entry.getKey());
            String result;
            try {
                result = purchaseTicketSingle(prefix, entry.getValue(), taskType, type,
                        loginUserId, target.route);
            } catch (Exception e) {
                result = String.join(",", entry.getValue());
                log.warn("提交线路组缓存任务失败，线路：{}，原因：{}", target.route, e.getMessage());
            }
            appendFailed(failed, result);
        }
        return failed.toString();
    }

    private String purchaseTicketSingle(String prefix, List<String> urls, String taskType,
                                        String type, Long loginUserId, String route) throws CdnHuaweiException {
        try {
            if (CdnRoute.isSelfHosted(route)) {
                return submitSelfHostedCacheTask(urls, taskType, type, loginUserId, route);
            }
            if (ObjectUtil.equal(route, CdnRoute.HUAWEI_VOLCENGINE.getCode())) {
                return purchaseTicketHV(prefix, urls, taskType, type, loginUserId);
            } else {
                // 获取到锁
                String key = prefix + "_" + route + ":" + DateUtil.today();
                int piaoNum = Integer.parseInt(JedisUtil.getStr(key));
                int num = urls.size();
                if (piaoNum >= num) {
                    if (ObjectUtil.equal(taskType, "refresh")) {
                        ICdnCacheSettingPlatformService cdnCacheSettingPlatformService = CdnCacheSettingPlatformFactory.getCdnPlatform(route);
                        String taskId = cdnCacheSettingPlatformService.submitCacheRefresh(urls.toArray(new String[0]), type);
                        if (Assert.notEmpty(taskId)) {
                            cacheTaskService.insertCacheTaskInfo(taskId, taskType, type, route, loginUserId);
                        }
                    } else {
                        ICdnCacheSettingPlatformService cdnCacheSettingPlatformService = CdnCacheSettingPlatformFactory.getCdnPlatform(route);
                        String taskId = cdnCacheSettingPlatformService.submitCachePreheating(urls.toArray(new String[0]));
                        if (Assert.notEmpty(taskId)) {
                            cacheTaskService.insertCacheTaskInfo(taskId, taskType, null, route, loginUserId);
                        }
                    }
                    // 还有余票，减少票数
                    JedisUtil.decrBy(key, num);
                    return "";
                } else {
                    return urls.stream().collect(Collectors.joining(","));
                }
            }
        } catch (Exception e) {
            log.error("缓存配置失败->URLs:[{}],error:[{}]", urls, e.getMessage());
            return urls.stream().collect(Collectors.joining(","));
        }
    }

    private String submitSelfHostedCacheTask(List<String> urls, String taskType, String type,
                                             Long loginUserId, String route) throws BusinessException {
        ICdnCacheSettingPlatformService platform = CdnCacheSettingPlatformFactory.getCdnPlatform(route);
        String taskId;
        if (ObjectUtil.equal(taskType, "refresh")) {
            taskId = platform.submitCacheRefresh(urls.toArray(new String[0]), type);
            if (Assert.notEmpty(taskId)) {
                cacheTaskService.insertCacheTaskInfo(taskId, taskType, type, route, loginUserId);
            }
        } else {
            taskId = platform.submitCachePreheating(urls.toArray(new String[0]));
            if (Assert.notEmpty(taskId)) {
                cacheTaskService.insertCacheTaskInfo(taskId, taskType, null, route, loginUserId);
            }
        }
        return Assert.notEmpty(taskId) ? "" : urls.stream().collect(Collectors.joining(","));
    }

    private void appendFailed(StringBuilder failed, String value) {
        if (Assert.isEmpty(value)) {
            return;
        }
        if (failed.length() > 0) {
            failed.append(",");
        }
        failed.append(value);
    }
    public String purchaseTicketHV(String prefix, List<String> urls, String taskType, String type, Long loginUserId) throws BusinessException {
        // 获取到锁
        String hwKey = prefix + "_" + CdnRoute.HUAWEI.getCode() + ":" + DateUtil.today();
        String volKey = prefix + "_" + CdnRoute.VOLCENGINE.getCode() + ":" + DateUtil.today();
        int hwPiaoNum = Integer.parseInt(JedisUtil.getStr(hwKey));
        int volPiaoNum = Integer.parseInt(JedisUtil.getStr(volKey));
        int num = urls.size();
        if (hwPiaoNum >= num && volPiaoNum >= num) {
            String route = CdnRoute.HUAWEI_VOLCENGINE.getCode();
            if (ObjectUtil.equal(taskType, "refresh")) {
                ICdnCacheSettingPlatformService cdnCacheSettingPlatformService = CdnCacheSettingPlatformFactory.getCdnPlatform(route);
                String taskId = cdnCacheSettingPlatformService.submitCacheRefresh(urls.toArray(new String[0]), type);
                if (Assert.notEmpty(taskId)) {
                    cacheTaskService.insertCacheTaskInfo(taskId, taskType, type, route, loginUserId);
                }
            } else {
                ICdnCacheSettingPlatformService cdnCacheSettingPlatformService = CdnCacheSettingPlatformFactory.getCdnPlatform(route);
                String taskId = cdnCacheSettingPlatformService.submitCachePreheating(urls.toArray(new String[0]));
                if (Assert.notEmpty(taskId)) {
                    cacheTaskService.insertCacheTaskInfo(taskId, taskType, null, route, loginUserId);
                }
            }
            // 还有余票，减少票数
            JedisUtil.decrBy(hwKey, num);
            JedisUtil.decrBy(volKey, num);
            return "";
        } else {
            return urls.stream().collect(Collectors.joining(","));
        }
    }
}
