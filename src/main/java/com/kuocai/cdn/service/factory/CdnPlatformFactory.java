package com.kuocai.cdn.service.factory;

import com.kuocai.cdn.enumeration.domainmerage.route.CdnOperationRoute;
import com.kuocai.cdn.service.domain.operation.ICdnPlatformService;

public class CdnPlatformFactory {


    /**
     * 获取Cdn平台
     *
     * @param route 线路
     * @return ICdnPlatformService
     */
    public static ICdnPlatformService getCdnPlatform(String route) {
        CdnOperationRoute cdnOperationRoute = CdnOperationRoute.convert(route);
        // 默认返回
        return cdnOperationRoute.getServiceSupplier().get();
    }
}
