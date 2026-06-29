package com.kuocai.cdn.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.dao.RealNameAuthenticationDao;
import com.kuocai.cdn.dto.datatable.DataTableQuery;
import com.kuocai.cdn.entity.RealNameAuthentication;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.enumeration.RealNameAuthenticationStatus;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.service.base.VoData;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.vo.RealNameAuthenticationVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * (RealNameAuthentication)服务
 *
 * @author XUEW
 * @since 2023-05-05 14:12:18
 */
@Service
public class RealNameAuthenticationService extends BaseService<RealNameAuthentication> implements VoData<RealNameAuthentication, RealNameAuthenticationVo> {

    @Autowired
    protected RealNameAuthenticationDao dao;

    @Resource
    private SysUserService sysUserService;

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
        jsonObject.put("data", convert2Vo(jsonArray.toJavaList(RealNameAuthentication.class)));
        return jsonObject;
    }

    /**
     * 查询方法，缺少信息
     * 通过vo方式返回前段
     */
    @Override
    public List<RealNameAuthenticationVo> convert2Vo(List<RealNameAuthentication> realNameAuthentications) {
        if (Assert.isEmpty(realNameAuthentications)) {
            return new ArrayList<>();
        }
        List<Long> userIds = realNameAuthentications.stream().map(RealNameAuthentication::getUserId).distinct().collect(Collectors.toList());
        List<SysUser> sysUsers = sysUserService.queryByIds(userIds);
        Map<Long, SysUser> sysUserMap = sysUsers.stream().collect(Collectors.toMap(SysUser::getId, u -> u));
        ArrayList<RealNameAuthenticationVo> realNameAuthenticationVos = new ArrayList<>();
        for (RealNameAuthentication authentication : realNameAuthentications) {
            String jsonString = JSONObject.toJSONString(authentication);
            RealNameAuthenticationVo realNameAuthenticationVo = JSONObject.parseObject(jsonString, RealNameAuthenticationVo.class);
            SysUser sysUser = sysUserMap.get(authentication.getUserId());
            realNameAuthenticationVo.setImg(sysUser.getImg());
            realNameAuthenticationVo.setUserName(sysUser.getUserName());
            realNameAuthenticationVos.add(realNameAuthenticationVo);
        }
        return realNameAuthenticationVos;
    }

    /**
     * 获取等待处理数量
     */
    public Long countWaiting() {
        return countByStatus(RealNameAuthenticationStatus.WAIT.getCode());
    }
}
