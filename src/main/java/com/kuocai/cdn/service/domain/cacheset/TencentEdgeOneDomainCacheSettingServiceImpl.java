package com.kuocai.cdn.service.domain.cacheset;

import cn.hutool.core.date.DateUtil;
import com.kuocai.cdn.api.tencent.edgeone.TencentEdgeOneClient;
import com.kuocai.cdn.entity.CacheTask;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.enumeration.CacheTaskType;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.exception.CdnHuaweiException;
import com.kuocai.cdn.service.CdnDomainService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.BrowserUtils;
import com.kuocai.cdn.vo.cacheset.CacheTaskVo;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.teo.v20220901.TeoClient;
import com.tencentcloudapi.teo.v20220901.models.AdvancedFilter;
import com.tencentcloudapi.teo.v20220901.models.CreatePrefetchTaskRequest;
import com.tencentcloudapi.teo.v20220901.models.CreatePrefetchTaskResponse;
import com.tencentcloudapi.teo.v20220901.models.CreatePurgeTaskRequest;
import com.tencentcloudapi.teo.v20220901.models.CreatePurgeTaskResponse;
import com.tencentcloudapi.teo.v20220901.models.DescribePrefetchTasksRequest;
import com.tencentcloudapi.teo.v20220901.models.DescribePrefetchTasksResponse;
import com.tencentcloudapi.teo.v20220901.models.DescribePurgeTasksRequest;
import com.tencentcloudapi.teo.v20220901.models.DescribePurgeTasksResponse;
import com.tencentcloudapi.teo.v20220901.models.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TencentEdgeOneDomainCacheSettingServiceImpl implements ICdnCacheSettingPlatformService {

    @Autowired
    private CdnDomainService cdnDomainService;

    @Override
    public String submitCachePreheating(String[] urls) throws BusinessException {
        try {
            List<String> jobIds = new ArrayList<>();
            TeoClient client = TencentEdgeOneClient.getClient();
            for (Map.Entry<String, List<String>> entry : groupUrlsByZone(urls).entrySet()) {
                CreatePrefetchTaskRequest request = new CreatePrefetchTaskRequest();
                request.setZoneId(entry.getKey());
                request.setTargets(entry.getValue().toArray(new String[0]));
                request.setEncodeUrl(true);
                CreatePrefetchTaskResponse response = client.CreatePrefetchTask(request);
                log.info("EdgeOne prefetch result: {}", CreatePrefetchTaskResponse.toJsonString(response));
                if (Assert.notEmpty(response.getJobId())) {
                    jobIds.add(response.getJobId());
                }
            }
            return String.join(",", jobIds);
        } catch (BusinessException e) {
            throw e;
        } catch (TencentCloudSDKException e) {
            throw new BusinessException("腾讯云 EdgeOne 预热失败：" + e.getMessage());
        }
    }

    @Override
    public String submitCacheRefresh(String[] urls, String type) throws BusinessException {
        try {
            List<String> jobIds = new ArrayList<>();
            TeoClient client = TencentEdgeOneClient.getClient();
            for (Map.Entry<String, List<String>> entry : groupUrlsByZone(urls).entrySet()) {
                CreatePurgeTaskRequest request = new CreatePurgeTaskRequest();
                request.setZoneId(entry.getKey());
                request.setType("directory".equals(type) ? "purge_prefix" : "purge_url");
                request.setTargets(entry.getValue().toArray(new String[0]));
                request.setEncodeUrl(true);
                CreatePurgeTaskResponse response = client.CreatePurgeTask(request);
                log.info("EdgeOne purge result: {}", CreatePurgeTaskResponse.toJsonString(response));
                if (Assert.notEmpty(response.getJobId())) {
                    jobIds.add(response.getJobId());
                }
            }
            return String.join(",", jobIds);
        } catch (BusinessException e) {
            throw e;
        } catch (TencentCloudSDKException e) {
            throw new BusinessException("腾讯云 EdgeOne 刷新失败：" + e.getMessage());
        }
    }

    @Override
    public void queryTaskInfo(CacheTaskType cacheTaskType, List<CacheTaskVo> results, Map<Long, SysUser> sysUserMap, CacheTask cacheTask) throws CdnHuaweiException {
        try {
            Task[] tasks = "refresh".equals(cacheTaskType.getCode()) ? queryPurgeTasks(cacheTask) : queryPrefetchTasks(cacheTask);
            for (Task task : tasks) {
                SysUser user = sysUserMap.get(cacheTask.getUserId());
                CacheTaskVo cacheTaskVo = CacheTaskVo.builder()
                        .taskType(cacheTaskType.getName())
                        .url(task.getTarget())
                        .fileType("purge_prefix".equals(task.getType()) ? "目录" : "文件")
                        .createTime(task.getCreateTime())
                        .createTimeLong(convertTime(task.getCreateTime()))
                        .status(convertStatus(task.getStatus()))
                        .userId(user == null ? cacheTask.getUserId() : user.getId())
                        .userName(user == null ? "" : user.getUserName())
                        .img(user == null ? "" : user.getImg())
                        .build();
                results.add(cacheTaskVo);
            }
        } catch (Exception e) {
            throw new CdnHuaweiException("查询腾讯云 EdgeOne 缓存任务失败：" + e.getMessage());
        }
    }

    private Task[] queryPurgeTasks(CacheTask cacheTask) throws BusinessException, TencentCloudSDKException {
        List<Task> tasks = new ArrayList<>();
        TeoClient client = TencentEdgeOneClient.getClient();
        for (String zoneId : TencentEdgeOneClient.listZoneIds()) {
            for (String jobId : splitJobIds(cacheTask)) {
                DescribePurgeTasksRequest request = new DescribePurgeTasksRequest();
                applyTaskQuery(request, cacheTask, zoneId, jobId);
                DescribePurgeTasksResponse response = client.DescribePurgeTasks(request);
                if (response.getTasks() != null) {
                    tasks.addAll(Arrays.asList(response.getTasks()));
                }
            }
        }
        return tasks.toArray(new Task[0]);
    }

    private Task[] queryPrefetchTasks(CacheTask cacheTask) throws BusinessException, TencentCloudSDKException {
        List<Task> tasks = new ArrayList<>();
        TeoClient client = TencentEdgeOneClient.getClient();
        for (String zoneId : TencentEdgeOneClient.listZoneIds()) {
            for (String jobId : splitJobIds(cacheTask)) {
                DescribePrefetchTasksRequest request = new DescribePrefetchTasksRequest();
                request.setStartTime(startTime(cacheTask));
                request.setEndTime(endTime());
                request.setLimit(100L);
                request.setFilters(new AdvancedFilter[]{filter("zone-id", zoneId), filter("job-id", jobId)});
                DescribePrefetchTasksResponse response = client.DescribePrefetchTasks(request);
                if (response.getTasks() != null) {
                    tasks.addAll(Arrays.asList(response.getTasks()));
                }
            }
        }
        return tasks.toArray(new Task[0]);
    }

    private void applyTaskQuery(DescribePurgeTasksRequest request, CacheTask cacheTask, String zoneId, String jobId) {
        request.setStartTime(startTime(cacheTask));
        request.setEndTime(endTime());
        request.setLimit(100L);
        request.setFilters(new AdvancedFilter[]{filter("zone-id", zoneId), filter("job-id", jobId)});
    }

    private AdvancedFilter filter(String name, String value) {
        AdvancedFilter filter = new AdvancedFilter();
        filter.setName(name);
        filter.setValues(new String[]{value});
        filter.setFuzzy(false);
        return filter;
    }

    private Map<String, List<String>> groupUrlsByZone(String[] urls) throws BusinessException {
        List<String> domainNames = new ArrayList<>();
        Map<String, List<String>> domainUrlMap = new LinkedHashMap<>();
        for (String url : urls) {
            try {
                String domainName = BrowserUtils.getDomainByUrl(url);
                domainNames.add(domainName);
                domainUrlMap.computeIfAbsent(domainName, item -> new ArrayList<>()).add(url);
            } catch (URISyntaxException e) {
                throw new BusinessException("URL格式错误：" + url);
            }
        }
        Map<String, CdnDomain> domainMap = cdnDomainService.queryByDomainNames(domainNames).stream()
                .collect(Collectors.toMap(CdnDomain::getDomainName, item -> item, (first, second) -> first));
        Map<String, List<String>> zoneUrlMap = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : domainUrlMap.entrySet()) {
            CdnDomain cdnDomain = domainMap.get(entry.getKey());
            String zoneId = cdnDomain == null ? null : cdnDomain.getDomainId();
            if (Assert.isEmpty(zoneId)) {
                zoneId = TencentEdgeOneClient.resolveZoneId(entry.getKey());
            }
            zoneUrlMap.computeIfAbsent(zoneId, item -> new ArrayList<>()).addAll(entry.getValue());
        }
        return zoneUrlMap;
    }

    private List<String> splitJobIds(CacheTask cacheTask) {
        if (cacheTask == null || Assert.isEmpty(cacheTask.getTaskId())) {
            return new ArrayList<>();
        }
        return Arrays.stream(cacheTask.getTaskId().split(","))
                .map(String::trim)
                .filter(Assert::notEmpty)
                .collect(Collectors.toList());
    }

    private String startTime(CacheTask cacheTask) {
        Date createTime = cacheTask.getCreateTime() == null ? new Date() : cacheTask.getCreateTime();
        return DateUtil.offsetDay(createTime, -1).toString("yyyy-MM-dd'T'HH:mm:ssXXX");
    }

    private String endTime() {
        return DateUtil.offsetDay(new Date(), 1).toString("yyyy-MM-dd'T'HH:mm:ssXXX");
    }

    private Long convertTime(String time) {
        if (time == null) {
            return System.currentTimeMillis();
        }
        return DateUtil.parse(time).getTime();
    }

    private String convertStatus(String status) {
        if ("success".equals(status)) {
            return "完成";
        }
        if ("failed".equals(status) || "timeout".equals(status)) {
            return "失败";
        }
        return "处理中";
    }
}
