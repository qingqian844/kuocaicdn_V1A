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
 * (PurchasedFlow)实体类
 *
 * @author makejava
 * @since 2023-04-01 17:01:41
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("purchased_flow")
public class PurchasedFlow implements Serializable {

    private static final long serialVersionUID = 491381190406489882L;

    /**
     * 主键iD
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 用户ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /**
     * 订单ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long transactionOrderId;

    /**
     * 流量包ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long flowPackageId;


    /**
     * 流量包名称
     */
    private String flowPackageName;

    /**
     * 流量包大小（单位B）
     */
    private Long flowPackageSize;

    /**
     * 已使用流量（B）
     */
    private Long usedFlow;

    private Integer edgeoneDomainQuota;

    /**
     * 截止时间
     */
    private Date deadline;

    /**
     * 状态
     */
    private String status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 禁用理由
     */
    private String banedReason;
}
