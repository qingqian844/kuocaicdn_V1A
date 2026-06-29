package com.kuocai.cdn.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * (SysUserOrder)实体类
 *
 * @author makejava
 * @since 2023-02-28 15:52:18
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SysUserOrderVo {

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
     * 订单名称
     */
    private String orderName;

    /**
     * 订单类型，1表示消费，2表示充值
     */
    private Integer orderType;

    /**
     * 支付类型 1:钱包支付，2:微信支付，3:支付宝支付，4:网银支付
     */
    private Integer payType;

    /**
     * 原始金额
     */
    private BigDecimal originalAmount;

    /**
     * 实际金额
     */
    private BigDecimal actualAmount;

    /**
     * 订单说明
     */
    private String orderComment;

    /**
     * 创建时间
     */
    private Date createTime;
}
