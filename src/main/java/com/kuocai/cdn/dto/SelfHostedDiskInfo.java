package com.kuocai.cdn.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SelfHostedDiskInfo {
    private String device;
    private String mountPath;
    private String fsType;
    private Long totalBytes;
    private Long availableBytes;
    private BigDecimal usedPercent;
    private Boolean writable;
}
