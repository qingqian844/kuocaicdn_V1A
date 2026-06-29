package com.kuocai.cdn.service;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.constant.KuoCaiConstants;
import com.kuocai.cdn.dao.MessageDao;
import com.kuocai.cdn.dto.datatable.DataTableQuery;
import com.kuocai.cdn.entity.Message;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.entity.WorkOrder;
import com.kuocai.cdn.enumeration.MessageStatus;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.service.base.VoData;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.JedisUtil;
import com.kuocai.cdn.vo.MessageVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * (Message)服务
 *
 * @author makejava
 * @since 2023-05-11 15:48:31
 */
@Service
public class MessageService extends BaseService<Message> implements VoData<Message, MessageVo> {

    @Autowired
    protected MessageDao dao;

    @Resource
    private SysUserService sysUserService;

    @Resource
    private WorkOrderService workOrderService;

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
            jsonObject = super.queryForDatatables(userId, query, "receive_user_id");
        }
        JSONArray jsonArray = jsonObject.getJSONArray("data");
        jsonObject.put("data", convert2Vo(jsonArray.toJavaList(Message.class)));
        return jsonObject;
    }

    /**
     * 转为Vo对象
     *
     * @param messages 消息列表
     * @return VO列表
     */
    @Override
    public List<MessageVo> convert2Vo(List<Message> messages) {
        if (Assert.isEmpty(messages)) {
            return new ArrayList<>();
        }
        ArrayList<MessageVo> messageVos = new ArrayList<>();
        List<Long> userIds = messages.stream().flatMap(message -> Stream.of(message.getReceiveUserId(), message.getSendUserId())).distinct().collect(Collectors.toList());
        List<SysUser> sysUsers = sysUserService.queryByIds(userIds);
        Map<Long, SysUser> sysUserMap = sysUsers.stream().collect(Collectors.toMap(SysUser::getId, u -> u));
        for (Message message : messages) {
            String jsonString = JSONObject.toJSONString(message);
            MessageVo messageVo = JSONObject.parseObject(jsonString, MessageVo.class);
            SysUser sendUser = sysUserMap.get(messageVo.getSendUserId());
            SysUser receiveUser = sysUserMap.get(messageVo.getReceiveUserId());
            messageVo.setSendUserName(sendUser.getUserName());
            messageVo.setSendUserImg(sendUser.getImg());
            messageVo.setReceiveUserName(receiveUser.getUserName());
            messageVo.setReceiveUserImg(receiveUser.getImg());
            messageVos.add(messageVo);
        }
        return messageVos;
    }

    /**
     * 获取用户未读消息
     *
     * @param userId 用户ID
     */
    public List<Message> queryUnReadMessages(Long userId) {
        Message queryParam = Message.builder().receiveUserId(userId).status(MessageStatus.UNREAD.getCode()).build();
        // 查询当前用户的全部消息
        return queryByObj(queryParam);
    }

    /**
     * 获取用户未读消息
     *
     * @param userId 用户ID
     */
    public List<MessageVo> queryUnReadMessagesVo(Long userId) {
        if (Assert.isEmpty(userId)) {
            return new ArrayList<>();
        }
        List<Message> messages = queryUnReadMessages(userId);
        if (null == messages) {
            return new ArrayList<>();
        }
        return convert2Vo(messages);
    }

    /**
     * 全部消息已读
     *
     * @param userId 用户ID
     */
    public void readAllMessage(Long userId) {
        // 查询当前用户的全部消息
        List<Message> messages = queryUnReadMessages(userId);
        if (Assert.isEmpty(messages)) {
            return;
        }
        // 修改为全部已读
        List<Long> ids = messages.stream().map(Message::getId).collect(Collectors.toList());
        Message param = Message.builder().status(MessageStatus.READ.getCode()).build();
        updateObjByIds(ids, param);
    }

    /**
     * 将这个ID的消息数据修改为已读状态
     *
     * @param id
     * @return
     */
    public void readMessage(Long id) {
        Message param = Message.builder().id(id).status(MessageStatus.READ.getCode()).build();
        save(param);
    }

    /**
     * 发送消息
     *
     * @param from    来源用户
     * @param target  目标用户
     * @param title   标题
     * @param message 内容
     * @return 消息
     */
    public Message sendMessage(Long from, Long target, String title, String message) {
        Message obj = Message.builder().sendUserId(from).receiveUserId(target).title(title).message(message).status(MessageStatus.UNREAD.getCode()).build();
        return save(obj);
    }

    /**
     * 发送消息
     *
     * @param from    来源用户
     * @param target  目标用户
     * @param message 内容
     * @return 消息
     */
    public Message sendMessage(Long from, Long target, String message) {
        return sendMessage(from, target, "系统消息", message);
    }

    public void workOrderMessageRemind(String from, WorkOrder workOrder, String message) {
        Long target = workOrder.getUserId();
        boolean isToAdmin = false;
        if (Assert.isEmpty(from)) {
            return;
        }
        // 判断是不是管理员发的
        boolean fromIsAdmin = "admin".equals(from);
        if (!fromIsAdmin) {
            isToAdmin = true;
            from = "admin";
        }
        String basicValue = "1";
        String remindKey = StrUtil.join(":", workOrder.getId(), fromIsAdmin ? from : target, fromIsAdmin ? target : from);
        // 这里的key可以优化，key过长会影响性能，但是这样可读性强一点
        Boolean transitionExists = JedisUtil.exists(KuoCaiConstants.WORK_ORDER_MESSAGE_REMIND_TRANSITION_PREFIX + remindKey);
        Boolean exists = JedisUtil.exists(KuoCaiConstants.WORK_ORDER_MESSAGE_REMIND_PREFIX + remindKey);
        if (!transitionExists) {
            // 如果过期，发送消息
            if (!exists) {
                workOrderService.workOrderMessageRemind(workOrder, message, isToAdmin);
            }
            // 这里不管是否过期都重置过期时间
            JedisUtil.setStr(KuoCaiConstants.WORK_ORDER_MESSAGE_REMIND_TRANSITION_PREFIX + remindKey, basicValue, KuoCaiConstants.WORK_ORDER_MESSAGE_REMIND_TRANSITION_EXPIRE);
            JedisUtil.setStr(KuoCaiConstants.WORK_ORDER_MESSAGE_REMIND_PREFIX + remindKey, basicValue, KuoCaiConstants.WORK_ORDER_MESSAGE_REMIND_EXPIRE);
        }
    }

    public void deleteByUserId(Long userId) {
        List<Message> messages = queryByObj(Message.builder().sendUserId(userId).build());
        // 如果没有消息，直接返回
        if (messages.isEmpty()) {
            return;
        }
        messages.addAll(queryByObj(Message.builder().receiveUserId(userId).build()));
        deleteByIds(messages.stream().map(Message::getId).collect(Collectors.toList()));
    }
}
