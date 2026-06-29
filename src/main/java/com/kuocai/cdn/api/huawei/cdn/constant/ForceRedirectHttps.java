package com.kuocai.cdn.api.huawei.cdn.constant;

/**
 * 强制跳转HTTPS（0：不强制；1：强制） 为空值时默认设置为关闭。（建议使用force_redirect_config修改配置）
 */
public class ForceRedirectHttps {

    public static final int FORCE = 1;

    public static final int NO_FORCE = 0;
}
