package com.kuocai.cdn.enumeration;

public enum RealNameAuthenticationStatus {
    SUCCESS("success"),
    WAIT("wait"),
    FAIL("fail");

    /**
     * 实名认证记录状态
     */
    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    RealNameAuthenticationStatus(String code) {
        this.code = code;
    }
}
