package com.kuocai.cdn.api.qiniu.cdn.properties;

import com.kuocai.cdn.util.RuntimeConfigUtils;

public class QiNiuCdn {

    public static String AK = RuntimeConfigUtils.optional("qiniu.cdn.ak", "QINIU_CDN_AK", "");

    public static String SK = RuntimeConfigUtils.optional("qiniu.cdn.sk", "QINIU_CDN_SK", "");

}
