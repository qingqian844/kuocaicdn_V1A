package com.kuocai.cdn.enumeration;

/**
 * 缓存任务类型
 */
public enum CacheTaskType {
    REFRESH("refresh", "缓存刷新"),
    PREHEATING("preheating", "缓存预热");

    /**
     * 实名认证记录状态
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

    CacheTaskType(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
