package com.kuocai.cdn.controller.api.v1;


import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kuocai.cdn.annotation.RateLimiter;
import com.kuocai.cdn.controller.base.ApiV1Controller;
import com.kuocai.cdn.dto.rest.SystemInfo;
import com.kuocai.cdn.entity.WorkOrder;
import com.kuocai.cdn.enumeration.WorkOrderStatus;
import com.kuocai.cdn.service.WorkOrderService;
import com.kuocai.cdn.util.JedisUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "api/v1/system")
public class SystemController extends ApiV1Controller {

    private final WorkOrderService workOrderService;

    SystemController(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    @RateLimiter
    @GetMapping(value = "info")
    public ResponseEntity<SystemInfo> info() {
        SystemInfo systemInfo = new SystemInfo();
        systemInfo.setIsMaster(!isAgent);
        if (isAgent) {
            systemInfo.setTitle(agentConfig.getWebsiteName());
            systemInfo.setIcon(agentConfig.getIcon());
            systemInfo.setKeywords(agentConfig.getWebsiteKeyword());
            systemInfo.setDescription(agentConfig.getWebsiteDescription());
            systemInfo.setIntroduce(agentConfig.getAbout());
            systemInfo.setLogo(agentConfig.getLogoDashboard());
            systemInfo.setLightLogo(agentConfig.getLogo());
        }
        return ResponseEntity.ok(systemInfo);
    }

    @RateLimiter
    @GetMapping(value = "message-count")
    public ResponseEntity<JSONObject> messages() {
        List<String> newMessageIds = JedisUtil.getListString("admin_work_order_new_message");
        JSONObject json = new JSONObject();
        String s = "0";
        if (newMessageIds != null) {
            s = String.valueOf(newMessageIds.size());
        }
        json.put("count", s);
        // 查询未读工单
        QueryWrapper<WorkOrder> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", WorkOrderStatus.WAITING);
        List<WorkOrder> workOrders = workOrderService.queryByWrapper(queryWrapper);
        String c = "0";
        if (null != workOrders) {
            c = String.valueOf(workOrders.size());
        }
        json.put("num", c);
        return ResponseEntity.ok(json);
    }
}
