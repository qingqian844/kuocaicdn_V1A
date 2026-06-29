package com.kuocai.cdn.constant;

import java.util.HashMap;
import java.util.Map;

/**
 * 业务类型映射
 */
public class CdnOriginTypeMap {
    public static Map<String, String> huawei = new HashMap<>();

    static {
        huawei.put("ipaddr", "IP源站");
        huawei.put("domain", "域名源站");
    }
}
