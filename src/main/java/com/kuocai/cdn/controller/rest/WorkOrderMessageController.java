package com.kuocai.cdn.controller.rest;

import cn.hutool.core.util.ObjectUtil;
import com.kuocai.cdn.annotation.RateLimiter;
import com.kuocai.cdn.annotation.SysLog;
import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.dto.WorkOrderMessageDTO;
import com.kuocai.cdn.dto.resp.RespResult;
import com.kuocai.cdn.entity.WorkOrder;
import com.kuocai.cdn.enumeration.WorkOrderStatus;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.WorkOrderAttachmentService;
import com.kuocai.cdn.service.WorkOrderMessageService;
import com.kuocai.cdn.service.WorkOrderService;
import com.kuocai.cdn.util.Assert;
import io.minio.GetObjectResponse;
import io.minio.StatObjectResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * (WorkOrderMessage)控制器
 *
 * @author ChenWEI
 * @since 2023-02-20 21:12:34
 */
@RestController
@RequestMapping(value = "WorkOrderMessage")
@Scope(value = "session")
@Slf4j
public class WorkOrderMessageController extends BaseController {

    @Autowired
    protected WorkOrderMessageService service;

    @Autowired
    private WorkOrderService workOrderService;

    @Autowired
    private WorkOrderAttachmentService attachmentService;

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
        if (Assert.isEmpty(type) || Assert.isEmpty(workOrderId)
                || !Arrays.asList("text", "img", "file").contains(type)) {
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
        WorkOrderMessageDTO attachment = null;
        if (ObjectUtil.equal(type, "img") || ObjectUtil.equal(type, "file")) {
            try {
                attachment = attachmentService.upload(fileObj);
                attachment.setFrom(actualFrom);
                type = attachment.getType();
                msg = attachment.getMsg();
            } catch (BusinessException e) {
                return RespResult.fail(e.getMessage());
            } catch (Exception e) {
                log.error("Upload work-order attachment failed, workOrderId={}", workOrderId, e);
                return RespResult.fail("上传文件失败");
            }
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
            if (attachment == null) {
                service.sendRedisMsg(workOrderId, type, msg, actualFrom);
            } else {
                service.sendRedisMsg(workOrderId, attachment);
            }
            String title = String.format("《%s》工单回复", workOrder.getTitle());
            String notificationMsg = attachment == null
                    ? msg
                    : ("img".equals(type) ? "收到一张图片" : "收到文件：" + attachment.getFileName());
            if ("admin".equals(actualFrom)) {
                workOrderService.order2InProcess(workOrderId);
                messageService.sendMessage(loginUserId, creatorId, title, notificationMsg);
            } else {
                String finalMsg = notificationMsg;
                sysUserService.queryAllAdmins().forEach(a -> messageService.sendMessage(creatorId, a.getId(), title, finalMsg));
            }
            messageService.workOrderMessageRemind(actualFrom, workOrder, notificationMsg);
        } else {
            return RespResult.fail("当前工单已经结单了～");
        }
        return RespResult.success("发送成功");
    }

    @GetMapping("attachment")
    public void attachment(Long workOrderId, String objectKey, Boolean download,
                           HttpServletResponse servletResponse) throws IOException {
        if (Assert.isEmpty(workOrderId) || Assert.isEmpty(objectKey)) {
            servletResponse.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        WorkOrder workOrder = workOrderService.queryById(workOrderId);
        if (Assert.isEmpty(workOrder)) {
            servletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (!canAccessWorkOrder(workOrder)) {
            servletResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String normalizedObjectKey = attachmentService.extractObjectName(
                WorkOrderMessageDTO.builder().storageKey(objectKey).build());
        WorkOrderMessageDTO attachment = service.findAttachment(workOrderId, workOrder, normalizedObjectKey);
        if (attachment == null) {
            servletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            StatObjectResponse stat = attachmentService.stat(normalizedObjectKey);
            boolean inline = "img".equals(attachment.getType()) && !Boolean.TRUE.equals(download);
            String fileName = attachmentService.displayFileName(attachment);
            String disposition = ContentDisposition.builder(inline ? "inline" : "attachment")
                    .filename(fileName, StandardCharsets.UTF_8)
                    .build()
                    .toString();

            servletResponse.reset();
            servletResponse.setHeader(HttpHeaders.CONTENT_DISPOSITION, disposition);
            servletResponse.setHeader("X-Content-Type-Options", "nosniff");
            servletResponse.setHeader(HttpHeaders.CACHE_CONTROL, "private, max-age=300");
            servletResponse.setContentType(attachmentService.contentType(attachment));
            servletResponse.setContentLengthLong(stat.size());
            try (GetObjectResponse inputStream = attachmentService.open(normalizedObjectKey)) {
                StreamUtils.copy(inputStream, servletResponse.getOutputStream());
            }
        } catch (Exception e) {
            if (!servletResponse.isCommitted()) {
                servletResponse.reset();
                servletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
            log.warn("Read work-order attachment failed, workOrderId={}, objectKey={}",
                    workOrderId, normalizedObjectKey, e);
        }
    }
}
