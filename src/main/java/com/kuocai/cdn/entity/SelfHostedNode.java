package com.kuocai.cdn.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("self_hosted_node")
public class SelfHostedNode implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String nodeName;
    private String host;
    private Integer sshPort;
    private String sshUsername;
    @JsonIgnore
    private String sshPasswordCipher;
    private String sshHostKey;
    @JsonIgnore
    private String agentTokenHash;
    private String region;
    private Integer weight;
    private Integer enabled;
    private String status;
    private Date lastHeartbeat;
    private String agentVersion;
    private Long desiredConfigVersion;
    private Long appliedConfigVersion;
    private BigDecimal cpuUsage;
    private BigDecimal memoryUsage;
    private BigDecimal diskUsage;
    private Long rxBytes;
    private Long txBytes;
    private Long rxRateBps;
    private Long txRateBps;
    private Long cacheBytes;
    private String cacheDiskMount;
    private Integer cacheMaxSizeGb;
    private Integer cacheCleanupEnabled;
    private Integer cacheCleanupAgeDays;
    private Integer cacheCleanupMinHits;
    @JsonIgnore
    private String detectedDisksJson;
    private String lastError;
    private String remark;
    private Date createTime;
    private Date updateTime;
}
