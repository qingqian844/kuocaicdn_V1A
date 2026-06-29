package com.kuocai.cdn.enumeration;

/**
 * 删除状态枚举
 *
 * @author XUEW
 * @date 下午9:01 2023/2/12
 */
public enum DeleteStatus {

    NOT_DELETE(0),
    DELETED(1);

    /**
     * 删除状态码
     */
    private Integer code;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    private DeleteStatus(Integer code) {
        this.code = code;
    }
}
