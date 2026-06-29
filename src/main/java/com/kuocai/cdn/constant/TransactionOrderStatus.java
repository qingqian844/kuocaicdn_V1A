package com.kuocai.cdn.constant;

/**
 * @author xiaobo
 * @date 2023/3/10
 */
public class TransactionOrderStatus {

    /**
     * 支付成功
     */
    public static final String TRADE_SUCCESS = "TRADE_SUCCESS";

    /**
     * 支付创建
     */
    public static final String WAIT_BUYER_PAY = "WAIT_BUYER_PAY";

    /**
     * 已过期
     */
    public static final String EXPIRED = "expired";

    /**
     * 退款
     */
    public static final String REFUND = "REFUND";

    /**
     * 退款中
     */
    public static final String REFUNDING = "REFUNDING";
}
