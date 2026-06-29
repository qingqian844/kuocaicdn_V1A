package com.kuocai.cdn.controller.rest;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.annotation.RateLimiter;
import com.kuocai.cdn.annotation.SysLog;
import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.dto.datatable.DataTableQuery;
import com.kuocai.cdn.dto.resp.RespResult;
import com.kuocai.cdn.entity.WorkOrder;
import com.kuocai.cdn.enumeration.WorkOrderStatus;
import com.kuocai.cdn.service.WorkOrderMessageService;
import com.kuocai.cdn.service.WorkOrderService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.vo.WorkOrderVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Date;

/**
 * (WorkOrder)控制器
 *
 * @author XUEW
 * @since 2023-02-20 21:06:04
 */
@Slf4j
@RestController
@RequestMapping(value = "WorkOrder")
@Scope(value = "session")
public class WorkOrderController extends BaseController {

    @Autowired
    protected WorkOrderService service;

    @Resource
    private WorkOrderMessageService msgService;

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
     * 结单接口
     *
     * @param workOrderId 工作订单id
     * @return {@code String}
     */
    @RateLimiter
    @PostMapping("stop")
    @SysLog(module = "工单管理", describe = "工单结单")
    public RespResult stopWorkOrder(Long workOrderId, String evaluationStars, String feedback) {
        if (Assert.isEmpty(workOrderId)) {
            return RespResult.fail("参数异常");
        }
        WorkOrder workOrder = service.queryById(workOrderId);
        RespResult access = checkWorkOrderAccess(workOrder);
        if (access != null) {
            return access;
        }
        service.stopWorkOrder(workOrderId, evaluationStars, feedback);
        return RespResult.success("修改成功");
    }

    @RateLimiter
    @PostMapping("submitWorkOrder")
    @SysLog(module = "工单管理", describe = "提交工单")
    public RespResult submitWorkOrder(WorkOrderVo workOrderVo) {
        if (Assert.isEmpty(workOrderVo)) {
            return RespResult.fail("参数异常");
        }
        WorkOrder workOrder = Convert.convert(WorkOrder.class, workOrderVo);
        workOrder.setStatus(WorkOrderStatus.WAITING.getCode());
        workOrder.setCd(DateUtil.format(DateUtil.date(), "yyyyMMddHHmmssSSS"));
        workOrder.setUserId(loginUserId);
        workOrder.setCreateTime(new Date());
        WorkOrder order = service.save(workOrder);
        if (!Assert.isEmpty(order)) {
            service.workOrderMessageRemind(order, null, true);
        }
        return RespResult.success("提交成功");
    }

    /**
     * 根据ID删除
     *
     * @param id 工单类型ID
     * @return 响应
     */
    @RateLimiter
    @PostMapping("delete")
    @SysLog(module = "工单管理", describe = "根据ID删除工单")
    @Transactional(rollbackFor = {Exception.class})
    public RespResult delete(Long id) {
        if (Assert.isEmpty(id)) {
            return RespResult.paramEmpty("ID");
        }
        WorkOrder workOrder = service.queryById(id);
        RespResult access = checkWorkOrderAccess(workOrder);
        if (access != null) {
            return access;
        }
        if (service.deleteById(id) == 1) {
            msgService.deleteByWorkOrderIds(ListUtil.toList(id));
            workOrderService.updateAdminWorkOrderNewMessage(id, false);
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
    @SysLog(module = "工单管理", describe = "根据ID批量删除工单")
    @Transactional(rollbackFor = {Exception.class})
    public RespResult deleteBatch(@RequestParam(value = "ids[]") Long[] ids) {
        if (Assert.isEmpty(ids)) {
            return RespResult.paramEmpty("ID数组");
        }
        RespResult access = checkWorkOrderIdsAccess(Arrays.asList(ids));
        if (access != null) {
            return access;
        }
        if (service.deleteByIds(ListUtil.toList(ids)) >= 1) {
            msgService.deleteByWorkOrderIds(Arrays.asList(ids));
            workOrderService.updateAdminWorkOrderNewMessage(ids, false);
            return RespResult.success("删除成功");
        }
        return RespResult.fail("删除失败");
    }

}
