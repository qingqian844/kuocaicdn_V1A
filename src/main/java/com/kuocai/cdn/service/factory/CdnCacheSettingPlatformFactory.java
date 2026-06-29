package com.kuocai.cdn.service.factory;

import com.kuocai.cdn.enumeration.domainmerage.route.CdnCacheSettingRoute;
import com.kuocai.cdn.service.domain.cacheset.ICdnCacheSettingPlatformService;

public class CdnCacheSettingPlatformFactory {


    /**
     * 获取Cdn平台
     *
     * @param route 线路
     * @return ICdnPlatformService
     */
    public static ICdnCacheSettingPlatformService getCdnPlatform(String route) {
        CdnCacheSettingRoute cdnRoute = CdnCacheSettingRoute.convert(route);
        // 默认返回
        return cdnRoute.getServiceSupplier().get();
    }
}
