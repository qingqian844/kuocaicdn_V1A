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
 * (WithdrawRecord)实体类
 *
 * @author todoitbo
 * @since 2023-06-17 14:54:09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("withdraw_record")
public class WithdrawRecord implements Serializable {

    private static final long serialVersionUID = 167309970811915883L;

    /**
     * 主键ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 代理用户ID
     */
    private Long userId;

    /**
     * 提现金额
     */
    private BigDecimal amount;

    /**
     * 提现类型：alipay、wechat_pay
     */
    private String withdrawType;

    /**
     * 提现方名称
     */
    private String withdrawName;

    /**
     * 提现方账号
     */
    private String withdrawAccount;

    /**
     * 状态：waiting、agree、reject
     */
    private String status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 拒绝理由
     */
    private String rejectReason;
}
