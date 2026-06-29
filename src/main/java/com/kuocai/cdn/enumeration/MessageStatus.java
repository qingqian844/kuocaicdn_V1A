package com.kuocai.cdn.enumeration;

public enum MessageStatus {

    READ("read", "已读"),
    UNREAD("unread", "未读");

    /**
     * 用户状态码
     */
    private String code;

    private String name;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private MessageStatus(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
