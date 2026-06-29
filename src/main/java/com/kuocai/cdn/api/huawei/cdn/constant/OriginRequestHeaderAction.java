package com.kuocai.cdn.api.huawei.cdn.constant;

/**
 * 回源请求头设置类型
 * 同一个请求头字段只允许删除或者设置。
 * 设置：若原始回源请求中不存在该字段，先执行新增再执行设置。
 */
public class OriginRequestHeaderAction {

    /**
     * delete：删除
     */
    public static final String DELETE = "delete";

    /**
     * set：设置
     */
    public static final String SET = "set";
}
