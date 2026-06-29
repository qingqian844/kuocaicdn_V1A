package com.kuocai.cdn.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
public class BonusRecordVo {

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
     * 用户名
     */
    private String userName;

    /**
     * 头像
     */
    private String img;

    /**
     * 代理用户ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long agentUserId;

    /**
     * 用户名
     */
    private String agentUserName;

    /**
     * 头像
     */
    private String agentImg;


    /**
     * 订单标题
     */
    private String title;

    /**
     * 订单金额
     */
    private Double amount;

    /**
     * 分润金额
     */
    private Double bonus;

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
