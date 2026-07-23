package com.kuocai.cdn.controller.rest;

import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.annotation.AuthorLimiter;
import com.kuocai.cdn.annotation.RateLimiter;
import com.kuocai.cdn.annotation.SysLog;
import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.dto.SelfHostedGroupSaveRequest;
import com.kuocai.cdn.dto.SelfHostedNodeSaveRequest;
import com.kuocai.cdn.dto.resp.RespResult;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.SelfHostedCdnService;
import com.kuocai.cdn.service.SelfHostedNodeInstallService;
import com.kuocai.cdn.service.SelfHostedNodeTelemetryService;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("SelfHostedNode")
@Scope("session")
public class SelfHostedNodeController extends BaseController {
    private final SelfHostedCdnService selfHostedCdnService;
    private final SelfHostedNodeInstallService installService;
    private final SelfHostedNodeTelemetryService telemetryService;

    public SelfHostedNodeController(SelfHostedCdnService selfHostedCdnService,
                                    SelfHostedNodeInstallService installService,
                                    SelfHostedNodeTelemetryService telemetryService) {
        this.selfHostedCdnService = selfHostedCdnService;
        this.installService = installService;
        this.telemetryService = telemetryService;
    }

    @AuthorLimiter
    @GetMapping("list")
    public RespResult list() {
        return RespResult.success("查询成功", selfHostedCdnService.listNodeViews());
    }

    @AuthorLimiter
    @GetMapping("groups")
    public RespResult groups() {
        return RespResult.success("查询成功", selfHostedCdnService.listGroups());
    }

    @AuthorLimiter
    @GetMapping("history")
    public RespResult history(Long id, String range) {
        if (id == null) {
            return RespResult.fail("节点 ID 不能为空");
        }
        try {
            JSONObject data = telemetryService.history(selfHostedCdnService.getNode(id), range);
            data.put("node", selfHostedCdnService.nodeView(id));
            return RespResult.success("查询成功", data);
        } catch (BusinessException e) {
            return RespResult.fail(e.getMessage());
        }
    }

    @AuthorLimiter
    @RateLimiter
    @PostMapping("save")
    @SysLog(module = "系统设置", describe = "保存自建CDN节点")
    public RespResult save(@RequestBody SelfHostedNodeSaveRequest request) {
        try {
            return RespResult.success("节点保存成功", selfHostedCdnService.saveNode(request));
        } catch (BusinessException e) {
            return RespResult.fail(e.getMessage());
        }
    }

    @AuthorLimiter
    @RateLimiter
    @PostMapping("saveGroup")
    @SysLog(module = "系统设置", describe = "保存自建CDN节点组")
    public RespResult saveGroup(@RequestBody SelfHostedGroupSaveRequest request) {
        try {
            return RespResult.success("节点组保存成功", selfHostedCdnService.saveGroup(request));
        } catch (BusinessException e) {
            return RespResult.fail(e.getMessage());
        }
    }

    @AuthorLimiter
    @RateLimiter
    @PostMapping("status")
    @SysLog(module = "系统设置", describe = "修改自建CDN节点状态")
    public RespResult status(Long id, Boolean enabled) {
        try {
            selfHostedCdnService.setNodeEnabled(id, Boolean.TRUE.equals(enabled));
            return RespResult.success("节点状态已更新");
        } catch (BusinessException e) {
            return RespResult.fail(e.getMessage());
        }
    }

    @AuthorLimiter
    @RateLimiter
    @PostMapping("delete")
    @SysLog(module = "系统设置", describe = "删除自建CDN节点")
    public RespResult delete(Long id) {
        try {
            selfHostedCdnService.deleteNode(id);
            return RespResult.success("节点已删除");
        } catch (BusinessException e) {
            return RespResult.fail(e.getMessage());
        }
    }

    @AuthorLimiter
    @RateLimiter
    @PostMapping("syncDns")
    @SysLog(module = "系统设置", describe = "同步自建CDN节点组DNS")
    public RespResult syncDns(Long groupId) {
        try {
            selfHostedCdnService.syncGroupDns(groupId);
            return RespResult.success("节点组 DNS 已同步");
        } catch (BusinessException e) {
            return RespResult.fail(e.getMessage());
        }
    }

    @AuthorLimiter
    @RateLimiter
    @PostMapping("test")
    public RespResult test(Long id) {
        try {
            return RespResult.success(installService.test(id));
        } catch (BusinessException e) {
            return RespResult.fail(e.getMessage());
        }
    }

    @AuthorLimiter
    @RateLimiter
    @PostMapping("install")
    @SysLog(module = "系统设置", describe = "安装自建CDN节点Agent")
    public RespResult install(Long id, HttpServletRequest request) {
        try {
            return RespResult.success(installService.install(id, controlPlaneUrl(request)));
        } catch (BusinessException e) {
            return RespResult.fail(e.getMessage());
        }
    }

    private String controlPlaneUrl(HttpServletRequest request) {
        String scheme = request.getHeader("X-Forwarded-Proto");
        if (scheme == null || scheme.trim().isEmpty()) {
            scheme = request.getScheme();
        }
        String host = request.getHeader("X-Forwarded-Host");
        if (host == null || host.trim().isEmpty()) {
            host = request.getHeader("Host");
        }
        return scheme + "://" + host;
    }
}
