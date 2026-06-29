package com.kuocai.cdn.api.huawei.cdn.constant;

/**
 * Referer过滤规则类型
 */
public class RefererType {

    /**
     * 不设置Referer过滤，默认
     */
    public static final int NO_REFERER = 0;

    /**
     * 黑名单
     */
    public static final int BLACKLIST = 1;

    /**
     * 白名单
     */
    public static final int WHITELIST = 2;
}
