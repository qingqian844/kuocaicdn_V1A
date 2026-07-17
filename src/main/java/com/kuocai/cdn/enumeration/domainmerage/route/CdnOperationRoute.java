package com.kuocai.cdn.enumeration.domainmerage.route;

import cn.hutool.extra.spring.SpringUtil;
import com.kuocai.cdn.enumeration.domainmerage.CdnRoute;
import com.kuocai.cdn.service.domain.operation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 基础配置路线方法
 */
@Getter
@AllArgsConstructor
public enum CdnOperationRoute {

    /**
     * 路线：huawei、volcengine、huawei_volcengine、yifan、tencent
     */
    HUAWEI(CdnRoute.HUAWEI.getCode(), "华为云", () -> SpringUtil.getBean(HuaweiDomainServiceImpl.class)),
    VOLCENGINE(CdnRoute.VOLCENGINE.getCode(), "火山云", () -> SpringUtil.getBean(VolCenGineDomainServiceImpl.class)),
    HUAWEI_VOLCENGINE(CdnRoute.HUAWEI_VOLCENGINE.getCode(), "华为火山云融合调度", () -> SpringUtil.getBean(HuaweiVolCenGineDomainServiceImpl.class)),
    QINIU(CdnRoute.QINIU.getCode(), "七牛云", () -> SpringUtil.getBean(QiNiuDomainServiceImpl.class)),
    BAISHAN(CdnRoute.BAISHAN.getCode(), "白山云", () -> SpringUtil.getBean(BaiShanDomainServiceImpl.class)),
    YIFAN(CdnRoute.YIFAN.getCode(), "易凡", () -> SpringUtil.getBean(YiFanDomainServiceImpl.class)),
    TENCENT(CdnRoute.TENCENT.getCode(), "腾讯云", () -> SpringUtil.getBean(TencentDomainServiceImpl.class)),
    TENCENT_EDGEONE(CdnRoute.TENCENT_EDGEONE.getCode(), "腾讯云 EdgeOne", () -> SpringUtil.getBean(TencentEdgeOneDomainServiceImpl.class)),
    CDNETWORKS(CdnRoute.CDNETWORKS.getCode(), "cdnetworks", () -> SpringUtil.getBean(CdnetworksDomainServiceImpl.class)),
    ALIYUN(CdnRoute.ALIYUN.getCode(), "阿里云", () -> SpringUtil.getBean(AliyunDomainServiceImpl.class)),
    BAIDU(CdnRoute.BAIDU.getCode(), "百度云", () -> SpringUtil.getBean(BaiduDomainServiceImpl.class)),
    KINGSOFT(CdnRoute.KINGSOFT.getCode(), "金山云", () -> SpringUtil.getBean(KingsoftDomainServiceImpl.class)),
    SELF_HOSTED(CdnRoute.SELF_HOSTED.getCode(), "自建 CDN", () -> SpringUtil.getBean(SelfHostedDomainServiceImpl.class)),
    SELF_HOSTED_MAINLAND(CdnRoute.SELF_HOSTED_MAINLAND.getCode(), "国内自建 CDN", () -> SpringUtil.getBean(SelfHostedDomainServiceImpl.class)),
    SELF_HOSTED_OVERSEAS(CdnRoute.SELF_HOSTED_OVERSEAS.getCode(), "海外自建 CDN", () -> SpringUtil.getBean(SelfHostedDomainServiceImpl.class)),
    SELF_HOSTED_GLOBAL(CdnRoute.SELF_HOSTED_GLOBAL.getCode(), "全球自建 CDN", () -> SpringUtil.getBean(SelfHostedDomainServiceImpl.class));

    /**
     * 路由编码
     */
    private final String route;

    /**
     * 路由名称
     */
    private final String name;

    private final Supplier<ICdnPlatformService> serviceSupplier;

    /**
     * 转化
     *
     * @param route 路由
     * @return {@code CdnOperationRoute}
     */
    public static CdnOperationRoute convert(String route) {
        return Arrays.stream(CdnOperationRoute.values())
                .filter(e -> Objects.equals(e.route, route))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported CDN operation route: " + route));
    }
}
