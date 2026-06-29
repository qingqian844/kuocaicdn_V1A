package com.kuocai.cdn.service.domain.cacheset;

import com.baidubce.services.cdn.CdnClient;
import com.baidubce.services.cdn.model.*;
import com.baidubce.services.cdn.model.cache.GetPrefetchStatusResponse;
import com.baidubce.services.cdn.model.cache.PrefetchStatus;
import com.baidubce.services.cdn.model.cache.PrefetchTask;
import com.kuocai.cdn.entity.CacheTask;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.enumeration.CacheTaskType;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.exception.CdnHuaweiException;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.util.KuocaiDateUtil;
import com.kuocai.cdn.vo.cacheset.CacheTaskVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.kuocai.cdn.api.baidu.cdn.BaiduCdnErrorCodeHandler.catchException;

@Slf4j
@Service
public class BaiduDomainCacheSettingServiceImpl extends BaseService<CdnDomain> implements ICdnCacheSettingPlatformService {
    private final CdnClient baiduCdnClient;

    BaiduDomainCacheSettingServiceImpl(@Qualifier("baiduCdnClient") CdnClient baiduCdnClient) {
        this.baiduCdnClient = baiduCdnClient;
    }

    private CdnClient getClient() {
        return baiduCdnClient;
    }

    @Override
    public String submitCachePreheating(String[] urls) throws BusinessException {
        CdnClient client = getClient();
        PrefetchRequest request = new PrefetchRequest();
        Arrays.stream(urls).forEach(it -> request.addTask(new PrefetchTask().withUrl(it)));
        try {
            PrefetchResponse prefetchResponse = client.prefetch(request);
            return prefetchResponse.getId();
        } catch (Exception e) {
            throw catchException(e);
        }
    }

    @Override
    public String submitCacheRefresh(String[] urls, String type) throws BusinessException {
        CdnClient client = getClient();
        PurgeRequest request = new PurgeRequest();
        if (type.equals("directory")) {
            Arrays.stream(urls).forEach(it -> request.addTask(new PurgeTask().withDirectory(it)));
        } else {
            Arrays.stream(urls).forEach(it -> request.addTask(new PurgeTask().withUrl(it)));
        }
        try {
            PurgeResponse purgeResponse = client.purge(request);
            return purgeResponse.getId();
        } catch (Exception e) {
            throw catchException(e);
        }
    }

    private List<PrefetchStatus> getPrefetchStatus(CacheTask cacheTask) throws CdnHuaweiException {
        CdnClient client = getClient();
        try {
            GetPrefetchStatusResponse response = client.getPrefetchStatus(new GetPrefetchStatusRequest().withId(cacheTask.getTaskId()));
            return response.getDetails();
        } catch (Exception e) {
            log.trace("查询缓存预热任务失败：", e);
            throw new CdnHuaweiException(e.getMessage());
        }
    }

    private List<PurgeStatus> getPurgeStatus(CacheTask cacheTask) throws CdnHuaweiException {
        CdnClient client = getClient();
        try {
            GetPurgeStatusResponse response = client.getPurgeStatus(new GetPurgeStatusRequest().withId(cacheTask.getTaskId()));
            return response.getDetails();
        } catch (Exception e) {
            log.trace("查询刷新缓存任务失败：", e);
            throw new CdnHuaweiException(e.getMessage());
        }
    }

    private String convertStatus(String str) {
        switch (str) {
            case "completed":
                return "完成";
            case "failed":
                return "失败";
            case "in-progress":
            case "waiting":
            default:
                return "处理中";
        }
    }

    private String covertTypeType(String str) {
        if ("directory".equals(str)) {
            return "目录";
        } else {
            return "文件";
        }
    }

    @Override
    public void queryTaskInfo(CacheTaskType cacheTaskType, List<CacheTaskVo> results, Map<Long, SysUser> sysUserMap, CacheTask cacheTask) throws CdnHuaweiException {
        if (cacheTaskType == CacheTaskType.PREHEATING) {
            List<PrefetchStatus> prefetchStatus = getPrefetchStatus(cacheTask);
            prefetchStatus.forEach(it -> {
                SysUser user = sysUserMap.get(cacheTask.getUserId());
                PrefetchTask task = it.getTask();
                CacheTaskVo cacheTaskVo = CacheTaskVo.builder()
                        .taskType(cacheTaskType.getName())
                        .url(task.getUrl())
                        .fileType("文件")
                        .createTime(KuocaiDateUtil.toDateStr(it.getCreatedAt()))
                        .createTimeLong(it.getCreatedAt().getTime())
                        .status(convertStatus(it.getStatus()))
                        .userId(user.getId())
                        .userName(user.getUserName())
                        .img(user.getImg())
                        .build();
                results.add(cacheTaskVo);
            });
        } else {
            List<PurgeStatus> purgeStatus = getPurgeStatus(cacheTask);
            purgeStatus.forEach(it -> {
                SysUser user = sysUserMap.get(cacheTask.getUserId());
                PurgeTask task = it.getTask();
                CacheTaskVo cacheTaskVo = CacheTaskVo.builder()
                        .taskType(cacheTaskType.getName())
                        .url(task.getUrl())
                        .fileType(covertTypeType(task.getType()))
                        .createTime(KuocaiDateUtil.toDateStr(it.getCreatedAt()))
                        .createTimeLong(it.getCreatedAt().getTime())
                        .status(convertStatus(it.getStatus()))
                        .userId(user.getId())
                        .userName(user.getUserName())
                        .img(user.getImg())
                        .build();
                results.add(cacheTaskVo);
            });
        }
    }
}
