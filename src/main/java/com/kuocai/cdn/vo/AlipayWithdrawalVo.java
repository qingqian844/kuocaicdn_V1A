package com.kuocai.cdn.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author xiaobo
 * @date 2023/6/12
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlipayWithdrawalVo {

    /**
     * 提现方支付宝账号
     */
    private String alipayAccount;

    /**
     * 提现方名字
     */
    private String nameWithdrawal;

    /**
     * 提现金额
     */
    private double withdrawalAmount;

    /**
     * 用户ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /**
     * 用户名称
     */
    private String userName;

}
