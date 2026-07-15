package com.kuocai.cdn.controller.rest;

import com.kuocai.cdn.dto.SelfHostedApplyResultRequest;
import com.kuocai.cdn.dto.SelfHostedHeartbeatRequest;
import com.kuocai.cdn.dto.SelfHostedCacheResultRequest;
import com.kuocai.cdn.dto.resp.RespResult;
import com.kuocai.cdn.entity.SelfHostedNode;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.SelfHostedCdnService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/self-hosted/agent")
public class SelfHostedAgentController {
    private final SelfHostedCdnService selfHostedCdnService;

    public SelfHostedAgentController(SelfHostedCdnService selfHostedCdnService) {
        this.selfHostedCdnService = selfHostedCdnService;
    }

    @PostMapping("heartbeat")
    public RespResult heartbeat(@RequestHeader("X-Kuocai-Node-Id") Long nodeId,
                                @RequestHeader("Authorization") String authorization,
                                @RequestBody SelfHostedHeartbeatRequest request) {
        try {
            SelfHostedNode node = selfHostedCdnService.authenticate(nodeId, authorization);
            return RespResult.success("ok", selfHostedCdnService.heartbeat(node, request));
        } catch (BusinessException e) {
            return RespResult.fail(e.getMessage());
        }
    }

    @GetMapping("config")
    public RespResult config(@RequestHeader("X-Kuocai-Node-Id") Long nodeId,
                             @RequestHeader("Authorization") String authorization) {
        try {
            SelfHostedNode node = selfHostedCdnService.authenticate(nodeId, authorization);
            return RespResult.success("ok", selfHostedCdnService.desiredConfig(node));
        } catch (BusinessException e) {
            return RespResult.fail(e.getMessage());
        }
    }

    @PostMapping("apply-result")
    public RespResult applyResult(@RequestHeader("X-Kuocai-Node-Id") Long nodeId,
                                  @RequestHeader("Authorization") String authorization,
                                  @RequestBody SelfHostedApplyResultRequest request) {
        try {
            SelfHostedNode node = selfHostedCdnService.authenticate(nodeId, authorization);
            selfHostedCdnService.applyResult(node, request);
            return RespResult.success("ok");
        } catch (BusinessException e) {
            return RespResult.fail(e.getMessage());
        }
    }

    @PostMapping("cache-result")
    public RespResult cacheResult(@RequestHeader("X-Kuocai-Node-Id") Long nodeId,
                                  @RequestHeader("Authorization") String authorization,
                                  @RequestBody SelfHostedCacheResultRequest request) {
        try {
            SelfHostedNode node = selfHostedCdnService.authenticate(nodeId, authorization);
            selfHostedCdnService.cacheResult(node, request);
            return RespResult.success("ok");
        } catch (BusinessException e) {
            return RespResult.fail(e.getMessage());
        }
    }
}
