package com.kuocai.cdn.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

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
    private List<SelfHostedDiskInfo> disks;
    private String lastError;
}
