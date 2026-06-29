package com.kuocai.cdn.constant;

import java.util.HashMap;
import java.util.Map;

public class FlowPackageChargeType {
    public static final String MONTH = "month";
    public static final String QUARTER = "quarter";
    public static final String YEAR = "year";

    public static Map<String, String> flowPackageChargeTypeNameMap;

    static {
        flowPackageChargeTypeNameMap = new HashMap<>(3);
        flowPackageChargeTypeNameMap.put("month", "月度");
        flowPackageChargeTypeNameMap.put("quarter", "季度");
        flowPackageChargeTypeNameMap.put("year", "年度");
    }
}
