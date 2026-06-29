package com.kuocai.cdn.util;

import com.kuocai.cdn.enumeration.domainmerage.CdnRoute;
import com.kuocai.cdn.enumeration.domainmerage.route.CdnOperationRoute;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class SupportedVendorUtils {

    private SupportedVendorUtils() {
    }

    public static List<Map<String, String>> allVendorOptions() {
        return Arrays.stream(CdnOperationRoute.values())
                .map(route -> {
                    Map<String, String> option = new LinkedHashMap<>();
                    option.put("code", route.getRoute());
                    option.put("name", route.getName());
                    return option;
                })
                .collect(Collectors.toList());
    }

    public static List<String> allVendorCodes() {
        return Arrays.stream(CdnOperationRoute.values())
                .map(CdnOperationRoute::getRoute)
                .collect(Collectors.toList());
    }

    public static Map<String, String> vendorNameMap() {
        Map<String, String> names = new LinkedHashMap<>();
        for (CdnOperationRoute route : CdnOperationRoute.values()) {
            names.put(route.getRoute(), route.getName());
        }
        return names;
    }

    public static String defaultVendor() {
        return CdnRoute.KINGSOFT.getCode();
    }
}
