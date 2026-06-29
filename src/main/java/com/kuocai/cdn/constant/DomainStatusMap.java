package com.kuocai.cdn.constant;

import java.util.HashMap;
import java.util.Map;

/**
 * 域名状态映射
 */
public class DomainStatusMap {
    public static Map<String, String> huawei = new HashMap<>();

    static {
        huawei.put("configuring", "配置中");
        huawei.put("configure_failed", "配置失败");
        huawei.put("online", "已开启");
        huawei.put("deleting", "删除中");
        huawei.put("offline", "已停用");
        huawei.put("checking", "审核中");
        huawei.put("check_failed", "审核未通过");
    }
}
