package com.kuocai.cdn.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * (FlowDonate)实体类
 *
 * @author todoitbo
 * @since 2023-05-11 19:43:31
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("flow_donate")
public class FlowDonate implements Serializable {

    private static final long serialVersionUID = 334432475616493920L;

    /**
     * 主键ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 用户ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /**
     * 流量包名称
     */
    private String flowPackageName;

    /**
     * 流量包大小
     */
    private Double flowPackageSize;

    /**
     * 截止时间
     */
    private Date deadline;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 状态：success、withdraw
     */
    private String status;

    /**
     * 赠送记录ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long purchasedFlowId;
}
