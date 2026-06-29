package com.kuocai.cdn.service.factory;

import com.kuocai.cdn.enumeration.domainmerage.route.CdnStatisticsRoute;
import com.kuocai.cdn.service.domain.statistics.ICdnStatisticsPlatformService;

public class CdnStatisticsPlatformFactory {

    /**
     * 获取Cdn平台
     *
     * @param route 线路: huawei volcengine huawei_volcengine yifan tencent
     * @return ICdnPlatformService
     */
    public static ICdnStatisticsPlatformService getCdnPlatform(String route) {
        CdnStatisticsRoute cdnStatisticsRoute = CdnStatisticsRoute.convert(route);
        // 默认返回
        return cdnStatisticsRoute.getServiceSupplier().get();
    }
}
