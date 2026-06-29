package com.kuocai.cdn.service.domain.cacheset;

import com.kuocai.cdn.entity.CacheTask;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.enumeration.CacheTaskType;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.exception.CdnHuaweiException;
import com.kuocai.cdn.vo.cacheset.CacheTaskVo;

import java.util.List;
import java.util.Map;

public interface ICdnCacheSettingPlatformService {


    /**
     * 提交缓存预热接口
     * @param urls 缓存地址
     * @return 任务ID
     * @throws BusinessException
     */
    String submitCachePreheating(String[] urls) throws BusinessException;


    /**
     *
     * @param urls 缓存地址
     * @param type 缓存类型 file， directory
     * @return 任务ID
     * @throws BusinessException
     */
    String submitCacheRefresh(String[] urls, String type) throws BusinessException;

    void queryTaskInfo(CacheTaskType cacheTaskType, List<CacheTaskVo> results, Map<Long, SysUser> sysUserMap, CacheTask cacheTask) throws CdnHuaweiException;

}
