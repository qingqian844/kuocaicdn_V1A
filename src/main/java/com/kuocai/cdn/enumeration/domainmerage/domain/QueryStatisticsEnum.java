package com.kuocai.cdn.enumeration.domainmerage.domain;

import com.kuocai.cdn.enumeration.domainmerage.CdnRoute;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

/**
 * 查询统计信息的时候最大可以传的域名数量
 */
@Getter
@AllArgsConstructor
public enum QueryStatisticsEnum {

    HUAWEI_STATISTICS(CdnRoute.HUAWEI.getCode(), 20),
    VOLCENGINE_STATISTICS(CdnRoute.VOLCENGINE.getCode(), 50),
    HUAWEI_VOLCENGINE_STATISTICS(CdnRoute.HUAWEI_VOLCENGINE.getCode(), 20),
    BAISHAN_STATISTICS(CdnRoute.BAISHAN.getCode(), 10),
    QINIU_STATISTICS(CdnRoute.QINIU.getCode(), 100),
    TENCENT_STATISTICS(CdnRoute.TENCENT.getCode(), 	20),
    TENCENT_EDGEONE_STATISTICS(CdnRoute.TENCENT_EDGEONE.getCode(), 20),
    CDNETWORKS_STATISTICS(CdnRoute.CDNETWORKS.getCode(), 20);

    private final String route;

    private final Integer queryMax;


    /**
     * 转化
     *
     * @param route 路由
     * @return QueryStatisticsEnum
     */
    public static QueryStatisticsEnum convert(String route) {
        return Arrays.stream(QueryStatisticsEnum.values())
                .filter(e -> Objects.equals(e.route, route))
                .findFirst()
                .orElse(QueryStatisticsEnum.HUAWEI_STATISTICS);
    }
}
