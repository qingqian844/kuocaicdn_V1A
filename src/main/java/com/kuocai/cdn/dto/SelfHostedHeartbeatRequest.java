package com.kuocai.cdn.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SelfHostedHeartbeatRequest {
    private String agentVersion;
    private Long appliedConfigVersion;
    private BigDecimal cpuUsage;
    private BigDecimal memoryUsage;
    private BigDecimal diskUsage;
    private Long rxBytes;
    private Long txBytes;
    private Long cacheBytes;
    private String lastError;
}
