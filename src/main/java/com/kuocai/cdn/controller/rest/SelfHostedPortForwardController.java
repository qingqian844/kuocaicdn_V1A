package com.kuocai.cdn.controller.rest;

import com.kuocai.cdn.annotation.RateLimiter;
import com.kuocai.cdn.annotation.SysLog;
import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.dto.SelfHostedPortForwardSaveRequest;
import com.kuocai.cdn.dto.resp.RespResult;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.SelfHostedPortForwardService;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("SelfHostedPortForward")
@Scope("session")
public class SelfHostedPortForwardController extends BaseController {
    private final SelfHostedPortForwardService portForwardService;

    public SelfHostedPortForwardController(SelfHostedPortForwardService portForwardService) {
        this.portForwardService = portForwardService;
    }

    @GetMapping("list")
    public RespResult list() {
        if (!portForwardService.isAvailable(route, isAdmin())) {
            return RespResult.fail("当前账号未开通自建 CDN");
        }
        return RespResult.success("查询成功", portForwardService.list(loginUserId, isAdmin()));
    }

    @GetMapping("groups")
    public RespResult groups() {
        try {
            return RespResult.success("查询成功", portForwardService.availableGroups(route, isAdmin()));
        } catch (BusinessException e) {
            return RespResult.fail(e.getMessage());
        }
    }

    @RateLimiter
    @PostMapping("save")
    @SysLog(module = "端口转发", describe = "保存自建 CDN 端口转发规则")
    public RespResult save(@RequestBody SelfHostedPortForwardSaveRequest request) {
        try {
            return RespResult.success("端口转发规则保存成功",
                    portForwardService.save(request, loginUserId, route, isAdmin()));
        } catch (BusinessException e) {
            return RespResult.fail(e.getMessage());
        }
    }

    @RateLimiter
    @PostMapping("status")
    @SysLog(module = "端口转发", describe = "修改自建 CDN 端口转发状态")
    public RespResult status(Long id, Boolean enabled) {
        try {
            if (!portForwardService.isAvailable(route, isAdmin())) {
                return RespResult.fail("当前账号未开通自建 CDN");
            }
            portForwardService.setStatus(id, Boolean.TRUE.equals(enabled), loginUserId, isAdmin());
            return RespResult.success("端口转发状态已更新");
        } catch (BusinessException e) {
            return RespResult.fail(e.getMessage());
        }
    }

    @RateLimiter
    @PostMapping("delete")
    @SysLog(module = "端口转发", describe = "删除自建 CDN 端口转发规则")
    public RespResult delete(Long id) {
        try {
            if (!portForwardService.isAvailable(route, isAdmin())) {
                return RespResult.fail("当前账号未开通自建 CDN");
            }
            portForwardService.delete(id, loginUserId, isAdmin());
            return RespResult.success("端口转发规则已删除");
        } catch (BusinessException e) {
            return RespResult.fail(e.getMessage());
        }
    }
}
