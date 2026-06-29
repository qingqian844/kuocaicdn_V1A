package com.kuocai.cdn.constant;

import java.util.HashMap;
import java.util.Map;

/**
 * 服务范围映射
 */
public class CdnServiceAreaMap {
    public static Map<String, String> huawei = new HashMap<>();

    static {
        huawei.put("mainland_china", "中国大陆");
        huawei.put("outside_mainland_china", "中国境外");
        huawei.put("global", "全球");
    }
}
