package com.kuocai.cdn.service.domain.cacheset;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kuocai.cdn.dao.SelfHostedCacheJobDao;
import com.kuocai.cdn.dao.SelfHostedCacheJobNodeDao;
import com.kuocai.cdn.dao.SelfHostedDomainConfigDao;
import com.kuocai.cdn.dao.SelfHostedGroupNodeDao;
import com.kuocai.cdn.dao.SelfHostedNodeDao;
import com.kuocai.cdn.dao.CdnDomainDao;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.entity.CacheTask;
import com.kuocai.cdn.entity.SelfHostedCacheJob;
import com.kuocai.cdn.entity.SelfHostedCacheJobNode;
import com.kuocai.cdn.entity.SelfHostedDomainConfig;
import com.kuocai.cdn.entity.SelfHostedGroupNode;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.enumeration.CacheTaskType;
import com.kuocai.cdn.exception.CdnHuaweiException;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.vo.cacheset.CacheTaskVo;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.net.URI;

@Service
public class SelfHostedDomainCacheSettingServiceImpl implements ICdnCacheSettingPlatformService {
    private final SelfHostedCacheJobDao cacheJobDao;
    private final SelfHostedCacheJobNodeDao cacheJobNodeDao;
    private final CdnDomainDao cdnDomainDao;
    private final SelfHostedDomainConfigDao domainConfigDao;
    private final SelfHostedGroupNodeDao groupNodeDao;
    private final SelfHostedNodeDao nodeDao;

    public SelfHostedDomainCacheSettingServiceImpl(SelfHostedCacheJobDao cacheJobDao,
                                                    SelfHostedCacheJobNodeDao cacheJobNodeDao,
                                                    CdnDomainDao cdnDomainDao,
                                                    SelfHostedDomainConfigDao domainConfigDao,
                                                    SelfHostedGroupNodeDao groupNodeDao,
                                                    SelfHostedNodeDao nodeDao) {
        this.cacheJobDao = cacheJobDao;
        this.cacheJobNodeDao = cacheJobNodeDao;
        this.cdnDomainDao = cdnDomainDao;
        this.domainConfigDao = domainConfigDao;
        this.groupNodeDao = groupNodeDao;
        this.nodeDao = nodeDao;
    }

    @Override public String submitCachePreheating(String[] urls) throws BusinessException { return saveJob("preheat", "file", urls); }
    @Override public String submitCacheRefresh(String[] urls, String type) throws BusinessException { return saveJob("refresh", type, urls); }

    @Override
    public void queryTaskInfo(CacheTaskType taskType, List<CacheTaskVo> results,
                              Map<Long, SysUser> users, CacheTask cacheTask) throws CdnHuaweiException {
        SelfHostedCacheJob job = cacheJobDao.selectOne(new QueryWrapper<SelfHostedCacheJob>()
                .eq("task_id", cacheTask.getTaskId()).last("LIMIT 1"));
        if (job == null || users.get(cacheTask.getUserId()) == null) return;
        SysUser user = users.get(cacheTask.getUserId());
        for (String url : JSON.parseArray(job.getTargetsJson(), String.class)) {
            results.add(CacheTaskVo.builder().taskType(taskType.getName()).url(url)
                    .fileType("directory".equals(job.getTargetType()) ? "目录" : "文件")
                    .createTime(job.getCreateTime().toString()).createTimeLong(job.getCreateTime().getTime())
                    .status(statusName(job.getStatus())).userId(user.getId()).userName(user.getUserName())
                    .img(user.getImg()).build());
        }
    }

    private String saveJob(String operation, String targetType, String[] urls) throws BusinessException {
        Map<Long, List<String>> nodeTargets = resolveNodeTargets(urls);
        if (nodeTargets.isEmpty()) {
            throw new BusinessException("缓存任务没有匹配到可用的自建 CDN 节点");
        }
        String taskId = UUID.randomUUID().toString().replace("-", "");
        Date now = new Date();
        SelfHostedCacheJob job = SelfHostedCacheJob.builder().taskId(taskId).userId(0L).operation(operation)
                .targetType(targetType).targetsJson(JSON.toJSONString(Arrays.asList(urls))).status("pending")
                .totalNodes(nodeTargets.size()).successNodes(0).failedNodes(0).createTime(now).updateTime(now).build();
        cacheJobDao.insert(job);
        for (Map.Entry<Long, List<String>> entry : nodeTargets.entrySet()) {
            Long nodeId = entry.getKey();
            cacheJobNodeDao.insert(SelfHostedCacheJobNode.builder().jobId(job.getId()).nodeId(nodeId)
                    .targetsJson(JSON.toJSONString(entry.getValue())).status("pending")
                    .createTime(now).updateTime(now).build());
        }
        return taskId;
    }

    private Map<Long, List<String>> resolveNodeTargets(String[] urls) throws BusinessException {
        Map<Long, List<String>> result = new LinkedHashMap<>();
        try {
            for (String url : urls) {
                String host = new URI(url).getHost();
                if (host == null) continue;
                CdnDomain domain = cdnDomainDao.selectOne(new QueryWrapper<CdnDomain>()
                        .eq("domain_name", host.toLowerCase()).eq("route", "self_hosted").last("LIMIT 1"));
                if (domain == null) continue;
                SelfHostedDomainConfig config = domainConfigDao.selectOne(new QueryWrapper<SelfHostedDomainConfig>()
                        .eq("cdn_domain_id", domain.getId()).eq("status", "enabled").last("LIMIT 1"));
                if (config == null) continue;
                for (SelfHostedGroupNode relation : groupNodeDao.selectList(
                        new QueryWrapper<SelfHostedGroupNode>().eq("group_id", config.getNodeGroupId()))) {
                    Date onlineAfter = new Date(System.currentTimeMillis() - 90_000L);
                    if (nodeDao.selectCount(new QueryWrapper<com.kuocai.cdn.entity.SelfHostedNode>()
                            .eq("id", relation.getNodeId()).eq("enabled", 1)
                            .in("status", "online", "degraded").ge("last_heartbeat", onlineAfter)) > 0) {
                        result.computeIfAbsent(relation.getNodeId(), key -> new ArrayList<>()).add(url);
                    }
                }
            }
        } catch (Exception e) {
            throw new BusinessException("缓存任务 URL 格式不正确：" + e.getMessage());
        }
        return result;
    }

    private String statusName(String status) {
        if ("completed".equals(status)) return "完成";
        if ("failed".equals(status)) return "失败";
        return "处理中";
    }
}
