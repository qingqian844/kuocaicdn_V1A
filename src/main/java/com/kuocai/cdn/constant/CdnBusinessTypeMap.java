package com.kuocai.cdn.constant;

import java.util.HashMap;
import java.util.Map;

/**
 * 业务类型映射
 */
public class CdnBusinessTypeMap {
    public static Map<String, String> huawei = new HashMap<>();

    static {
        huawei.put("web", "网站加速");
        huawei.put("download", "下载加速");
        huawei.put("video", "点播加速");
    }
}
