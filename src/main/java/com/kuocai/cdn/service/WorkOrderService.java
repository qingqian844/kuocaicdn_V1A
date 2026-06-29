package com.kuocai.cdn.service;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kuocai.cdn.async.SmsAsync;
import com.kuocai.cdn.async.WeixinWebhookAsync;
import com.kuocai.cdn.constant.KuoCaiConstants;
import com.kuocai.cdn.dao.WorkOrderDao;
import com.kuocai.cdn.dto.WorkOrderMessageDTO;
import com.kuocai.cdn.dto.datatable.DataTableQuery;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.entity.WorkOrder;
import com.kuocai.cdn.entity.WorkOrderMessage;
import com.kuocai.cdn.entity.WorkOrderType;
import com.kuocai.cdn.enumeration.WorkOrderStatus;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.service.base.VoData;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.JedisUtil;
import com.kuocai.cdn.vo.WorkOrderMessageRemindVo;
import com.kuocai.cdn.vo.WorkOrderVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * (WorkOrder)服务
 *
 * @author XUEW
 * @since 2023-02-20 21:06:04
 */
@Service
@Slf4j
public class WorkOrderService extends BaseService<WorkOrder> implements VoData<WorkOrder, WorkOrderVo> {

    private final WeixinWebhookAsync weixinWebhookAsync;

    @Resource
    protected WorkOrderDao dao;

    @Resource
    private WorkOrderMessageService workOrderMessageService;

    @Resource
    private SysUserService sysUserService;

    @Resource
    private WorkOrderTypeService orderTypeService;

    @Resource
    private SmsAsync smsAsync;

    WorkOrderService(WeixinWebhookAsync weixinWebhookAsync) {
        this.weixinWebhookAsync = weixinWebhookAsync;
    }


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
            jsonObject = super.queryForDatatables(userId, query);
        }
        JSONArray jsonArray = jsonObject.getJSONArray("data");
        jsonObject.put("data", convert2Vo(jsonArray.toJavaList(WorkOrder.class)));
        return jsonObject;
    }

    /**
     * 查询方法，缺少信息
     * 通过vo方式返回前段
     *
     * @param workOrders
     * @return
     */
    @Override
    public List<WorkOrderVo> convert2Vo(List<WorkOrder> workOrders) {
        if (Assert.isEmpty(workOrders)) {
            return new ArrayList<>();
        }
        List<Long> userIds = workOrders.stream().map(WorkOrder::getUserId).distinct().collect(Collectors.toList());
        List<SysUser> sysUsers = sysUserService.queryByIds(userIds);
        List<WorkOrderType> workOrderTypes = orderTypeService.queryAll();
        Map<Long, SysUser> sysUserMap = sysUsers.stream().collect(Collectors.toMap(SysUser::getId, u -> u));
        Map<Long, WorkOrderType> workOrderTypeMap = workOrderTypes.stream().collect(Collectors.toMap(WorkOrderType::getId, u -> u));
        ArrayList<WorkOrderVo> WorkOrderVos = new ArrayList<>();
        for (WorkOrder workOrder : workOrders) {
            String jsonString = JSONObject.toJSONString(workOrder);
            WorkOrderVo orderVo = JSONObject.parseObject(jsonString, WorkOrderVo.class);
            SysUser sysUser = sysUserMap.get(workOrder.getUserId());
            orderVo.setUserImg(sysUser.getImg());
            orderVo.setUserName(sysUser.getUserName());
            WorkOrderType orderType = workOrderTypeMap.get(workOrder.getTypeId());
            orderVo.setTypeName(orderType.getTypeName());
            WorkOrderVos.add(orderVo);
        }
        return WorkOrderVos;
    }

    /**
     * 查询工单列表信息
     *
     * @param workOrderVo
     * @return
     */
    public IPage<WorkOrder> getWorkOrderInfos(WorkOrderVo workOrderVo) {
        QueryWrapper<WorkOrder> wrapper = new QueryWrapper<WorkOrder>();
        if (Assert.notEmpty(workOrderVo.getStatusPk())) {
            wrapper.eq("status", workOrderVo.getStatusPk());
        }
        if (Assert.notEmpty(workOrderVo.getTypeId())) {
            wrapper.eq("type_id", workOrderVo.getTypeId());
        }
        if (Assert.notEmpty(workOrderVo.getSearch())) {
            wrapper.like("title", workOrderVo.getSearch()).or().like("user_name", workOrderVo.getSearch());
        }
        if (Assert.notEmpty(workOrderVo.getStartTime()) && Assert.notEmpty(workOrderVo.getEndTime())) {
            wrapper.between("create_time", workOrderVo.getStartTime(), workOrderVo.getEndTime());
        }
        Page<WorkOrder> page = new Page<>();
        page.setCurrent(workOrderVo.getStartRecord());
        page.setSize(workOrderVo.getLimitRecord());
        OrderItem orderItem = new OrderItem();
        orderItem.setAsc(workOrderVo.isDateSort());
        orderItem.setColumn("create_time");
        page.setOrders(ListUtil.toList(orderItem));
        return queryByWrapperPage(wrapper, page);
    }

    /**
     * 获取等待处理工单数量
     */
    public Long countWaiting() {
        return countByStatus(WorkOrderStatus.WAITING.getCode());
    }

    /**
     * 停止工作订单
     *
     * @param workOrderId     工作订单id
     * @param evaluationStars 评价明星
     * @param feedback        反馈
     */
    @Transactional(rollbackFor = {Exception.class})
    public Boolean stopWorkOrder(Long workOrderId, String evaluationStars, String feedback) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(workOrderId);
        workOrder.setFeedback(feedback);
        workOrder.setResult(evaluationStars);
        workOrder.setStatus(WorkOrderStatus.CLOSE.getCode());
        save(workOrder);
        // 把Redis数据实例化
        String key = KuoCaiConstants.WORK_ORDER_MESSAGE + ":" + workOrderId;
        List<WorkOrderMessageDTO> msgInfos = JedisUtil.getJsonArray(key, WorkOrderMessageDTO.class);
        String s = JSONArray.toJSONString(msgInfos);
        // 修改气泡
        updateAdminWorkOrderNewMessage(workOrderId, false);
        WorkOrderMessage workOrderMessage = new WorkOrderMessage();
        workOrderMessage.setWorkOrderId(workOrderId);
        workOrderMessage.setContext(s);
//        workOrderMessage.setAdminId();
//        workOrderMessage.setUserId();
        workOrderMessageService.save(workOrderMessage);
        return true;
    }

    /**
     * 验证订单停止工作
     *
     * @param workOrderId 工作订单id
     * @return boolean
     */
    public boolean validateWorkOrderIsStop(Long workOrderId) {
        WorkOrder workOrder = queryById(workOrderId);
        if (Assert.isEmpty(workOrder)) {
            return false;
        }
        //未结单状态可以发送信息
        if (ObjectUtil.notEqual(workOrder.getStatus(), WorkOrderStatus.CLOSE.getCode())) {
            return true;
        } else {
            return false;
        }
    }

    public void order2InProcess(Long workOrderId) {
        WorkOrder workOrder = queryById(workOrderId);
        // 如果是等待处理的状态就把状态修改为处理中
        if (ObjectUtil.equal(workOrder.getStatus(), WorkOrderStatus.WAITING.getCode())) {
            workOrder.setStatus(WorkOrderStatus.IN_PROCESS.getCode());
            save(workOrder);
        }
    }


    /**
     * description: 工单消息提醒
     *
     * @param workOrder        工单实体
     * @param workOrderContent 内容
     * @param isToAdmin        是否是创建工单
     * @throws Exception e
     */
    public void workOrderMessageRemind(WorkOrder workOrder, String workOrderContent, boolean isToAdmin) {
        // 工单不为空
        if (Assert.notEmpty(workOrder)) {
            // 如果是创建工单
            if (isToAdmin) {
                weixinWebhookAsync.sendNewWorkOrderMessage(workOrder, workOrderContent);
//                List<SysUser> adminList = sysUserService.queryByWrapper(new QueryWrapper<SysUser>().eq("role_id", 1));
//                // 管理员列表不为空
//                if (adminList.size() > 0) {
//                    // 将信息发送给管理员
//                    adminList.stream().filter(sysUser -> Assert.notEmpty(sysUser.getEmail())).forEach(sysUser -> {
//                        try {
//                            smsAsync.workOrderMessageRemind(WorkOrderMessageRemindVo.builder()
//                                    .userName(sysUser.getUserName())
//                                    .email(sysUser.getEmail())
//                                    .workOrderTitle(workOrder.getTitle())
//                                    .workOrderContent(Assert.isEmpty(workOrderContent) ? workOrder.getRemark() : workOrderContent)
//                                    .build());
//                        } catch (Exception e) {
//                            log.error("工单消息提醒管理员失败：{}", e.getMessage());
//                        }
//                    });
//                }
            } else {
                Long userId = workOrder.getUserId();
                SysUser sysUser = sysUserService.queryById(userId);
                // 用户且邮箱不为空
                if (Assert.notEmpty(sysUser) && Assert.notEmpty(sysUser.getEmail())) {
                    WorkOrderMessageRemindVo orderMessageRemindVo = WorkOrderMessageRemindVo.builder()
                            .userName(sysUser.getUserName())
                            .email(sysUser.getEmail())
                            .workOrderTitle(workOrder.getTitle())
                            .workOrderContent(Assert.isEmpty(workOrderContent) ? workOrder.getRemark() : workOrderContent)
                            .build();
                    // 发送消息
                    try {
                        smsAsync.workOrderMessageRemind(orderMessageRemindVo);
                    } catch (Exception e) {
                        log.error("工单消息提醒用户失败：{}", e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * 更新管理员工单新消息
     */
    public void updateAdminWorkOrderNewMessage(Long workOrderId, boolean isAdd) {
        List<String> adminNewMessageIds = JedisUtil.getListString("admin_work_order_new_message");
        if (Assert.isEmpty(adminNewMessageIds)) {
            adminNewMessageIds = new ArrayList<>();
        } else {
            String thisId = String.valueOf(workOrderId);
            if (isAdd) {
                if (adminNewMessageIds.contains(thisId)) {
                    return;
                }
                adminNewMessageIds.add(thisId);
            } else {
                adminNewMessageIds.remove(thisId);
            }
        }
        JedisUtil.setList("admin_work_order_new_message", adminNewMessageIds);
    }

    /**
     * 更新管理员工单新消息
     */
    public void updateAdminWorkOrderNewMessage(Long[] workOrderIds, boolean isAdd) {
        List<String> adminNewMessageIds = JedisUtil.getListString("admin_work_order_new_message");
        if (Assert.isEmpty(adminNewMessageIds)) {
            adminNewMessageIds = new ArrayList<>();
        } else {
            if (isAdd) {
                for (Long workOrderId : workOrderIds) {
                    String thisId = String.valueOf(workOrderId);
                    if (adminNewMessageIds.contains(thisId)) {
                        continue;
                    }
                    adminNewMessageIds.add(thisId);
                }
            } else {
                for (Long workOrderId : workOrderIds) {
                    adminNewMessageIds.remove(String.valueOf(workOrderId));
                }
            }
        }
        JedisUtil.setList("admin_work_order_new_message", adminNewMessageIds);
    }
}
