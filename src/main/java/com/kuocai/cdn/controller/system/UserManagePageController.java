package com.kuocai.cdn.controller.system;

import com.kuocai.cdn.annotation.AuthorLimiter;
import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.entity.SysRole;
import com.kuocai.cdn.entity.SysUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 账户管理页面跳转控制器
 *
 * @author XUEW
 * @date 下午9:00 2023/2/12
 */
@Slf4j
@Controller
@Scope(value = "session")
public class UserManagePageController extends BaseController {

    /**
     * 用户列表
     */
    @AuthorLimiter
    @GetMapping("/user-list")
    public String userList(Map<String, Object> map) {
        return "admin/user/user-list";
    }

    /**
     * 添加用户
     */
    @AuthorLimiter
    @GetMapping("/user-add")
    public String userAdd(Map<String, Object> map) {
        List<SysRole> roles = sysRoleService.queryAll();
        map.put("roles", roles);
        return "admin/user/user-add";
    }

    /**
     * 管理员列表
     */
    @AuthorLimiter
    @GetMapping("/admin-list")
    public String adminList(Map<String, Object> map) {
        return "admin/user/admin-list";
    }

    /**
     * 角色列表
     */
    @AuthorLimiter
    @GetMapping("/role-list")
    public String roleList(Map<String, Object> map) {
        return "admin/user/role-list";
    }

    /**
     * 修改用户
     */
    @AuthorLimiter
    @GetMapping("/user-update")
    public String editUser(Map<String, Object> map, Long id) {
        List<SysRole> roles = sysRoleService.queryAll();
        map.put("roles", roles);
        map.put("consumer", sysUserService.queryById(id));
        return "admin/user/user-update";
    }

    /**
     * 禁用用户列表
     */
    @AuthorLimiter
    @GetMapping("/banned-list")
    public String bannedList(Map<String, Object> map) {
        return "admin/user/banned-list";
    }
}
