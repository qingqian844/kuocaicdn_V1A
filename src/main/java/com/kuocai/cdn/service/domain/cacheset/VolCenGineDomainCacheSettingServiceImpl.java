package com.kuocai.cdn.service.domain.cacheset;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.api.volcengine.cdn.properties.VolcengineCdn;
import com.kuocai.cdn.entity.CacheTask;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.enumeration.CacheTaskType;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.exception.CdnHuaweiException;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.vo.cacheset.CacheTaskVo;
import com.volcengine.model.beans.CDN;
import com.volcengine.service.cdn.CDNService;
import com.volcengine.service.cdn.impl.CDNServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 火山云加速域名统计(CdnDomain)服务
 */
@Slf4j
@Service
public class VolCenGineDomainCacheSettingServiceImpl extends BaseService<CdnDomain> implements ICdnCacheSettingPlatformService {

    @Override
    public String submitCachePreheating(String[] urls) throws BusinessException {
        CDNService service = CDNServiceImpl.getInstance();
        try {
            service.setAccessKey(VolcengineCdn.AK);
            service.setSecretKey(VolcengineCdn.SK);
            CDN.SubmitPreloadTaskRequest req = new CDN.SubmitPreloadTaskRequest()
                    .setUrls(String.join("\n", urls));
            CDN.SubmitPreloadTaskResponse resp = service.submitPreloadTask(req);
            JSONObject response = dealResponse(JSON.toJSONString(resp));
            return response.getString("TaskID");
        } catch (Exception e) {
            throw new BusinessException(e.getMessage());
        } finally {
            service.destroy();
        }
    }

    @Override
    public String submitCacheRefresh(String[] urls, String type) throws BusinessException {
        CDNService service = CDNServiceImpl.getInstance();
        try {
            if (ObjectUtil.equal(type, "directory")) {
                type = "dir";
            }
            service.setAccessKey(VolcengineCdn.AK);
            service.setSecretKey(VolcengineCdn.SK);
            CDN.SubmitRefreshTaskRequest req = new CDN.SubmitRefreshTaskRequest()
                    .setUrls(String.join("\n", urls))
                    .setType(type);
            CDN.SubmitRefreshTaskResponse resp = service.submitRefreshTask(req);
            JSONObject response = dealResponse(JSON.toJSONString(resp));
            return response.getString("TaskID");
        } catch (Exception e) {
            throw new BusinessException(e.getMessage());
        } finally {
            service.destroy();
        }
    }


    @Override
    public void queryTaskInfo(CacheTaskType cacheTaskType, List<CacheTaskVo> results, Map<Long, SysUser> sysUserMap, CacheTask cacheTask) throws CdnHuaweiException {
        CDNService service = CDNServiceImpl.getInstance();
        try {
            String taskType = "";
            if(ObjectUtil.equal(cacheTaskType.getCode(), CacheTaskType.REFRESH.getCode())){
                if(ObjectUtil.equal(cacheTask.getRefreshType(), "file")){
                    // URL 刷新任务
                    taskType = "refresh_file";
                }else if(ObjectUtil.equal(cacheTask.getRefreshType(), "directory")){
                    // 目录刷新任务
                    taskType = "refresh_dir";
                }
            }else if(ObjectUtil.equal(cacheTaskType.getCode(), CacheTaskType.PREHEATING.getCode())){
                taskType = "preload";
            }
            service.setAccessKey(VolcengineCdn.AK);
            service.setSecretKey(VolcengineCdn.SK);
            CDN.DescribeContentTasksRequest req = new CDN.DescribeContentTasksRequest()
                    .setTaskID(cacheTask.getTaskId())
                    .setTaskType(taskType)
                    .setStartTime(DateUtil.lastWeek().getTime() / 1000)
                    .setEndTime(DateUtil.currentSeconds())
                    .setPageSize(100l);
            CDN.DescribeContentTasksResponse resp = service.describeContentTasks(req);
            JSONObject response = dealResponse(JSON.toJSONString(resp));
            JSONArray datas = response.getJSONArray("Data");
            String fileType = "";
            for (Object data : datas) {

                JSONObject dataJsonObj = (JSONObject) data;
                CacheTaskVo cacheTaskVo = new CacheTaskVo();
                cacheTaskVo.setTaskType(cacheTaskType.getName());
                cacheTaskVo.setUrl(dataJsonObj.getString("Url"));
                if (ObjectUtil.equal(dataJsonObj.getString("TaskType"), "refresh_dir")) {
                    fileType = "目录";
                } else if (ObjectUtil.equal(dataJsonObj.getString("TaskType"), "refresh_file")) {
                    fileType = "文件";
                }
                cacheTaskVo.setFileType(fileType);
                cacheTaskVo.setCreateTime(DateUtil.formatDateTime(new Date(dataJsonObj.getLongValue("CreateTime") * 1000)));
                cacheTaskVo.setCreateTimeLong(dataJsonObj.getLongValue("CreateTime"));
                String status = "";
                if (ObjectUtil.equal(dataJsonObj.getString("Status"), "running")) {
                    status = "处理中";
                } else if (ObjectUtil.equal(dataJsonObj.getString("Status"), "complete")) {
                    status = "完成";
                } else if (ObjectUtil.equal(dataJsonObj.getString("Status"), "failed")) {
                    status = "失败";
                }
                cacheTaskVo.setStatus(status);
                SysUser user = sysUserMap.get(cacheTask.getUserId());
                cacheTaskVo.setUserId(user.getId());
                cacheTaskVo.setUserName(user.getUserName());
                cacheTaskVo.setImg(user.getImg());
                results.add(cacheTaskVo);
            }
        } catch (Exception e) {
            throw new CdnHuaweiException(e.getMessage());
        } finally {
            service.destroy();
        }

    }

    public JSONObject dealResponse(String response) throws BusinessException {
        JSONObject responseObject = JSONObject.parseObject(response);
        JSONObject responseMetadata = responseObject.getJSONObject("ResponseMetadata");
        if (responseMetadata.containsKey("Error")) {
            JSONObject error = responseMetadata.getJSONObject("Error");
            throw new BusinessException(error.getString("Message"));
        } else {
            if (responseObject.containsKey("Result")) {
                return responseObject.getJSONObject("Result");
            }
        }
        return null;
    }
}
