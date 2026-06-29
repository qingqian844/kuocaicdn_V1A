package com.kuocai.cdn.service;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.Lists;
import com.kuocai.cdn.constant.KuoCaiConstants;
import com.kuocai.cdn.dao.WorkOrderMessageDao;
import com.kuocai.cdn.dto.WorkOrderMessageDTO;
import com.kuocai.cdn.entity.WorkOrder;
import com.kuocai.cdn.entity.WorkOrderMessage;
import com.kuocai.cdn.enumeration.WorkOrderStatus;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.JedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


/**
 * (WorkOrderMessage)服务
 *
 * @author ChenWEI
 * @since 2023-02-20 21:12:34
 */
@Service
public class WorkOrderMessageService extends BaseService<WorkOrderMessage> {

    @Autowired
    protected WorkOrderMessageDao dao;

    /**
     * 查询工单对应的聊天信息，如果工单不是未处理就从数据库查询，否则就查询Redis
     *
     * @param workOrderId 工单主键
     * @return
     */
    public List<WorkOrderMessageDTO> getWorkOrderMessageInfos(Long workOrderId, WorkOrder workOrder) {
        // 判断是否结单
        if (ObjectUtil.equal(workOrder.getStatus(), WorkOrderStatus.CLOSE.getCode())) {
            WorkOrderMessage orderMessage = queryByOrderId(workOrderId);
            if (Assert.notEmpty(orderMessage)) {
                //将消息题转换成消息格式
                return JSONArray.parseArray(orderMessage.getContext(), WorkOrderMessageDTO.class);
            } else {
                return new ArrayList<>();
            }
        } else {
            // 查询redis
            String key = KuoCaiConstants.WORK_ORDER_MESSAGE + ":" + workOrderId;
            return JedisUtil.getJsonArray(key, WorkOrderMessageDTO.class);
        }
    }

    /**
     * 发送消息存在Redis中
     *
     * @param workOrderId 工作订单id
     * @param type        类型
     * @param msg         消息
     * @param from        发送者
     */
    public void sendRedisMsg(Long workOrderId, String type, String msg, String from) {
        WorkOrderMessageDTO msgInfo = new WorkOrderMessageDTO();
        msgInfo.setMsg(msg);
        msgInfo.setFrom(from);
        msgInfo.setTime(DateUtil.formatDateTime(new DateTime()));
        msgInfo.setType(type);
        String key = KuoCaiConstants.WORK_ORDER_MESSAGE + ":" + workOrderId;
        List<WorkOrderMessageDTO> msgInfos = JedisUtil.getJsonArray(key, WorkOrderMessageDTO.class);
        if (Assert.isEmpty(msgInfos)) {
            List<WorkOrderMessageDTO> tempList = Lists.newArrayList();
            tempList.add(msgInfo);
            JedisUtil.setJsonArray(key, tempList);
        } else {
            msgInfos.add(msgInfo);
            JedisUtil.setJsonArray(key, msgInfos);
        }
        if ("user".equals(from)) {
            List<String> newMessageIds = JedisUtil.getListString("admin_work_order_new_message");
            if (Assert.isEmpty(newMessageIds)) {
                newMessageIds = new ArrayList<>();
            }
            String thisId = String.valueOf(workOrderId);
            if (!newMessageIds.contains(thisId)) {
                newMessageIds.add(thisId);
                JedisUtil.setList("admin_work_order_new_message", newMessageIds);
            }
        }
    }

    /**
     * 通过工单主键查询消息
     *
     * @param orderId 工单主键
     */
    public WorkOrderMessage queryByOrderId(Long orderId) {
        List<WorkOrderMessage> orderMessages = queryByObj(WorkOrderMessage.builder().workOrderId(orderId).build());
        if (Assert.notEmpty(orderMessages)) {
            return orderMessages.get(0);
        }
        return null;
    }

    /**
     * 批量删除工单消息数据
     *
     * @param workOrderIds 工单Ids集合
     * @return 删除行数
     */
    public int deleteByWorkOrderIds(List<Long> workOrderIds) {
        QueryWrapper<WorkOrderMessage> wrapper = new QueryWrapper<>();
        wrapper.in("work_order_id", workOrderIds);
        int row = dao.delete(wrapper);
        List<String> keys = new ArrayList<>();
        for (Long workOrderId : workOrderIds) {
            String key = KuoCaiConstants.WORK_ORDER_MESSAGE + ":" + workOrderId;
            keys.add(key);
        }
        JedisUtil.delKeys(keys);
        return row;
    }
}
