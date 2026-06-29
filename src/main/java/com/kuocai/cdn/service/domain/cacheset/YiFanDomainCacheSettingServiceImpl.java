package com.kuocai.cdn.service.domain.cacheset;

import com.kuocai.cdn.entity.CacheTask;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.enumeration.CacheTaskType;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.exception.CdnHuaweiException;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.vo.cacheset.CacheTaskVo;

import java.util.List;
import java.util.Map;


/**
 * 易凡加速域名统计(CdnDomain)服务
 */
public class YiFanDomainCacheSettingServiceImpl extends BaseService<CdnDomain> implements ICdnCacheSettingPlatformService {
    @Override
    public String submitCachePreheating(String[] urls) throws BusinessException {
        return null;
    }

    @Override
    public String submitCacheRefresh(String[] urls, String type) throws BusinessException {
        return null;
    }

    @Override
    public void queryTaskInfo(CacheTaskType cacheTaskType, List<CacheTaskVo> results, Map<Long, SysUser> sysUserMap, CacheTask cacheTask) throws CdnHuaweiException {

    }
}
