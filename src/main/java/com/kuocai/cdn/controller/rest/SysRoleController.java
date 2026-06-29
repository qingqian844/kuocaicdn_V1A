package com.kuocai.cdn.controller.rest;

import cn.hutool.core.collection.ListUtil;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.annotation.AuthorLimiter;
import com.kuocai.cdn.annotation.RateLimiter;
import com.kuocai.cdn.annotation.SysLog;
import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.dto.datatable.DataTableQuery;
import com.kuocai.cdn.dto.resp.RespResult;
import com.kuocai.cdn.entity.SysRole;
import com.kuocai.cdn.service.SysRoleService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.vo.SysRoleVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.*;

/**
 * 用户角色(WorkOrderType)控制器
 *
 * @author ChenWEI
 * @since 2023-02-20 21:12:35
 */
@RestController
@RequestMapping(value = "SysRole")
@Scope(value = "session")
public class SysRoleController extends BaseController {

    @Autowired
    protected SysRoleService service;

    /**
     * Datatables查询接口
     *
     * @param query 查询条件
     * @return 查询结果
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("queryForDatatables")
    public RespResult queryForDatatables(@RequestBody DataTableQuery query) {
        JSONObject datatables = service.queryForDatatables(query);
        return RespResult.success("查询成功", datatables);
    }

    /**
     * 根据ID删除
     *
     * @param id 用户角色ID
     * @return 响应
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("delete")
    @SysLog(module = "账户管理", describe = "根据ID删除用户角色信息")
    public RespResult delete(Long id) {
        if (service.deleteById(id) == 1) {
            return RespResult.success("删除成功");
        }
        return RespResult.fail("删除失败");
    }

    /**
     * 根据ID批量删除
     *
     * @param ids 用户角色ID
     * @return 响应
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("deleteBatch")
    @SysLog(module = "账户管理", describe = "根据ID批量删除用户角色信息")
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
     * @param vo 用户角色
     * @return 响应
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("save")
    @SysLog(module = "账户管理", describe = "保存或修改用户角色信息")
    public RespResult save(SysRoleVo vo) {
        String roleName = vo.getRoleName();
        String roleCode = vo.getRoleCode();
        String remark = vo.getRemark();
        if (Assert.isEmpty(roleName)) {
            return RespResult.paramEmpty("角色名称");
        }
        if (Assert.isEmpty(remark)) {
            return RespResult.paramEmpty("描述信息");
        }
        SysRole role = service.save(SysRole.builder().id(vo.getId()).roleCode(roleCode).roleName(roleName).remark(remark).build());
        return RespResult.success("保存成功", role);
    }
}
