package com.kuocai.cdn.enumeration;

/**
 * 工单状态
 */
public enum WorkOrderStatus {

    WAITING("waiting", "等待处理"),
    IN_PROCESS("in_process", "处理中"),

    CLOSE("close", "已关闭");

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

    private WorkOrderStatus(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
