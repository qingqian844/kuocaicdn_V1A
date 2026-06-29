package com.kuocai.cdn.enumeration;

/**
 * 流量包状态
 */
public enum FlowPackageStatus {
    // 消费
    ENABLE("enable"),
    //充值
    DISABLE("disable");

    private final String code;

    public String getCode() {
        return code;
    }

    FlowPackageStatus(String code) {
        this.code = code;
    }
}
