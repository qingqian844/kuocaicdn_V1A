package com.kuocai.cdn.controller.system;

import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.entity.SysUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

/**
 * 日志管理页面跳转控制器
 *
 * @author XUEW
 * @date 下午9:00 2023/2/12
 */
@Slf4j
@Controller
@Scope(value = "session")
public class LogManagePageController extends BaseController {

    /**
     * 我的操作日志
     */
    @GetMapping("/operation-logs")
    public String operationLogs(Map<String, Object> map) {
        List<String> modules = operationLogService.queryAllModules();
        map.put("modules", modules);
        if (isAdmin()) {
            List<SysUser> sysUsers = sysUserService.queryAll();
            map.put("sysUsers", sysUsers);
        }
        return "admin/operation-logs/operation-logs";
    }

}