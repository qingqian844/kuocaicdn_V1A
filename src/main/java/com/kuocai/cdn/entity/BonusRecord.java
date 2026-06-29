package com.kuocai.cdn.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * (BonusRecord)实体类
 *
 * @author todoitbo
 * @since 2023-06-14 20:03:23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("bonus_record")
public class BonusRecord implements Serializable {

    private static final long serialVersionUID = -13055692126017231L;

    /**
     * 主键ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 订单ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long transactionOrderId;

    /**
     * 下级用户ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /**
     * 代理用户ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long agentUserId;

    /**
     * 订单标题
     */
    private String title;

    /**
     * 订单金额
     */
    private BigDecimal amount;

    /**
     * 分润金额
     */
    private BigDecimal bonus;

    /**
     * 分润类型
     */
    private String bonusType;

    /**
     * 状态：waiting、confirm、cancel
     */
    private String status;

    /**
     * 创建时间
     */
    private Date createTime;
}
