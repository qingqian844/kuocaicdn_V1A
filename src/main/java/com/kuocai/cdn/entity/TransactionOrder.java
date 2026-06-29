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
 * (TransactionOrder)实体类
 *
 * @author makejava
 * @since 2023-03-10 10:10:55
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("transaction_order")
public class TransactionOrder implements Serializable {

    private static final long serialVersionUID = 961117313037104195L;

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
     * 用户名称
     */
    private String userName;

    /**
     * 订单编号
     */
    private String orderNum;

    /**
     * 订单类型
     */
    private String orderType;

    /**
     * 订单标题
     */
    private String title;

    /**
     * 订单明细
     */
    private String detail;

    /**
     * 金额
     */
    private BigDecimal amount;

    /**
     * 订单状态
     */
    private String status;

    /**
     * 支付方式
     */
    private String payType;

    /**
     * 支付二维码链接
     */
    private String payUrl;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 支付时间
     */
    private Date payTime;

    /**
     * 购买产品ID, 根据OrderType具体表示
     */
    private Long productId;

    /**
     * 创建者
     */
    private Long createBy;

    /**
     * 更新者
     */
    private Long updateBy;
}
