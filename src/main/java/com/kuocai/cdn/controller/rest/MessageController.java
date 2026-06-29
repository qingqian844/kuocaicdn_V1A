package com.kuocai.cdn.controller.rest;

import cn.hutool.core.collection.ListUtil;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.annotation.AuthorLimiter;
import com.kuocai.cdn.annotation.RateLimiter;
import com.kuocai.cdn.annotation.SysLog;
import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.dto.datatable.DataTableQuery;
import com.kuocai.cdn.dto.resp.RespResult;
import com.kuocai.cdn.entity.Message;
import com.kuocai.cdn.enumeration.MessageStatus;
import com.kuocai.cdn.service.MessageService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.vo.MessageVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.*;

/**
 * (Message)控制器
 *
 * @author makejava
 * @since 2023-05-11 15:48:32
 */
@RestController
@RequestMapping(value = "Message")
@Scope(value = "session")
public class MessageController extends BaseController {

    @Autowired
    protected MessageService service;

    /**
     * Datatables查询接口
     *
     * @param query 查询条件
     * @return 查询结果
     */
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
        if (Assert.isEmpty(id)) {
            return RespResult.paramEmpty("ID");
        }
        Message message = service.queryById(id);
        RespResult access = checkMessageAccess(message);
        if (access != null) {
            return access;
        }
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
        RespResult access = checkMessageIdsAccess(ListUtil.toList(ids));
        if (access != null) {
            return access;
        }
        if (service.deleteByIds(ListUtil.toList(ids)) >= 1) {
            return RespResult.success("删除成功");
        }
        return RespResult.fail("删除失败");
    }

    /**
     * 当前用户全部消息已读
     *
     * @return 响应
     */
    @RateLimiter
    @PostMapping("readAllMessage")
    @SysLog(module = "消息管理", describe = "当前用户全部已读")
    public RespResult readAllMessage() {
        service.readAllMessage(loginUserId);
        return RespResult.success("已读成功");
    }

    /**
     * 已读一条消息
     *
     * @return 响应
     */
    @RateLimiter
    @PostMapping("readMessage")
    @SysLog(module = "消息管理", describe = "已读一条消息")
    public RespResult readMessage(Long id) {
        if (Assert.isEmpty(id)) {
            return RespResult.paramEmpty("消息ID");
        }
        Message message = service.queryById(id);
        RespResult access = checkMessageAccess(message);
        if (access != null) {
            return access;
        }
        service.readMessage(id);
        return RespResult.success("已读成功");
    }

    /**
     * 站内消息发送
     *
     * @return 响应
     */
    @RateLimiter
    @AuthorLimiter
    @PostMapping("saveMessage")
    @SysLog(module = "消息管理", describe = "管理员发送站内消息")
    public RespResult readMessage(MessageVo messageVo) {
        String title = "管理员站内消息提示";
        String message = messageVo.getMessage();
        Long sendUserId = loginUserId;
        Long receiveUserId = messageVo.getReceiveUserId();
        if (Assert.isEmpty(receiveUserId)) {
            return RespResult.paramEmpty("接收用户ID");
        }
        Message build = Message.builder().message(message).title(title).sendUserId(sendUserId).receiveUserId(receiveUserId).status(MessageStatus.UNREAD.getCode()).build();
        service.save(build);
        return RespResult.success("发送成功！");
    }
}
