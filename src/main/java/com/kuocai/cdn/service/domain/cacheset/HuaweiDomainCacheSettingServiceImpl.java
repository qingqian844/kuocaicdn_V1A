package com.kuocai.cdn.service.domain.cacheset;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.api.huawei.cdn.PreheatingApi;
import com.kuocai.cdn.api.huawei.cdn.dto.PreheatingTaskDTO;
import com.kuocai.cdn.api.huawei.cdn.dto.RefreshTaskDTO;
import com.kuocai.cdn.entity.CacheTask;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.enumeration.CacheTaskType;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.exception.CdnHuaweiException;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.vo.cacheset.CacheTaskVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 华为加速域名统计(CdnDomain)服务实现
 */
@Slf4j
@Service
public class HuaweiDomainCacheSettingServiceImpl extends BaseService<CdnDomain> implements ICdnCacheSettingPlatformService {

    @Override
    public String submitCachePreheating(String[] urls) throws BusinessException {
        PreheatingTaskDTO param = new PreheatingTaskDTO();
        param.setUrls(urls);
        JSONObject preheatingTask = PreheatingApi.createPreheatingTask(param);
        String taskId = preheatingTask.getString("preheating_task");
        return taskId;
    }


    @Override
    public String submitCacheRefresh(String[] urls, String type) throws BusinessException {
        RefreshTaskDTO param = new RefreshTaskDTO();
        param.setUrls(urls);
        param.setType(type);
        JSONObject preheatingTask = PreheatingApi.createRefreshCacheTask(param);
        String taskId = preheatingTask.getString("refresh_task");
        return taskId;
    }

    @Override
    public void queryTaskInfo(CacheTaskType cacheTaskType, List<CacheTaskVo> results, Map<Long, SysUser> sysUserMap, CacheTask cacheTask) throws CdnHuaweiException {
        JSONObject historyTaskDetail = PreheatingApi.getHistoryTaskDetail(cacheTask.getTaskId(), new PreheatingTaskDTO());
        String fileType = "";
        if (ObjectUtil.equal(historyTaskDetail.getString("file_type"), "directory")) {
            fileType = "目录";
        } else if (ObjectUtil.equal(historyTaskDetail.getString("file_type"), "file")) {
            fileType = "文件";
        }else{
            fileType = "文件";
        }
        JSONArray urls = historyTaskDetail.getJSONArray("urls");
        for (Object url : urls) {
            JSONObject urlJsonObject = (JSONObject) url;
            CacheTaskVo cacheTaskVo = new CacheTaskVo();
            cacheTaskVo.setTaskType(cacheTaskType.getName());
            cacheTaskVo.setUrl(urlJsonObject.getString("url"));
            cacheTaskVo.setFileType(fileType);
            cacheTaskVo.setCreateTime(DateUtil.formatDateTime(new Date(urlJsonObject.getLongValue("create_time"))));
            cacheTaskVo.setCreateTimeLong(urlJsonObject.getLongValue("create_time"));
            String status = "";
            if (ObjectUtil.equal(urlJsonObject.getString("status"), "processing")) {
                status = "处理中";
            } else if (ObjectUtil.equal(urlJsonObject.getString("status"), "succeed")) {
                status = "完成";
            } else if (ObjectUtil.equal(urlJsonObject.getString("status"), "failed")) {
                status = "失败";
            } else if (ObjectUtil.equal(urlJsonObject.getString("status"), "waiting")) {
                status = "等待";
            } else if (ObjectUtil.equal(urlJsonObject.getString("status"), "refreshing")) {
                status = "刷新中";
            } else if (ObjectUtil.equal(urlJsonObject.getString("status"), "preheating")) {
                status = "预热中";
            }
            cacheTaskVo.setStatus(status);
            SysUser user = sysUserMap.get(cacheTask.getUserId());
            cacheTaskVo.setUserId(user.getId());
            cacheTaskVo.setUserName(user.getUserName());
            cacheTaskVo.setImg(user.getImg());
            results.add(cacheTaskVo);
        }
    }
}
