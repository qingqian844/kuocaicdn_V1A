package com.kuocai.cdn.controller.rest;

import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.annotation.AuthorLimiter;
import com.kuocai.cdn.annotation.RateLimiter;
import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.dto.datatable.DataTableQuery;
import com.kuocai.cdn.dto.resp.RespResult;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.service.SysUserBannedService;
import com.kuocai.cdn.util.JedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(value = "SysUserBanned")
@Scope(value = "session")
public class SysUserBannedController extends BaseController {

    protected SysUserBannedService service;

    SysUserBannedController(SysUserBannedService service) {
        this.service = service;
    }

    @AuthorLimiter
    @RateLimiter
    @PostMapping("queryForDatatables")
    public RespResult queryForDatatables(@RequestBody DataTableQuery query) {
        JSONObject datatables = sysUserService.queryBannedUserForDatatables(query);
        return RespResult.success("查询成功", datatables);
    }

    @AuthorLimiter
    @RateLimiter
    @PostMapping("banned")
    public RespResult banned(Long userId, String reason) {
        SysUser user = sysUserService.queryById(userId);
        if (user == null) {
            return RespResult.fail("用户不存在");
        }
        if (1L == user.getRoleId()) {
            return RespResult.fail("不能禁用管理员");
        }
        String key = "banned:" + userId;
        service.banned(user, reason);
        // 7 天
        JedisUtil.setStr(key, "1", 7 * 86400);
        return RespResult.success("操作成功");
    }

    @AuthorLimiter
    @RateLimiter
    @PostMapping("unBanned")
    public RespResult unbanned(Long userId) {
//        System.out.println(userId);
        SysUser user = sysUserService.queryById(userId);
        if (user == null) {
            return RespResult.fail("用户不存在");
        }
        String key = "banned:" + userId;
        service.unbanned(user);
        JedisUtil.delKey(key);
        return RespResult.success("操作成功");
    }
}
