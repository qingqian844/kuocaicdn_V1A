package com.kuocai.cdn.controller.rest;

import cn.hutool.core.collection.ListUtil;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.annotation.RateLimiter;
import com.kuocai.cdn.annotation.SysLog;
import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.dto.datatable.DataTableQuery;
import com.kuocai.cdn.dto.resp.RespResult;
import com.kuocai.cdn.entity.Example;
import com.kuocai.cdn.service.ExampleService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.vo.ExampleVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.*;

/**
 * (Example)控制器
 *
 * @author XUEW
 * @since 2023-05-09 19:27:28
 */
@RestController
@RequestMapping(value = "Example")
@Scope(value = "session")
public class ExampleController extends BaseController {

    @Autowired
    protected ExampleService service;

    /**
     * Datatables查询接口
     *
     * @param query 查询条件
     * @return 查询结果
     */
    @RateLimiter
    @PostMapping("queryForDatatables")
    public RespResult queryForDatatables(@RequestBody DataTableQuery query) {
        JSONObject datatables = null;
        if (isAdmin()) {
            datatables = service.queryForDatatables(null, query);
        } else {
            datatables = service.queryForDatatables(loginUserId, query);
        }
        return RespResult.success("查询成功", datatables);
    }

    /**
     * 根据ID删除
     *
     * @param id ID
     * @return 响应
     */
    @RateLimiter
    @PostMapping("delete")
    @SysLog(module = "示例程序", describe = "根据ID删除信息")
    public RespResult delete(Long id) {
        if (service.deleteById(id) == 1) {
            return RespResult.success("删除成功");
        }
        return RespResult.fail("删除失败");
    }

    /**
     * 根据ID批量删除
     *
     * @param ids ID
     * @return 响应
     */
    @RateLimiter
    @PostMapping("deleteBatch")
    @SysLog(module = "示例程序", describe = "根据ID批量删除信息")
    public RespResult deleteBatch(@RequestParam(value = "ids[]") Long[] ids) {
        if (Assert.isEmpty(ids)) {
            return RespResult.paramEmpty("ID数组");
        }
        if (service.deleteByIds(ListUtil.toList(ids)) >= 1) {
            return RespResult.success("删除成功");
        }
        return RespResult.fail("删除失败");
    }

    /**
     * 保存用户角色
     *
     * @param vo 数据
     * @return 响应
     */
    @RateLimiter
    @PostMapping("save")
    @SysLog(module = "示例程序", describe = "保存或修改信息")
    public RespResult save(ExampleVo vo) {
        String name = vo.getName();
        String cd = vo.getCd();
        String remark = vo.getRemark();
        Long id = vo.getId();
        String status = vo.getStatus();
        if (Assert.isEmpty(name)) {
            return RespResult.paramEmpty("名称");
        }
        if (Assert.isEmpty(cd)) {
            return RespResult.paramEmpty("编码");
        }
        Example example = service.save(Example.builder().id(id).userId(loginUserId).cd(cd).name(name).remark(remark).status(status).build());
        return RespResult.success("保存成功", example);
    }

}
