package com.kuocai.cdn.service.domain.cacheset;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.kuocai.cdn.api.qiniu.cdn.QiNiuDomainCacheApi;
import com.kuocai.cdn.api.qiniu.cdn.dto.CacheDto;
import com.kuocai.cdn.entity.CacheTask;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.enumeration.CacheTaskType;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.exception.CdnHuaweiException;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.vo.cacheset.CacheTaskVo;
import com.qiniu.cdn.CdnResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * 易凡加速域名统计(CdnDomain)服务
 */
@Service
public class QiNiuDomainCacheSettingServiceImpl extends BaseService<CdnDomain> implements ICdnCacheSettingPlatformService {

    /**
     * 缓存预热
     * @param urls 缓存地址
     * @return
     * @throws BusinessException
     */
    @Override
    public String submitCachePreheating(String[] urls) throws BusinessException {
        CdnResult.PrefetchResult prefetch = QiNiuDomainCacheApi.prefetch(urls);
        return prefetch.requestId;
    }

    /**
     * 缓存刷新
     * @param urls 缓存地址
     * @param type 缓存类型 file， directory
     * @return
     * @throws BusinessException
     */
    @Override
    public String submitCacheRefresh(String[] urls, String type) throws BusinessException {
        CdnResult.RefreshResult refreshResult;
        if(ObjectUtil.equal(type, "file")){
            refreshResult = QiNiuDomainCacheApi.refreshUrls(urls);
        }else if(ObjectUtil.equal(type, "directory")){
            refreshResult = QiNiuDomainCacheApi.refreshDirs(urls);
        }else{
            throw new BusinessException("提交缓存刷新:缓存类型错误");
        }
        return refreshResult.requestId;
    }

    @Override
    public void queryTaskInfo(CacheTaskType cacheTaskType, List<CacheTaskVo> results, Map<Long, SysUser> sysUserMap, CacheTask cacheTask) throws CdnHuaweiException {
        try {
            String taskType = "";
            List<CacheDto> datas = new ArrayList<>();
            if(ObjectUtil.equal(cacheTaskType.getCode(), CacheTaskType.REFRESH.getCode())){
                datas = QiNiuDomainCacheApi.queryRefresh(cacheTask.getTaskId());
            }else if(ObjectUtil.equal(cacheTaskType.getCode(), CacheTaskType.PREHEATING.getCode())){
                datas = QiNiuDomainCacheApi.queryPrefetch(cacheTask.getTaskId());
            }
            String fileType = "";
            for (CacheDto data : datas) {
                CacheTaskVo cacheTaskVo = new CacheTaskVo();
                cacheTaskVo.setTaskType(cacheTaskType.getName());
                cacheTaskVo.setUrl(data.getUrl());
                if (ObjectUtil.equal(data.getIsDir(), "yes")) {
                    fileType = "目录";
                } else if (ObjectUtil.equal(data.getIsDir(), "no")) {
                    fileType = "文件";
                }
                cacheTaskVo.setFileType(fileType);
                cacheTaskVo.setCreateTime(DateUtil.formatDateTime(data.getCreateAt()));
                cacheTaskVo.setCreateTimeLong(data.getCreateAt().getTime());
                String status = "";
                if (ObjectUtil.equal(data.getState(), "success")) {
                    status = "处理中";
                } else if (ObjectUtil.equal(data.getState(), "processing")) {
                    status = "完成";
                } else if (ObjectUtil.equal(data.getState(), "failure")) {
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
        }
    }
}
