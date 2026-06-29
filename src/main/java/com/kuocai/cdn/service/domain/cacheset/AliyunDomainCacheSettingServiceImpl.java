package com.kuocai.cdn.service.domain.cacheset;

import cn.hutool.core.date.DateUtil;
import com.aliyun.cdn20180510.Client;
import com.aliyun.cdn20180510.models.*;
import com.kuocai.cdn.api.aliyun.cdn.AliyunCdnClientFactory;
import com.kuocai.cdn.entity.CacheTask;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.enumeration.CacheTaskType;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.exception.CdnHuaweiException;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.vo.cacheset.CacheTaskVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static com.kuocai.cdn.api.aliyun.cdn.AliyunCdnErrorCodeHandler.catchException;

@Slf4j
@Service
public class AliyunDomainCacheSettingServiceImpl extends BaseService<CdnDomain> implements ICdnCacheSettingPlatformService {

    private final Client aliyunCdnClient;

    AliyunDomainCacheSettingServiceImpl(@Qualifier("aliyunCdnClient") Client aliyunCdnClient) {
        this.aliyunCdnClient = aliyunCdnClient;
    }

    private Client getClient() throws BusinessException {
        try {
            Client client = AliyunCdnClientFactory.getClient();
            if (client == null) {
                throw new BusinessException("阿里云CDN配置未填写或未生效，请先在后台保存正确的阿里云 AccessKey");
            }
            return client;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("阿里云CDN客户端初始化失败，请检查后台阿里云配置");
        }
    }

    @Override
    public String submitCachePreheating(String[] urls) throws BusinessException {
        PushObjectCacheRequest req = new PushObjectCacheRequest();
        req.setObjectPath(String.join("\n", urls));
        req.setArea("domestic");
        Client client = getClient();
        try {
            PushObjectCacheResponse response = client.pushObjectCache(req);
            return response.getBody().getPushTaskId();
        } catch (Exception e) {
            throw catchException(e);
        }
    }

    @Override
    public String submitCacheRefresh(String[] urls, String type) throws BusinessException {
        RefreshObjectCachesRequest req = new RefreshObjectCachesRequest();
        req.setObjectPath(String.join("\n", urls));
        if (type.equals("directory")) {
            req.setObjectType("Directory");
        } else {
            req.setObjectType("File");
        }
        Client client = getClient();
        try {
            RefreshObjectCachesResponse response = client.refreshObjectCaches(req);
            return response.getBody().getRefreshTaskId();
        } catch (Exception e) {
            throw catchException(e);
        }
    }

    private String covertObjectType(String objectType) {
        if ("directory".equals(objectType)) {
            return "目录";
        } else {
            return "文件";
        }
    }

    private Long convertTime(String date) {
        // e.g date: 2020-08-03T08:54:23Z
        return DateUtil.parse(date, "yyyy-MM-dd'T'HH:mm:ss'Z'").getTime();
    }

    private String convertStatus(String status) {
        // Complete：已完成。
        // Pending：等待刷新。
        // Refreshing：刷新中。
        // Failed：刷新失败。
        switch (status) {
            case "Complete":
                return "完成";
            case "Failed":
                return "失败";
            case "Pending":
            case "Refreshing":
            default:
                return "处理中";
        }
    }

    @Override
    public void queryTaskInfo(CacheTaskType cacheTaskType, List<CacheTaskVo> results, Map<Long, SysUser> sysUserMap, CacheTask cacheTask) throws CdnHuaweiException {
        DescribeRefreshTaskByIdResponse response = getDescribeRefreshTaskByIdResponse(cacheTask);
        for (DescribeRefreshTaskByIdResponseBody.DescribeRefreshTaskByIdResponseBodyTasks task : response.getBody().getTasks()) {
            SysUser user = sysUserMap.get(cacheTask.getUserId());
            CacheTaskVo cacheTaskVo = CacheTaskVo.builder()
                    .taskType(cacheTaskType.getName())
                    .url(task.getObjectPath())
                    .fileType(covertObjectType(task.getObjectType()))
                    .createTime(task.getCreationTime())
                    .createTimeLong(convertTime(task.getCreationTime()))
                    .status(convertStatus(task.getStatus()))
                    .userId(user.getId())
                    .userName(user.getUserName())
                    .img(user.getImg())
                    .build();
            results.add(cacheTaskVo);
        }
    }

    private DescribeRefreshTaskByIdResponse getDescribeRefreshTaskByIdResponse(CacheTask cacheTask) throws CdnHuaweiException {
        DescribeRefreshTaskByIdRequest request = new DescribeRefreshTaskByIdRequest();
        request.setTaskId(cacheTask.getTaskId());
        try {
            Client client = getClient();
            return client.describeRefreshTaskById(request);
        } catch (Exception e) {
            log.trace("查询刷新缓存任务失败：", e);
            throw new CdnHuaweiException(e.getMessage());
        }
    }
}
