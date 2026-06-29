package com.kuocai.cdn.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.dao.ExampleDao;
import com.kuocai.cdn.dto.datatable.DataTableQuery;
import com.kuocai.cdn.entity.Example;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.service.base.VoData;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.vo.ExampleVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * (Example)服务
 *
 * @author XUEW
 * @since 2023-05-09 19:27:28
 */
@Service
public class ExampleService extends BaseService<Example> implements VoData<Example, ExampleVo> {

    @Autowired
    protected ExampleDao dao;

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
        jsonObject.put("data", convert2Vo(jsonArray.toJavaList(Example.class)));
        return jsonObject;
    }

    /**
     * 查询方法，补齐信息
     * 通过vo方式返回前端
     */
    @Override
    public List<ExampleVo> convert2Vo(List<Example> examples) {
        if (Assert.isEmpty(examples)) {
            return new ArrayList<>();
        }
        List<Long> userIds = examples.stream().map(Example::getUserId).distinct().collect(Collectors.toList());
        List<SysUser> sysUsers = sysUserService.queryByIds(userIds);
        Map<Long, SysUser> sysUserMap = sysUsers.stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        ArrayList<ExampleVo> ExampleVos = new ArrayList<>();
        for (Example example : examples) {
            String jsonString = JSONObject.toJSONString(example);
            ExampleVo exampleVo = JSONObject.parseObject(jsonString, ExampleVo.class);
            SysUser sysUser = sysUserMap.get(example.getUserId());
            exampleVo.setImg(sysUser.getImg());
            exampleVo.setUserName(sysUser.getUserName());
            ExampleVos.add(exampleVo);
        }
        return ExampleVos;
    }
}
