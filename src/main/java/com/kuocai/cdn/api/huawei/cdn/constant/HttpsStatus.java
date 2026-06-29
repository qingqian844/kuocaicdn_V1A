package com.kuocai.cdn.api.huawei.cdn.constant;

/**
 * HTTPS证书是否启用
 */
public class HttpsStatus {

    /**
     * 0：不启用，此时无需填写证书及私钥参数
     */
    public static final int DISABLE = 0;

    /**
     * 1：启用HTTPS加速并协议跟随回源
     */
    public static final int ENABLE_AND_AGREEMENT_FOLLOW_ORIGIN = 1;

    /**
     * 2：启用HTTPS加速并HTTP回源
     */
    public static final int ENABLE_AND_HTTP_ORIGIN = 2;

    /**
     * 3：启用HTTPS加速并HTTPS回源，首次配置证书需要传递证书及私钥，如已有证书可不用传证书及私钥。
     */
    public static final int ENABLE_AND_HTTPS_ORIGIN = 3;
}
