package com.kuocai.cdn.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class SelfHostedNodeMetricPoint {
    private Date recordedAt;
    private BigDecimal cpuUsage;
    private BigDecimal memoryUsage;
    private BigDecimal diskUsage;
    private Long rxBytes;
    private Long txBytes;
    private Long rxRateBps;
    private Long txRateBps;
    private Long cacheBytes;
}
