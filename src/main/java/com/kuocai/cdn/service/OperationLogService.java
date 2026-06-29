package com.kuocai.cdn.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kuocai.cdn.dao.OperationLogDao;
import com.kuocai.cdn.dto.datatable.DataTableQuery;
import com.kuocai.cdn.entity.OperationLog;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.service.base.VoData;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.vo.OperationLogVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 操作日志服务
 *
 * @author XUEW
 * @date 下午9:03 2023/2/12
 */
@Service
public class OperationLogService extends BaseService<OperationLog> implements VoData<OperationLog, OperationLogVo> {

    @Autowired
    protected OperationLogDao dao;

    @Resource
    private SysUserService sysUserService;

    /**
     * 获取用户最近的登录设备
     *
     * @param userId 用户ID
     * @param limit  记录条数
     * @return 登录设备列表
     */
    public List<OperationLog> queryUserLastOperationLog(Long userId, int limit) {
        QueryWrapper<OperationLog> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.orderByDesc("create_time");
        wrapper.last("limit " + limit);
        return queryByWrapper(wrapper);
    }

    /**
     * 获取所有的功能模块
     *
     * @return 功能模块列表
     */
    public List<String> queryAllModules() {
        QueryWrapper<OperationLog> wrapper = new QueryWrapper<>();
        wrapper.select("distinct module");
        List<OperationLog> operationLogs = queryByWrapper(wrapper);
        return operationLogs.stream().map(OperationLog::getModule).collect(Collectors.toList());
    }

    /**
     * 重写Datatable查询接口
     *
     * @param query 查询参数
     * @return 响应
     */
    @Override
    public JSONObject queryForDatatables(Long userId, DataTableQuery query) {
        JSONObject jsonObject = null;
        if (Assert.isEmpty(userId)) {
            jsonObject = super.queryForDatatables(query);
        } else {
            jsonObject = super.queryForDatatables(userId, query);
        }
        JSONArray jsonArray = jsonObject.getJSONArray("data");
        jsonObject.put("data", convert2Vo(jsonArray.toJavaList(OperationLog.class)));
        return jsonObject;
    }

    /**
     * 查询方法，缺少信息
     * 通过vo方式返回前段
     */
    @Override
    public List<OperationLogVo> convert2Vo(List<OperationLog> operationLogs) {
        if (Assert.isEmpty(operationLogs)) {
            return new ArrayList<>();
        }
        List<Long> userIds = operationLogs.stream().map(OperationLog::getUserId).distinct().collect(Collectors.toList());
        List<SysUser> sysUsers = sysUserService.queryByIds(userIds);
        Map<Long, SysUser> sysUserMap = sysUsers.stream().collect(Collectors.toMap(SysUser::getId, u -> u));
        ArrayList<OperationLogVo> operationLogVos = new ArrayList<>();
        for (OperationLog operationLog : operationLogs) {
            String jsonString = JSONObject.toJSONString(operationLog);
            OperationLogVo operationLogVo = JSONObject.parseObject(jsonString, OperationLogVo.class);
            SysUser sysUser = sysUserMap.get(operationLog.getUserId());
            operationLogVo.setImg(sysUser.getImg());
            operationLogVo.setUserName(sysUser.getUserName());
            operationLogVos.add(operationLogVo);
        }
        return operationLogVos;
    }
}
