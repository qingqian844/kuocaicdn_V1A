package com.kuocai.cdn.util;

import cn.hutool.core.collection.ListUtil;
import com.kuocai.cdn.config.SystemConfig;
import com.kuocai.cdn.enumeration.domainmerage.route.CdnOperationRoute;

import java.time.LocalDateTime;

public class MergeRouteUtil {

    public static CdnOperationRoute getCurrentRoute() {
        String huaweiWorkHours = SystemConfig.mergeCdnApiConfig.getHuaweiWorkHours();
        String volcanicWorkHours = SystemConfig.mergeCdnApiConfig.getVolcanicWorkHours();

        String[] huaweiHours = huaweiWorkHours.split(";");
        String[] volcanicHours = volcanicWorkHours.split(";");

        if (ListUtil.toList(huaweiHours).contains(String.valueOf(LocalDateTime.now().getHour()))) {
            return CdnOperationRoute.HUAWEI;
        }
        if (ListUtil.toList(volcanicHours).contains(String.valueOf(LocalDateTime.now().getHour()))) {
            return CdnOperationRoute.VOLCENGINE;
        }else{
            return null;
        }

    }
}
