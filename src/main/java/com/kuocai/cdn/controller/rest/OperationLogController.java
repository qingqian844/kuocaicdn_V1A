package com.kuocai.cdn.controller.rest;

import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.annotation.RateLimiter;
import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.dto.datatable.DataTableQuery;
import com.kuocai.cdn.dto.resp.RespResult;
import com.kuocai.cdn.entity.OperationLog;
import com.kuocai.cdn.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 操作记录控制器
 *
 * @author XUEW
 * @date 下午9:00 2023/2/12
 */
@RestController
@RequestMapping(value = "OperationLog")
@Scope(value = "session")
public class OperationLogController extends BaseController {

    @Autowired
    private OperationLogService service;

    /**
     * 查询最新5条用户的操作记录
     *
     * @param userId 用户iD
     * @return 响应
     */
    @RateLimiter
    @PostMapping("queryOperationLogs")
    public RespResult queryOperationLogs(Long userId) {
        Long realUserId = loginUserId;
        if (isAdmin() && userId != null) {
            realUserId = userId;
        }
        List<OperationLog> operationLogs = service.queryUserLastOperationLog(realUserId, 5);
        return RespResult.success("查询成功", operationLogs);
    }

    /**
     * 分页查询当前用户操作日志
     *
     * @param query
     * @return
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
}
