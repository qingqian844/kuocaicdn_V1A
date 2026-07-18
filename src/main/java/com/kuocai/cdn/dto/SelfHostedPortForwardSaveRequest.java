package com.kuocai.cdn.dto;

import lombok.Data;

@Data
public class SelfHostedPortForwardSaveRequest {
    private Long id;
    private Long userId;
    private String ruleName;
    private String protocol;
    private Integer listenPort;
    private String originHost;
    private Integer originPort;
    private Long nodeGroupId;
    private String status;
    private String remark;
}
