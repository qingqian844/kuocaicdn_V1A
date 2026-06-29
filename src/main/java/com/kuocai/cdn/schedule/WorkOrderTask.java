package com.kuocai.cdn.schedule;

import com.alibaba.fastjson.JSONArray;
import com.kuocai.cdn.constant.KuoCaiConstants;
import com.kuocai.cdn.dto.WorkOrderMessageDTO;
import com.kuocai.cdn.entity.WorkOrder;
import com.kuocai.cdn.entity.WorkOrderMessage;
import com.kuocai.cdn.service.TransactionOrderService;
import com.kuocai.cdn.service.WorkOrderMessageService;
import com.kuocai.cdn.service.WorkOrderService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.JedisUtil;
import com.kuocai.cdn.util.KuocaiDateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * @author XUEW
 * @apiNote
 */
@Slf4j
@Component
@Profile("prod")
public class WorkOrderTask {

    @Resource
    private TransactionOrderService orderService;

    @Resource
    private WorkOrderService workOrderService;

    @Resource
    private WorkOrderMessageService workOrderMessageService;

    /**
     * 工单用户重命名
     */
    @Scheduled(cron = "0 0 */5 ? * *")
    public void rename() {
        log.info("开始重命名工单用户名...");
        orderService.rename();
        log.info("重命名工单用户名完成！");
    }

    /**
     * 自动关闭超时工单
     * 自动关闭超过2天未解决的工单
     */
    @Scheduled(cron = "0 0/10 * * * ?")
    public void autoClose() {
        // 查询所有处理中的订单
        List<WorkOrder> workOrders = workOrderService.queryByObj(WorkOrder.builder().status("in_process").build());
        for (WorkOrder workOrder : workOrders) {
            // 提前拿数据
            String key = KuoCaiConstants.WORK_ORDER_MESSAGE + ":" + workOrder.getId();
            List<WorkOrderMessageDTO> msgInfos = JedisUtil.getJsonArray(key, WorkOrderMessageDTO.class);
            if (Assert.isEmpty(msgInfos)) {
                if (!KuocaiDateUtil.isOverDays(workOrder.getCreateTime(), 7)) {
                    continue;
                }
            } else {
                // 最后一条消息
                WorkOrderMessageDTO workOrderMessageDTO = msgInfos.get(msgInfos.size() - 1);
                Date lastDate = KuocaiDateUtil.toDate(workOrderMessageDTO.getTime());
                if (lastDate == null || !KuocaiDateUtil.isOverDays((lastDate), 7)) {
                    continue;
                }
            }
            log.info("工单【{}】自动关闭", workOrder.getCd());
            workOrder.setStatus("close");
            workOrderService.save(workOrder);
            // 把Redis数据实例化
            if (Assert.isEmpty(msgInfos)) {
                continue;
            }
            String s = JSONArray.toJSONString(msgInfos);
            WorkOrderMessage workOrderMessage = new WorkOrderMessage();
            workOrderMessage.setWorkOrderId(workOrder.getId());
            workOrderMessage.setContext(s);
            workOrderMessageService.save(workOrderMessage);
            JedisUtil.delKey(key);
        }
    }
}
