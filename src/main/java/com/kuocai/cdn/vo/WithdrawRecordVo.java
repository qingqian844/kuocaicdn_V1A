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
 * (WithdrawRecord)实体类
 *
 * @author todoitbo
 * @since 2023-06-17 14:54:09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawRecordVo {

    /**
     * 主键ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 代理用户ID
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
