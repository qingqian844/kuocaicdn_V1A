package com.kuocai.cdn.controller.system;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.entity.SysUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

/**
 * 消息中心页面跳转控制器
 *
 * @author XUEW
 * @date 下午9:00 2023/2/12
 */
@Slf4j
@Controller
@Scope(value = "session")
public class MessageCenterPageController extends BaseController {

    /**
     * 我的消息
     */
    @GetMapping("/message")
    public String message(Map<String, Object> map) {
        if (isAdmin()) {
            List<SysUser> sysUsers = sysUserService.queryAll();
            map.put("sysUsers", sysUsers);
            return "admin/message/message";
        }
        return "user/message/message";
    }

    /**
     * 系统公告
     */
    @GetMapping("/announcement")
    public String announcement(Map<String, Object> map) {
        List<SysUser> sysUsers = sysUserService.queryByWrapper(new QueryWrapper<SysUser>().eq("role_id", 1));
        map.put("sysUsers", sysUsers);
        return "admin/message/announcement";
    }

}