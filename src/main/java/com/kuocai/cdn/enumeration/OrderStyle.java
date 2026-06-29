package com.kuocai.cdn.enumeration;

/**
 * 订单类型枚举
 *
 * @author xiaobo
 * @date 2023/3/2
 */
public enum OrderStyle {
    // 消费
    CONSUMPTION(1),
    //充值
    RECHARGE(2);

    private final Integer code;

    public Integer getCode() {
        return code;
    }

    OrderStyle(Integer code) {
        this.code = code;
    }
}
