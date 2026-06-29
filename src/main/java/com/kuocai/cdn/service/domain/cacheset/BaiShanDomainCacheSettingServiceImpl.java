package com.kuocai.cdn.service.domain.cacheset;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.api.baishan.cdn.BsDomainCacheApi;
import com.kuocai.cdn.entity.CacheTask;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.enumeration.CacheTaskType;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.exception.CdnHuaweiException;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.vo.cacheset.CacheTaskVo;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * 白山加速域名缓存配置(CdnDomain)服务
 */

@Service
public class BaiShanDomainCacheSettingServiceImpl extends BaseService<CdnDomain> implements ICdnCacheSettingPlatformService {

    /**
     * 设置缓存预取
     *
     * @param urls 缓存地址
     * @return
     * @throws BusinessException
     */
    @Override
    public String submitCachePreheating(String[] urls) throws BusinessException {
        String prefetch = BsDomainCacheApi.prefetch(Arrays.asList(urls));
        return prefetch;
    }

    /**
     * 设置缓存刷新
     *
     * @param urls 缓存地址
     * @param type 缓存类型 file， directory
     * @return
     * @throws BusinessException
     */
    @Override
    public String submitCacheRefresh(String[] urls, String type) throws BusinessException {
        if (ObjectUtil.equal(type, "directory")) {
            return BsDomainCacheApi.refresh(Arrays.asList(urls), "dir");
        } else if (ObjectUtil.equal(type, "file")) {
            return BsDomainCacheApi.refresh(Arrays.asList(urls), "url");
        } else {
            throw new BusinessException("缓存类型错误");
        }
    }

    @Override
    public void queryTaskInfo(CacheTaskType cacheTaskType, List<CacheTaskVo> results, Map<Long, SysUser> sysUserMap, CacheTask cacheTask) throws CdnHuaweiException {
        String taskType = "";
        JSONArray datas = new JSONArray();
        try {
            if (ObjectUtil.equal(cacheTaskType.getCode(), CacheTaskType.REFRESH.getCode())) {
                if (ObjectUtil.equal(cacheTask.getRefreshType(), "file")) {
                    // URL 刷新任务
                    taskType = "refresh_file";
                    datas = BsDomainCacheApi.queryRefresh(cacheTask.getTaskId());
                } else if (ObjectUtil.equal(cacheTask.getRefreshType(), "directory")) {
                    // 目录刷新任务
                    datas = BsDomainCacheApi.queryRefresh(cacheTask.getTaskId());
                }
            } else if (ObjectUtil.equal(cacheTaskType.getCode(), CacheTaskType.PREHEATING.getCode())) {
                taskType = "preload";
                datas = BsDomainCacheApi.queryPrefetch(cacheTask.getTaskId());
            }
        } catch (Exception e) {
            throw new CdnHuaweiException("bs查询缓存历史信息错误:" + e.getMessage());
        }

        for (Object data : datas) {
            String fileType = "";
            JSONObject dataJsonObj = (JSONObject) data;
            CacheTaskVo cacheTaskVo = new CacheTaskVo();
            cacheTaskVo.setTaskType(cacheTaskType.getName());
            cacheTaskVo.setUrl(dataJsonObj.getString("url"));
            if (ObjectUtil.equal(dataJsonObj.getString("type"), "dir")) {
                fileType = "目录";
            } else if (ObjectUtil.equal(dataJsonObj.getString("type"), "url")) {
                fileType = "文件";
            }
            cacheTaskVo.setFileType(fileType);
            cacheTaskVo.setCreateTime(DateUtil.formatDateTime(cacheTask.getCreateTime()));
            cacheTaskVo.setCreateTimeLong(cacheTask.getCreateTime().getTime());
            String status = "";
            if (ObjectUtil.equal(dataJsonObj.getString("status"), "processing")) {
                status = "处理中";
            } else if (ObjectUtil.equal(dataJsonObj.getString("status"), "completed")) {
                status = "完成";
            } else if (ObjectUtil.equal(dataJsonObj.getString("status"), "failed")) {
                status = "失败";
            } else if (ObjectUtil.equal(dataJsonObj.getString("status"), "waiting")) {
                status = "处理中";
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
