package com.kuocai.cdn.dto;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
public class SelfHostedNodeSaveRequest {
    private Long id;
    private String nodeName;
    private String host;
    private Integer sshPort;
    private String sshUsername;
    @JSONField(serialize = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ToString.Exclude
    private String sshPassword;
    private String region;
    private Integer weight;
    private Integer enabled;
    private Long groupId;
    private List<Long> groupIds;
    private String cacheDiskMount;
    private Integer cacheMaxSizeGb;
    private Integer cacheCleanupEnabled;
    private Integer cacheCleanupAgeDays;
    private Integer cacheCleanupMinHits;
    private String remark;
}
