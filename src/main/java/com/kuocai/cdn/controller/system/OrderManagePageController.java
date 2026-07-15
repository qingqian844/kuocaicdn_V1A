package com.kuocai.cdn.controller.system;

import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.dto.WorkOrderMessageDTO;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.entity.WorkOrder;
import com.kuocai.cdn.entity.WorkOrderType;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.JedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

/**
 * 工单管理页面跳转控制器
 *
 * @author XUEW
 * @date 下午9:00 2023/2/12
 */
@Slf4j
@Controller
@Scope(value = "session")
public class OrderManagePageController extends BaseController {

    /**
     * 工单列表
     */
    @GetMapping("/order-list")
    public String orderInfo(Map<String, Object> map) {
        if (isAdmin()) {
            return adminOrderInfo(map);
        }
        return userOrderInfo(map);
    }

    public String userOrderInfo(Map<String, Object> map) {
        List<WorkOrderType> orderTypes = workOrderTypeService.queryAll();
        map.put("orderTypes", orderTypes);
        return "user/work-order/order-list";
    }

    public String adminOrderInfo(Map<String, Object> map) {
        List<WorkOrderType> orderTypes = workOrderTypeService.queryAll();
        List<SysUser> sysUsers = sysUserService.queryAll();
        List<String> newMessageIds = JedisUtil.getListString("admin_work_order_new_message");
        map.put("sysUsers", sysUsers);
        map.put("newMessageIds", newMessageIds);
        map.put("orderTypes", orderTypes);
        return "admin/work-order/order-list";
    }

    /**
     * 工单详情
     */
    @GetMapping("/order-detail")
    public String orderInfo(Long workOrderId, Map<String, Object> map) {
        WorkOrder workOrder = workOrderService.queryById(workOrderId);
        if (Assert.isEmpty(workOrder) || !canAccessWorkOrder(workOrder)) {
            return "redirect:/order-list";
        }
        WorkOrderType orderType = workOrderTypeService.queryById(workOrder.getTypeId());
        SysUser orderUser = sysUserService.queryById(workOrder.getUserId());
        List<WorkOrderMessageDTO> workOrderMessageInfos = workOrderMessageService.getWorkOrderMessageInfos(workOrderId, workOrder);
        // 管理员登陆
        if (isAdmin()) {
            SysUser sysUser = sysUserService.queryById(workOrder.getUserId());
            //获取用户的名字信息 头像
            map.put("acceptUserName", sysUser.getUserName());
            map.put("acceptUserImg", sysUser.getImg());
            // 清除新消息
            List<String> newMessageIds = JedisUtil.getListString("admin_work_order_new_message");
            String thisId = String.valueOf(workOrderId);
            if (Assert.notEmpty(newMessageIds) && newMessageIds.contains(thisId)) {
                newMessageIds.remove(thisId);
                JedisUtil.setList("admin_work_order_new_message", newMessageIds);
            }
        } else {
            map.put("acceptUserName", "系统管理员");
            map.put("acceptUserImg", "https://kuocaicdn.com/image/e6a0524d1f09b835f16588f9d67df8ec.png");
        }
        map.put("sendUserName", loginUser.getUserName());
        map.put("sendUserImg", loginUser.getImg());
        map.put("loginUserRoleCode", loginUserRoleCode);
        map.put("workOrder", workOrder);
        map.put("orderType", orderType);
        map.put("orderUser", orderUser);
        map.put("workOrderMsgs", workOrderMessageInfos);
        return "admin/work-order/order-detail";
    }

    /**
     * 工单分类
     */
    @GetMapping("/order-type-list")
    public String orderType(Map<String, Object> map) {
        return "admin/work-order/order-type-list";
    }

    /**
     * 创建工单
     */
    @GetMapping("/order-create")
    public String orderCreate(Map<String, Object> map) {
        List<WorkOrderType> workOrderTypes = workOrderTypeService.queryAll();
        map.put("workOrderTypes", workOrderTypes);
        return "user/work-order/order-create";
    }
}
