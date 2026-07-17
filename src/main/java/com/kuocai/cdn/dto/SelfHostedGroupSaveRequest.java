package com.kuocai.cdn.dto;

import lombok.Data;

import java.util.List;

@Data
public class SelfHostedGroupSaveRequest {
    private Long id;
    private String groupName;
    private String cnameLabel;
    private String coverage;
    private Integer isDefault;
    private String status;
    private String remark;
    private List<Long> nodeIds;
}
