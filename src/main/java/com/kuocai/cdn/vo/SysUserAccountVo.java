package com.kuocai.cdn.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 用户账户(SysUserAccount)实体类
 *
 * @author makejava
 * @since 2023-02-28 15:52:18
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SysUserAccountVo {

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
     * 充值金额
     */
    private BigDecimal rechargeAmount;

    /**
     * 支付类型
     */
    private String payType;
}
