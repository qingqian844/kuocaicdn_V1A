package com.kuocai.cdn.controller.rest;

import cn.hutool.core.util.ObjectUtil;
import com.kuocai.cdn.annotation.RateLimiter;
import com.kuocai.cdn.annotation.SysLog;
import com.kuocai.cdn.component.OssClient;
import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.dto.resp.RespResult;
import com.kuocai.cdn.entity.WorkOrder;
import com.kuocai.cdn.enumeration.WorkOrderStatus;
import com.kuocai.cdn.service.WorkOrderMessageService;
import com.kuocai.cdn.service.WorkOrderService;
import com.kuocai.cdn.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * (WorkOrderMessage)控制器
 *
 * @author ChenWEI
 * @since 2023-02-20 21:12:34
 */
@RestController
@RequestMapping(value = "WorkOrderMessage")
@Scope(value = "session")
public class WorkOrderMessageController extends BaseController {

    @Autowired
    protected WorkOrderMessageService service;

    @Autowired
    private WorkOrderService workOrderService;

    @Autowired
    private OssClient ossClient;

    /**
     * 查询对应工单的聊天信息
     *
     * @param workOrderId 工单主键
     * @return
     */
    @RateLimiter
    @GetMapping("getWorkOrderMsg")
    public RespResult getWorkOrderMessageInfos(Long workOrderId) {
        if (Assert.isEmpty(workOrderId)) {
            return RespResult.fail("输入参数不合法");
        }
        WorkOrder workOrder = workOrderService.queryById(workOrderId);
        RespResult access = checkWorkOrderAccess(workOrder);
        if (access != null) {
            return access;
        }
        return RespResult.success("查询成功", service.getWorkOrderMessageInfos(workOrderId, workOrder));
    }


    /**
     * 发送消息
     *
     * @param workOrderId 工作订单id
     * @param type        类型
     * @param msg         消息
     * @param from        发送者
     * @param fileObj     obj文件
     * @return {@code RespResult}
     */
    @RateLimiter
    @PostMapping("sendMsg")
    @SysLog(module = "工单管理", describe = "工单消息发送")
    public RespResult sendMsg(Long workOrderId, String type, String msg, String from, MultipartFile fileObj) {
        if (Assert.isEmpty(type) || Assert.isEmpty(workOrderId)) {
            return RespResult.fail("输入参数不合法");
        }
        WorkOrder workOrder = workOrderService.queryById(workOrderId);
        RespResult access = checkWorkOrderAccess(workOrder);
        if (access != null) {
            return access;
        }
        if (ObjectUtil.equal(workOrder.getStatus(), WorkOrderStatus.CLOSE.getCode())) {
            return RespResult.fail("当前工单已经结单了！");
        }
        String actualFrom = isAdmin() ? "admin" : "user";
        if (ObjectUtil.equal(type, "img")) {
            String path = null;
            try {
                path = ossClient.upload(fileObj);
            } catch (Exception e) {
                return RespResult.fail("上传文件失败");
            }
            msg = path;
        }
        if (Assert.isEmpty(msg)) {
            return RespResult.fail("输入参数不合法");
        }
        msg = msg.trim();
        if (Assert.isEmpty(msg)) {
            return RespResult.fail("输入参数不合法");
        }
        if (ObjectUtil.notEqual(workOrder.getStatus(), WorkOrderStatus.CLOSE.getCode())) {
            Long creatorId = workOrder.getUserId();
            // 如果是管理员第一次发送消息就把工单状态修改为处理中
            service.sendRedisMsg(workOrderId, type, msg, actualFrom);
            String title = String.format("《%s》工单回复", workOrder.getTitle());
            if (msg.startsWith("http")) {
                msg = "收到一张图片";
            }
            if ("admin".equals(actualFrom)) {
                workOrderService.order2InProcess(workOrderId);
                messageService.sendMessage(loginUserId, creatorId, title, msg);
            } else {
                String finalMsg = msg;
                sysUserService.queryAllAdmins().forEach(a -> messageService.sendMessage(creatorId, a.getId(), title, finalMsg));
            }
            messageService.workOrderMessageRemind(actualFrom, workOrder, msg);
        } else {
            return RespResult.fail("当前工单已经结单了～");
        }
        return RespResult.success("发送成功");
    }
}
