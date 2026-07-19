package com.kuocai.cdn.enumeration.domainmerage;

import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Kuocai CDN 线路
 *
 * @author chenwei
 * @date 2023/3/2
 */
@Getter
public enum CdnRoute {
    // 华为路线
    HUAWEI("huawei"),
    // 火山路线
    VOLCENGINE("volcengine"),
    // 华为火山融合路线
    HUAWEI_VOLCENGINE("huawei_volcengine"),
    // 七牛路线
    QINIU("qiniu"),
    // 白山云路线
    BAISHAN("baishan"),
    // 易凡路线
    YIFAN("yifan"),
    // 腾讯路线
    TENCENT("tencent"),
    TENCENT_EDGEONE("tencent_edgeone"),
    // CDNetworks 路线
    CDNETWORKS("cdnetworks"),
    // 阿里云路线
    ALIYUN("aliyun"),
    // 百度路线
    BAIDU("baidu"),
    // 网宿路线
    WANGSU("wangsu"),
    // 金山云路线
    KINGSOFT("kingsoft"),
    MULTI_CDN("multi_cdn"),
    // 旧版自建 CDN 路线，仅用于兼容历史用户和域名。
    SELF_HOSTED("self_hosted"),
    SELF_HOSTED_MAINLAND("self_hosted_mainland"),
    SELF_HOSTED_OVERSEAS("self_hosted_overseas"),
    SELF_HOSTED_GLOBAL("self_hosted_global");

    private static final List<String> SELF_HOSTED_CODES = Collections.unmodifiableList(Arrays.asList(
            SELF_HOSTED.getCode(),
            SELF_HOSTED_MAINLAND.getCode(),
            SELF_HOSTED_OVERSEAS.getCode(),
            SELF_HOSTED_GLOBAL.getCode()
    ));

    private static final List<String> SELF_HOSTED_PRODUCT_CODES = Collections.unmodifiableList(Arrays.asList(
            SELF_HOSTED_MAINLAND.getCode(),
            SELF_HOSTED_OVERSEAS.getCode(),
            SELF_HOSTED_GLOBAL.getCode()
    ));

    private final String code;

    CdnRoute(String code) {
        this.code = code;
    }

    public static boolean isSelfHosted(String route) {
        return route != null && SELF_HOSTED_CODES.contains(route);
    }

    public static boolean isMultiCdn(String route) {
        return MULTI_CDN.getCode().equals(route);
    }

    public static List<String> selfHostedCodes() {
        return SELF_HOSTED_CODES;
    }

    public static List<String> selfHostedProductCodes() {
        return SELF_HOSTED_PRODUCT_CODES;
    }

    public static String selfHostedCoverage(String route) {
        if (SELF_HOSTED_MAINLAND.getCode().equals(route)) {
            return "mainland";
        }
        if (SELF_HOSTED_OVERSEAS.getCode().equals(route)) {
            return "overseas";
        }
        if (SELF_HOSTED_GLOBAL.getCode().equals(route)) {
            return "global";
        }
        return null;
    }

    public static String selfHostedServiceArea(String route) {
        if (SELF_HOSTED_MAINLAND.getCode().equals(route)) {
            return "mainland_china";
        }
        if (SELF_HOSTED_OVERSEAS.getCode().equals(route)) {
            return "outside_mainland_china";
        }
        if (SELF_HOSTED_GLOBAL.getCode().equals(route)) {
            return "global";
        }
        return null;
    }

    public static String selfHostedRouteForServiceArea(String serviceArea) {
        if ("mainland_china".equals(serviceArea)) {
            return SELF_HOSTED_MAINLAND.getCode();
        }
        if ("outside_mainland_china".equals(serviceArea)) {
            return SELF_HOSTED_OVERSEAS.getCode();
        }
        if ("global".equals(serviceArea)) {
            return SELF_HOSTED_GLOBAL.getCode();
        }
        return SELF_HOSTED.getCode();
    }

    public static String selfHostedRouteForCoverage(String coverage) {
        if ("mainland".equals(coverage)) {
            return SELF_HOSTED_MAINLAND.getCode();
        }
        if ("overseas".equals(coverage)) {
            return SELF_HOSTED_OVERSEAS.getCode();
        }
        if ("global".equals(coverage)) {
            return SELF_HOSTED_GLOBAL.getCode();
        }
        return SELF_HOSTED.getCode();
    }

    public static String resolveSelfHostedCreateRoute(String userRoute, String serviceArea) {
        if (SELF_HOSTED_MAINLAND.getCode().equals(userRoute)
                || SELF_HOSTED_OVERSEAS.getCode().equals(userRoute)
                || SELF_HOSTED_GLOBAL.getCode().equals(userRoute)) {
            return userRoute;
        }
        return selfHostedRouteForServiceArea(serviceArea);
    }
}
