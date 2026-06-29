package com.kuocai.cdn.controller.rest;

import cn.hutool.core.collection.ListUtil;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.annotation.RateLimiter;
import com.kuocai.cdn.annotation.SysLog;
import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.dto.datatable.DataTableQuery;
import com.kuocai.cdn.dto.resp.RespResult;
import com.kuocai.cdn.entity.WorkOrderType;
import com.kuocai.cdn.service.WorkOrderTypeService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.vo.WorkOrderTypeVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.*;

/**
 * 工单类型(WorkOrderType)控制器
 *
 * @author ChenWEI
 * @since 2023-02-20 21:12:35
 */
@RestController
@RequestMapping(value = "WorkOrderType")
@Scope(value = "session")
public class WorkOrderTypeController extends BaseController {

    @Autowired
    protected WorkOrderTypeService service;

    /**
     * Datatables查询接口
     *
     * @param query 查询条件
     * @return 查询结果
     */
    @RateLimiter
    @PostMapping("queryForDatatables")
    public RespResult queryForDatatables(@RequestBody DataTableQuery query) {
        JSONObject datatables = service.queryForDatatables(query);
        return RespResult.success("查询成功", datatables);
    }

    /**
     * 根据ID删除
     *
     * @param id 工单类型ID
     * @return 响应
     */
    @RateLimiter
    @PostMapping("delete")
    @SysLog(module = "工单管理", describe = "根据ID删除工单类型")
    public RespResult delete(Long id) {
        if (service.deleteById(id) == 1) {
            return RespResult.success("删除成功");
        }
        return RespResult.fail("删除失败");
    }

    /**
     * 根据ID批量删除
     *
     * @param ids 工单类型ID
     * @return 响应
     */
    @RateLimiter
    @PostMapping("deleteBatch")
    @SysLog(module = "工单管理", describe = "根据ID批量删除工单类型")
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
     * 保存工单分类
     *
     * @param vo 工单分类
     * @return 响应
     */
    @RateLimiter
    @PostMapping("save")
    @SysLog(module = "工单管理", describe = "保存或者修改工单类型")
    public RespResult save(WorkOrderTypeVo vo) {
        String typeName = vo.getTypeName();
        String remark = vo.getRemark();
        if (Assert.isEmpty(typeName)) {
            return RespResult.paramEmpty("分类名称");
        }
        if (Assert.isEmpty(remark)) {
            return RespResult.paramEmpty("描述信息");
        }
        WorkOrderType orderType = service.save(WorkOrderType.builder().id(vo.getId()).typeName(typeName).remark(remark).build());
        return RespResult.success("保存成功", orderType);
    }
}
