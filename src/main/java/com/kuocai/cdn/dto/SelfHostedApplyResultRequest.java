package com.kuocai.cdn.dto;

import lombok.Data;

@Data
public class SelfHostedApplyResultRequest {
    private Long version;
    private Boolean success;
    private String error;
}
