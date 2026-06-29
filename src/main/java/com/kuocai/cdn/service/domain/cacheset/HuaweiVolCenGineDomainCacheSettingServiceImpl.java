package com.kuocai.cdn.service.domain.cacheset;

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

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 华为火山云加速域名统计(CdnDomain)服务
 */

@Slf4j
@Service
public class HuaweiVolCenGineDomainCacheSettingServiceImpl extends BaseService<CdnDomain> implements ICdnCacheSettingPlatformService {


    @Resource
    private HuaweiDomainCacheSettingServiceImpl huawei;

    @Resource
    private VolCenGineDomainCacheSettingServiceImpl volCenGine;

    @Override
    public String submitCachePreheating(String[] urls) throws BusinessException {
        volCenGine.submitCachePreheating(urls);
        return huawei.submitCachePreheating(urls);
    }

    @Override
    public String submitCacheRefresh(String[] urls, String type) throws BusinessException {
        volCenGine.submitCacheRefresh(urls, type.equals("directory") ? "dir" : "file");
        return huawei.submitCacheRefresh(urls, type);
    }

    @Override
    public void queryTaskInfo(CacheTaskType cacheTaskType, List<CacheTaskVo> results, Map<Long, SysUser> sysUserMap, CacheTask cacheTask) throws CdnHuaweiException {
        huawei.queryTaskInfo(cacheTaskType, results, sysUserMap, cacheTask);
    }
}
