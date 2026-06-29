package com.kuocai.cdn.api.wangsu.cdn.properties;

import com.kuocai.cdn.api.cdnetworks.cdn.properties.CdnetworksCdn;

public class WangsuCdn extends CdnetworksCdn {
    public static String Project = "";

    public static String AccessKey = "";

    public static String SecretKey = "";

    public static String ContractId = "40017058";

    public static String ItemId = "20";

    public static final String END_POINT = "open.chinanetcenter.com";

    public static final String HTTPS_REQUEST_PREFIX = "https://";

    public static final String HEAD_SIGN_ACCESS_KEY = "x-cnc-accessKey";

    public static final String HEAD_SIGN_TIMESTAMP = "x-cnc-timestamp";

    public static final String HEAD_SIGN_ALGORITHM = "CNC-HMAC-SHA256";

    public static final String X_CNC_AUTH_METHOD = "x-cnc-auth-method";

    public static final String AUTH_METHOD = "AKSK";
}
