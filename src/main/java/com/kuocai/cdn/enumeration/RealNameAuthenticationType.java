package com.kuocai.cdn.enumeration;

public enum RealNameAuthenticationType {
    PERSON("person"),
    ENTERPRISE("enterprise");

    /**
     * 实名认证用户类型
     */
    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    private RealNameAuthenticationType(String code) {
        this.code = code;
    }
}
