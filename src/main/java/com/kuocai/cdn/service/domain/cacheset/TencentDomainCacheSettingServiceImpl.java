package com.kuocai.cdn.service.domain.cacheset;

import cn.hutool.core.date.DateUtil;
import com.kuocai.cdn.api.tencent.cdn.TencentClient;
import com.kuocai.cdn.api.tencent.cdn.TencentErrorCodeHandler;
import com.kuocai.cdn.entity.CacheTask;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.enumeration.CacheTaskType;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.exception.CdnHuaweiException;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.vo.cacheset.CacheTaskVo;
import com.tencentcloudapi.cdn.v20180606.CdnClient;
import com.tencentcloudapi.cdn.v20180606.models.*;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class TencentDomainCacheSettingServiceImpl extends BaseService<CdnDomain> implements ICdnCacheSettingPlatformService {

    @Override
    public String submitCachePreheating(String[] urls) throws BusinessException {
        PushUrlsCacheRequest req = new PushUrlsCacheRequest();
        req.setUrls(urls);
        CdnClient client = TencentClient.getCdnClient();
        try {
            PushUrlsCacheResponse pushUrlsCacheResponse = client.PushUrlsCache(req);
            log.info("预热缓存结果：{}", PushUrlsCacheRequest.toJsonString(pushUrlsCacheResponse));
            return pushUrlsCacheResponse.getTaskId();
        } catch (TencentCloudSDKException e) {
            log.error("预热缓存失败：{} - {}", e.getErrorCode(), e.getMessage());
            throw new BusinessException("预热缓存失败：" + TencentErrorCodeHandler.getErrorDescription(e));
        }
    }

    @Override
    public String submitCacheRefresh(String[] urls, String type) throws BusinessException {
        CdnClient client = TencentClient.getCdnClient();
        if ("directory".equals(type)) {
            PurgePathCacheRequest req = new PurgePathCacheRequest();
            req.setPaths(urls);
            req.setFlushType("flush");
            try {
                PurgePathCacheResponse updateDomainConfigResponse = client.PurgePathCache(req);
                log.info("刷新缓存结果：{}", PurgePathCacheRequest.toJsonString(updateDomainConfigResponse));
                return updateDomainConfigResponse.getTaskId();
            } catch (TencentCloudSDKException e) {
                log.error("刷新缓存失败：{} - {}", e.getErrorCode(), e.getMessage());
                throw new BusinessException("刷新缓存失败：" + TencentErrorCodeHandler.getErrorDescription(e));
            }
        } else {
            PurgeUrlsCacheRequest req = new PurgeUrlsCacheRequest();
            req.setUrls(urls);
            try {
                PurgeUrlsCacheResponse purgeUrlsCacheResponse = client.PurgeUrlsCache(req);
                log.info("刷新缓存结果：{}", PurgeUrlsCacheRequest.toJsonString(purgeUrlsCacheResponse));
                return purgeUrlsCacheResponse.getTaskId();
            } catch (TencentCloudSDKException e) {
                log.error("刷新缓存失败：{} - {}", e.getErrorCode(), e.getMessage());
                throw new BusinessException("刷新缓存失败：" + TencentErrorCodeHandler.getErrorDescription(e));
            }
        }
    }

    @Override
    public void queryTaskInfo(CacheTaskType cacheTaskType, List<CacheTaskVo> results, Map<Long, SysUser> sysUserMap, CacheTask cacheTask) throws CdnHuaweiException {
        CdnClient client = TencentClient.getCdnClient();
        if ("refresh".equals(cacheTaskType.getCode())) {
            DescribePurgeTasksRequest req = new DescribePurgeTasksRequest();
            req.setTaskId(cacheTask.getTaskId());
            req.setPurgeType("directory".equals(cacheTask.getRefreshType()) ? "path" : "url");
            try {
                DescribePurgeTasksResponse resp = client.DescribePurgeTasks(req);
                log.info("查询刷新缓存任务结果：{}", DescribePurgeTasksRequest.toJsonString(resp));
                for (PurgeTask purgeTask : resp.getPurgeLogs()) {
                    SysUser user = sysUserMap.get(cacheTask.getUserId());
                    CacheTaskVo cacheTaskVo = CacheTaskVo.builder()
                            .taskType(cacheTaskType.getName())
                            .url(purgeTask.getUrl())
                            .fileType("path".equals(purgeTask.getPurgeType()) ? "目录" : "文件")
                            .createTime(purgeTask.getCreateTime())
                            .createTimeLong(convertTime(purgeTask.getCreateTime()))
                            .status(convertStatus(purgeTask.getStatus()))
                            .userId(user.getId())
                            .userName(user.getUserName())
                            .img(user.getImg())
                            .build();
                    results.add(cacheTaskVo);
                }
            } catch (TencentCloudSDKException e) {
                log.error("查询刷新缓存任务失败：{} - {}", e.getErrorCode(), e.getMessage());
                throw new CdnHuaweiException("查询刷新缓存任务失败：" + TencentErrorCodeHandler.getErrorDescription(e));
            }
        } else {
            DescribePushTasksRequest req = new DescribePushTasksRequest();
            req.setTaskId(cacheTask.getTaskId());
            try {
                DescribePushTasksResponse resp = client.DescribePushTasks(req);
                log.info("查询预热缓存任务结果：{}", DescribePushTasksRequest.toJsonString(resp));
                for (PushTask pushTask : resp.getPushLogs()) {
                    SysUser user = sysUserMap.get(cacheTask.getUserId());
                    CacheTaskVo cacheTaskVo = CacheTaskVo.builder()
                            .taskType(cacheTaskType.getName())
                            .url(pushTask.getUrl())
                            .fileType("文件")
                            .createTime(pushTask.getCreateTime())
                            .createTimeLong(convertTime(pushTask.getCreateTime()))
                            .status(convertStatus(pushTask.getStatus()))
                            .userId(user.getId())
                            .userName(user.getUserName())
                            .img(user.getImg())
                            .build();
                    results.add(cacheTaskVo);
                }
            } catch (TencentCloudSDKException e) {
                log.error("查询预热缓存任务失败：{} - {}", e.getErrorCode(), e.getMessage());
                throw new CdnHuaweiException("查询预热缓存任务失败：" + TencentErrorCodeHandler.getErrorDescription(e));
            }
        }
    }

    private Long convertTime(String time) {
        return DateUtil.parse(time).getTime();
    }

    private String convertStatus(String status) {
        // fail：刷新失败 done：刷新成功 process：刷新中
        if ("fail".equals(status)) {
            return "失败";
        } else if ("done".equals(status)) {
            return "完成";
        } else if ("process".equals(status)) {
            return "处理中";
        } else if ("invalid".equals(status)) {
            return "失败";
        } else {
            return "处理中";
        }
    }
}
