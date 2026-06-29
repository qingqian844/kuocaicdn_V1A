package com.kuocai.cdn.service;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kuocai.cdn.dao.CacheTaskDao;
import com.kuocai.cdn.entity.CacheTask;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.enumeration.CacheTaskType;
import com.kuocai.cdn.exception.CdnHuaweiException;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.service.domain.cacheset.ICdnCacheSettingPlatformService;
import com.kuocai.cdn.service.factory.CdnCacheSettingPlatformFactory;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.vo.cacheset.CacheTaskVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


/**
 * (CacheTask)服务
 *
 * @author makejava
 * @since 2023-05-10 18:53:58
 */
@Slf4j
@Service
public class CacheTaskService extends BaseService<CacheTask> {

    @Autowired
    protected CacheTaskDao dao;

    @Resource
    private SysUserService sysUserService;

    /**
     * 新增cachetask信息
     *
     * @param taskId
     * @param taskType
     * @param refreshType
     * @param cdnSupplier
     * @param loginUserId
     * @return
     */
    public CacheTask insertCacheTaskInfo(String taskId, String taskType, String refreshType, String cdnSupplier, Long loginUserId) {
        CacheTask cacheTask = new CacheTask();
        cacheTask.setUserId(loginUserId);
        cacheTask.setTaskType(taskType);
        cacheTask.setCdnSupplier(cdnSupplier);
        cacheTask.setTaskId(taskId);
        cacheTask.setRefreshType(refreshType);
        return save(cacheTask);
    }

    public List<CacheTask> queryByUserId(Long loginUserId) {
        List<CacheTask> cacheTasks = queryByObj(CacheTask.builder().userId(loginUserId).build());
        return cacheTasks;
    }

    /**
     * 根据用户和缓存类型查询数据
     *
     * @param loginUserId
     * @param taskType
     * @return
     */
    public List<CacheTask> queryByUserIdWithType(Long loginUserId, String taskType) {
        QueryWrapper<CacheTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_type", taskType);
        queryWrapper.eq("user_id", loginUserId);
        queryWrapper.between("create_time", LocalDateTime.now().minusDays(15), LocalDateTime.now());
        queryWrapper.orderByDesc("create_time");
//        List<CacheTask> cacheTasks = queryByObj(CacheTask.builder().taskType(taskType).build());
        List<CacheTask> cacheTasks = queryByWrapper(queryWrapper);
//        List<CacheTask> cacheTasks = queryByObj(CacheTask.builder().userId(loginUserId).taskType(taskType).build());
        return cacheTasks;
    }

    /**
     * 根据缓存类型查询
     *
     * @param taskType
     * @return
     */
    public List<CacheTask> queryByTaskType(String taskType) {
        QueryWrapper<CacheTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_type", taskType);
        queryWrapper.between("create_time", LocalDateTime.now().minusDays(15), LocalDateTime.now());
        queryWrapper.orderByDesc("create_time");
//        List<CacheTask> cacheTasks = queryByObj(CacheTask.builder().taskType(taskType).build());
        List<CacheTask> cacheTasks = queryByWrapper(queryWrapper);
        return cacheTasks;
    }

    /**
     * 获取华为的预热刷新历史记录
     *
     * @param cacheTaskType
     * @param loginUserRoleCode
     * @param loginUserId
     * @param userId            管理需要查询的某个用户
     * @return
     * @throws CdnHuaweiException
     */
    public List<JSONObject> queryCdnInfos(CacheTaskType cacheTaskType, String loginUserRoleCode, Long loginUserId, Long userId) throws CdnHuaweiException {
        List<JSONObject> results = new ArrayList<>();
        List<CacheTask> cacheTasks = new ArrayList<>();
        if (ObjectUtil.equal(loginUserRoleCode, "admin") && Assert.isEmpty(userId)) {
            cacheTasks = queryByTaskType(cacheTaskType.getCode());
        } else if (ObjectUtil.equal(loginUserRoleCode, "admin") && Assert.notEmpty(userId)) {
            cacheTasks = queryByUserIdWithType(userId, cacheTaskType.getCode());
        } else if (ObjectUtil.equal(loginUserRoleCode, "user")) {
            cacheTasks = queryByUserIdWithType(loginUserId, cacheTaskType.getCode());
        }
        cacheTasks = filterCacheTasksByCreateTime(cacheTasks, 3);
        // 没有数据直接返回
        if (Assert.isEmpty(cacheTasks)) {
            return results;
        }
        List<Long> userIds = cacheTasks.stream().map(CacheTask::getUserId).distinct().collect(Collectors.toList());
        List<SysUser> sysUsers = sysUserService.queryByIds(userIds);
        Map<Long, SysUser> sysUserMap = sysUsers.stream().collect(Collectors.toMap(SysUser::getId, u -> u));
        List<CacheTaskVo> cacheTaskVos = new ArrayList<>();
        for (CacheTask cacheTask : cacheTasks) {
            ICdnCacheSettingPlatformService platform = CdnCacheSettingPlatformFactory.getCdnPlatform(cacheTask.getCdnSupplier());
            platform.queryTaskInfo(cacheTaskType, cacheTaskVos, sysUserMap, cacheTask);
        }
        results = JSON.parseArray(JSON.toJSONString(cacheTaskVos), JSONObject.class);
        return results;
    }

    private static List<CacheTask> filterCacheTasksByCreateTime(List<CacheTask> cacheTasks, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.DATE, -days);
        long timestamp = calendar.getTimeInMillis();

        return cacheTasks.stream()
                .filter(cacheTask -> cacheTask.getCreateTime().getTime() >= timestamp)
                .collect(Collectors.toList());
    }
}
