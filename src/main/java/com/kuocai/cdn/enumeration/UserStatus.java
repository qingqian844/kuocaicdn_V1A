package com.kuocai.cdn.enumeration;

/**
 * 用户状态枚举
 *
 * @author XUEW
 * @date 下午9:01 2023/2/12
 */
public enum UserStatus {

    BANNED("banned"),
    REGISTER_NOT_CERTIFIED("register_not_certified"),
    CERTIFIED("certified"),
    CANCELLATION("cancellation");

    /**
     * 用户状态码
     */
    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    private UserStatus(String code) {
        this.code = code;
    }
}
