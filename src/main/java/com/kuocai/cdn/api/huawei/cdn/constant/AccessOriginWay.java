package com.kuocai.cdn.api.huawei.cdn.constant;

/**
 * 回源方式
 */
public class AccessOriginWay {

    /**
     * 回源跟随
     */
    public static final int Origin_FOLLOW = 1;

    /**
     * "http"(默认)
     */
    public static final int HTTP = 2;

    /**
     * "https" 为空值时默认设置为http
     */
    public static final int HTTPS = 3;
}
