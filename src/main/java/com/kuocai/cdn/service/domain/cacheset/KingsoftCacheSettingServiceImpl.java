package com.kuocai.cdn.service.domain.cacheset;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.api.kingsoft.cdn.KingsoftApiService;
import com.kuocai.cdn.entity.CacheTask;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.enumeration.CacheTaskType;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.exception.CdnHuaweiException;
import com.kuocai.cdn.vo.cacheset.CacheTaskVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import com.kuocai.cdn.util.KuocaiDateUtil;
import cn.hutool.core.date.DateUtil;

@Slf4j
@Service
public class KingsoftCacheSettingServiceImpl implements ICdnCacheSettingPlatformService {

    @Autowired
    private KingsoftApiService kingsoftApiService;

    private static final String CDN_API_VERSION = "2016-09-01";

    private BusinessException handleContentApiException(Exception e) {
        if (e instanceof BusinessException) { return (BusinessException) e; }
        String message = e.getMessage();
        if (message != null) {
            if (message.contains("InvalidParameter.TaskIds.NotFound")) return new BusinessException("任务ID不存在或已过期");
            if (message.contains("InvalidUrl.SubceedLimit")) return new BusinessException("URL数量超过限制");
            if (message.contains("InvalidUrl.Malformed")) return new BusinessException("URL格式错误，请检查");
            if (message.contains("AccessDenied")) return new BusinessException("权限不足，请检查访问密钥(AK/SK)");
            if (message.contains("Throttling")) return new BusinessException("API调用频率过高，请稍后再试");
            if (message.contains("InternalFailure")) return new BusinessException("金山云内部错误，请稍后重试或联系客服");
        }
        String errorMsg = e.getMessage() != null ? e.getMessage() : "未知错误";
        if (errorMsg.length() > 100) errorMsg = errorMsg.substring(0, 100) + "...";
        log.error("未映射的金山云内容管理API错误: {}", errorMsg, e);
        return new BusinessException("内容管理操作失败: " + errorMsg);
    }

    @Override
    public String submitCachePreheating(String[] urls) throws BusinessException {
        log.info("[金山云缓存] 提交缓存预热 - URL数量: {}", urls.length);
        try {
            Map<String, Object> body = new HashMap<>();

            List<Map<String, String>> urlObjectList = new ArrayList<>();
            for (String url : urls) {
                Map<String, String> urlMap = new HashMap<>();
                urlMap.put("Url", url);
                urlObjectList.add(urlMap);
            }

            body.put("Urls", urlObjectList);

            String path = "/" + CDN_API_VERSION + "/content/PreloadCaches";

            JSONObject result = kingsoftApiService.postKingsoftApi("PreloadCaches", CDN_API_VERSION, path, body);
            String taskId = result.getString("PreloadTaskId");

            log.info("金山云CDN提交缓存预热成功，任务ID：{}", taskId);
            return taskId;
        } catch (Exception e) {
            log.error("金山云CDN提交缓存预热失败", e);
            throw handleContentApiException(e);
        }
    }
    @Override
    public String submitCacheRefresh(String[] urls, String type) throws BusinessException {
        log.info("金山云CDN提交缓存刷新，URL数量：{}，类型：{}", urls.length, type);
        try {
            Map<String, Object> body = new HashMap<>();

            List<Map<String, String>> urlObjectList = new ArrayList<>();
            for (String url : urls) {
                Map<String, String> urlMap = new HashMap<>();
                urlMap.put("Url", url);
                urlObjectList.add(urlMap);
            }

            if ("file".equals(type)) {
                body.put("Files", urlObjectList);
            } else if ("directory".equals(type)) {
                body.put("Dirs", urlObjectList);
            }

            String path = "/" + CDN_API_VERSION + "/content/RefreshCaches";

            JSONObject result = kingsoftApiService.postKingsoftApi("RefreshCaches", CDN_API_VERSION, path, body);
            String taskId = result.getString("RefreshTaskId");

            log.info("[金山云缓存] 提交缓存刷新成功 - 任务ID: {}", taskId);
            return taskId;
        } catch (Exception e) {
            log.error("[金山云缓存] 提交缓存刷新失败", e);
            throw handleContentApiException(e);
        }
    }

    @Override
    public void queryTaskInfo(CacheTaskType cacheTaskType, List<CacheTaskVo> results, Map<Long, SysUser> sysUserMap, CacheTask cacheTask) throws CdnHuaweiException {
        log.info("金山云CDN查询任务信息，任务类型：{}，任务ID：{}", cacheTaskType, cacheTask.getTaskId());
        if (cacheTask == null || cacheTask.getTaskId() == null) {
            log.warn("查询金山云任务失败：任务或任务ID为空。");
            return;
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("TaskId", cacheTask.getTaskId());

            String path = "/" + CDN_API_VERSION + "/content/GetRefreshOrPreloadTask";
            JSONObject response = kingsoftApiService.postKingsoftApi("GetRefreshOrPreloadTask", CDN_API_VERSION, path, body);

            if (response != null && response.containsKey("Datas")) {
                JSONArray tasksData = response.getJSONArray("Datas");
                if (tasksData == null || tasksData.isEmpty()) {
                    log.warn("金山云CDN未查询到任务信息，任务ID：{}", cacheTask.getTaskId());
                    return;
                }

                SysUser user = sysUserMap.get(cacheTask.getUserId());
                if (user == null) {
                    log.warn("无法找到任务关联的用户，任务ID: {}, 用户ID: {}", cacheTask.getTaskId(), cacheTask.getUserId());
                    return;
                }

                for (int i = 0; i < tasksData.size(); i++) {
                    JSONObject taskData = tasksData.getJSONObject(i);
                    if (taskData == null) {
                        continue; // 跳过空的任务数据
                    }

                    String status = mapKingsoftStatusToSystem(taskData.getString("Status"));
                    String url = taskData.getString("Url");
                    String subType = taskData.getString("SubType");
                    String createTimeString = taskData.getString("CreateTime");

                    if (url == null || createTimeString == null) {
                        log.warn("任务数据不完整，跳过处理。任务详情: {}", taskData.toJSONString());
                        continue;
                    }

                    String fileType = convertSubTypeToName(subType);

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'+0800'");
                    Date createTime = sdf.parse(createTimeString);

                    CacheTaskVo cacheTaskVo = CacheTaskVo.builder()
                            .id(cacheTask.getId())
                            .taskType(cacheTaskType.getName())
                            .url(url)
                            .fileType(fileType)
                            .createTime(KuocaiDateUtil.toDateStr(createTime))
                            .createTimeLong(createTime.getTime())
                            .status(status)
                            .progress("processing".equals(status) ? "50%" : ("success".equals(status) ? "100%" : "0%"))
                            .userId(user.getId())
                            .userName(user.getUserName())
                            .img(user.getImg())
                            .build();
                    results.add(cacheTaskVo);
                }
            }
        } catch (BusinessException e) {
            log.error("查询金山云CDN任务状态失败，任务ID：{}", cacheTask.getTaskId(), e);
            throw new CdnHuaweiException(handleContentApiException(e).getMessage());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private String mapKingsoftStatusToSystem(String kingsoftStatus) {
        if (kingsoftStatus == null) return "failed";
        switch (kingsoftStatus.toLowerCase()) {
            case "successful":
            case "success":
                return "success";
            case "processing":
            case "progressing":
                return "processing";
            case "failed":
                return "failed";
            default:
                return "failed";
        }
    }

    private String convertSubTypeToName(String subType) {
        if (subType == null) {
            return "文件";
        }
        switch (subType.toUpperCase()) {
            case "PRELOAD_FILE":
                return "文件预热";
            case "REFRESH_FILE":
                return "文件刷新";
            case "REFRESH_DIR":
                return "目录刷新";
            default:
                return "文件";
        }
    }

    private String convertRefreshType(String refreshType) {
        return "directory".equals(refreshType) ? "目录" : "文件";
    }
}