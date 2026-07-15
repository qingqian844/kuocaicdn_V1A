package com.kuocai.cdn.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("self_hosted_cache_job")
public class SelfHostedCacheJob implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String taskId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    private String operation;
    private String targetType;
    private String targetsJson;
    private String status;
    private Integer totalNodes;
    private Integer successNodes;
    private Integer failedNodes;
    private Date createTime;
    private Date updateTime;
}
