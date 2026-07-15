package com.kuocai.cdn.dto;

import lombok.Data;

@Data
public class SelfHostedCacheResultRequest {
    private String taskId;
    private Boolean success;
    private String error;
}
