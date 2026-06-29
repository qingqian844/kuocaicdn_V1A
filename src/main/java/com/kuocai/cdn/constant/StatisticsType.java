package com.kuocai.cdn.constant;

import java.util.Arrays;

/**
 * 统计类型
 */
public class StatisticsType {
    public static final String RESOURCE = "Resource";
    public static final String VISITS = "Visits";
    public static final String HTTP_CODE_STATUS = "HttpCodeStatus";
//    public static final String TOP_URI = "TopURI";

    public static final String ALL = "All";

    public static boolean okType(String type) {
        return Arrays.asList(RESOURCE, VISITS, HTTP_CODE_STATUS, ALL).contains(type);
    }
}
