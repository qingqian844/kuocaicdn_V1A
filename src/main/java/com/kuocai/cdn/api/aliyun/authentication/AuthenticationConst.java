package com.kuocai.cdn.api.aliyun.authentication;

import com.kuocai.cdn.util.RuntimeConfigUtils;

public class AuthenticationConst {

    public static final String SERVER_URL = RuntimeConfigUtils.optional(
            "alipay.authentication.server-url",
            "ALIPAY_AUTH_SERVER_URL",
            "https://openapi.alipay.com/gateway.do"
    );

    public static final String APP_ID = RuntimeConfigUtils.required(
            "alipay.authentication.app-id",
            "ALIPAY_AUTH_APP_ID"
    );

    public static final String PRIVATE_KEY = RuntimeConfigUtils.required(
            "alipay.authentication.private-key",
            "ALIPAY_AUTH_PRIVATE_KEY"
    );

    public static final String CHARSET = "UTF8";

    public static final String ALIPAY_PUBLIC_KEY = RuntimeConfigUtils.required(
            "alipay.authentication.public-key",
            "ALIPAY_AUTH_PUBLIC_KEY"
    );

    public static final String SIGN_TYPE = "RSA2";

    public static final String FORMAT = "json";

    public static final String ALIPAY_CERTIFICATION_URL =
            "https://openauth.alipay.com/oauth2/publicAppAuthorize.htm?app_id=" + APP_ID
                    + "&scope=id_verify&redirect_uri="
                    + RuntimeConfigUtils.optional(
                    "alipay.authentication.redirect-uri",
                    "ALIPAY_AUTH_REDIRECT_URI",
                    "https://www.kuocaicdn.com/alipay-authentication-redirect"
            )
                    + "&cert_verify_id=%s";
}
