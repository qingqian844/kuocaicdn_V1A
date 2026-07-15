package com.kuocai.cdn.enumeration.domainmerage.route;

import cn.hutool.extra.spring.SpringUtil;
import com.kuocai.cdn.enumeration.domainmerage.CdnRoute;
import com.kuocai.cdn.service.domain.statistics.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 域名统计路线方法
 */
@Getter
@AllArgsConstructor
public enum CdnStatisticsRoute {

    /**
     * 路线：huawei、volcengine、huawei_volcengine、yifan
     */
    HUAWEI(CdnRoute.HUAWEI.getCode(), "华为云", () -> SpringUtil.getBean(HuaweiDomainStatisticsServiceImpl.class)),
    VOLCENGINE(CdnRoute.VOLCENGINE.getCode(), "火山云", () -> SpringUtil.getBean(VolCenGineDomainStatisticsServiceImpl.class)),
    HUAWEI_VOLCENGINE(CdnRoute.HUAWEI_VOLCENGINE.getCode(), "华为火山云融合调度", () -> SpringUtil.getBean(HuaweiVolCenGineDomainStatisticsServiceImpl.class)),
    QINIU(CdnRoute.QINIU.getCode(), "七牛云", () -> SpringUtil.getBean(QiNiuDomainStatisticsServiceImpl.class)),
    BAISHAN(CdnRoute.BAISHAN.getCode(), "白山云", () -> SpringUtil.getBean(BaiShanDomainStatisticsServiceImpl.class)),
    YIFAN(CdnRoute.YIFAN.getCode(), "易凡", () -> SpringUtil.getBean(YiFanDomainStatisticsServiceImpl.class)),
    TENCENT(CdnRoute.TENCENT.getCode(), "腾讯云", () -> SpringUtil.getBean(TencentDomainStatisticsServiceImpl.class)),
    TENCENT_EDGEONE(CdnRoute.TENCENT_EDGEONE.getCode(), "腾讯云 EdgeOne", () -> SpringUtil.getBean(TencentEdgeOneDomainStatisticsServiceImpl.class)),
    CDNETWORKS(CdnRoute.CDNETWORKS.getCode(), "cdnetworks", () -> SpringUtil.getBean(CdnetworksDomainStatisticsServiceImpl.class)),
    ALIYUN(CdnRoute.ALIYUN.getCode(), "阿里云", () -> SpringUtil.getBean(AliyunDomainStatisticsServiceImpl.class)),
    BAIDU(CdnRoute.BAIDU.getCode(), "百度云", () -> SpringUtil.getBean(BaiduDomainStatisticsServiceImpl.class)),
    KINGSOFT(CdnRoute.KINGSOFT.getCode(), "金山云", () -> SpringUtil.getBean(KingsoftDomainStatisticsServiceImpl.class)),
    SELF_HOSTED(CdnRoute.SELF_HOSTED.getCode(), "自建 CDN", () -> SpringUtil.getBean(SelfHostedDomainStatisticsServiceImpl.class));

    private final String route;

    private final String name;

    private final Supplier<ICdnStatisticsPlatformService> serviceSupplier;

    /**
     * 转化
     *
     * @param route 路线
     * @return {@link CdnStatisticsRoute}
     */
    public static CdnStatisticsRoute convert(String route) {
        return Arrays.stream(CdnStatisticsRoute.values())
                .filter(e -> Objects.equals(e.route, route))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported CDN statistics route: " + route));
    }
}
