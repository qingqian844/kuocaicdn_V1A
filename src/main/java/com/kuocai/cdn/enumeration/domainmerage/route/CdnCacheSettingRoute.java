package com.kuocai.cdn.enumeration.domainmerage.route;

import cn.hutool.extra.spring.SpringUtil;
import com.kuocai.cdn.enumeration.domainmerage.CdnRoute;
import com.kuocai.cdn.service.domain.cacheset.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 刷新预热路线方法
 */
@Getter
@AllArgsConstructor
public enum CdnCacheSettingRoute {

    /**
     * 路线：huawei、volcengine、huawei_volcengine、yifan、tencent, cdnetworks, aliyun
     */
    HUAWEI(CdnRoute.HUAWEI.getCode(), "华为云", () -> SpringUtil.getBean(HuaweiDomainCacheSettingServiceImpl.class)),
    VOLCENGINE(CdnRoute.VOLCENGINE.getCode(), "火山云", () -> SpringUtil.getBean(VolCenGineDomainCacheSettingServiceImpl.class)),
    HUAWEI_VOLCENGINE(CdnRoute.HUAWEI_VOLCENGINE.getCode(), "华为火山云融合调度", () -> SpringUtil.getBean(HuaweiVolCenGineDomainCacheSettingServiceImpl.class)),
    QINIU(CdnRoute.QINIU.getCode(), "七牛云", () -> SpringUtil.getBean(QiNiuDomainCacheSettingServiceImpl.class)),
    BAISHAN(CdnRoute.BAISHAN.getCode(), "白山云", () -> SpringUtil.getBean(BaiShanDomainCacheSettingServiceImpl.class)),
    YIFAN(CdnRoute.YIFAN.getCode(), "易凡", () -> SpringUtil.getBean(YiFanDomainCacheSettingServiceImpl.class)),
    TENCENT(CdnRoute.TENCENT.getCode(), "腾讯云", () -> SpringUtil.getBean(TencentDomainCacheSettingServiceImpl.class)),
    TENCENT_EDGEONE(CdnRoute.TENCENT_EDGEONE.getCode(), "腾讯云 EdgeOne", () -> SpringUtil.getBean(TencentEdgeOneDomainCacheSettingServiceImpl.class)),
    CDNETWORKS(CdnRoute.CDNETWORKS.getCode(), "CDNETWORKS", () -> SpringUtil.getBean(CdnetworksDomainCacheSettingServiceImpl.class)),
    ALIYUN(CdnRoute.ALIYUN.getCode(), "阿里云", () -> SpringUtil.getBean(AliyunDomainCacheSettingServiceImpl.class)),
    BAIDU(CdnRoute.BAIDU.getCode(), "百度云", () -> SpringUtil.getBean(BaiduDomainCacheSettingServiceImpl.class)),
    KINGSOFT(CdnRoute.KINGSOFT.getCode(), "金山云", () -> SpringUtil.getBean(KingsoftCacheSettingServiceImpl.class)),
    SELF_HOSTED(CdnRoute.SELF_HOSTED.getCode(), "自建 CDN", () -> SpringUtil.getBean(SelfHostedDomainCacheSettingServiceImpl.class)),
    SELF_HOSTED_MAINLAND(CdnRoute.SELF_HOSTED_MAINLAND.getCode(), "国内自建 CDN", () -> SpringUtil.getBean(SelfHostedDomainCacheSettingServiceImpl.class)),
    SELF_HOSTED_OVERSEAS(CdnRoute.SELF_HOSTED_OVERSEAS.getCode(), "海外自建 CDN", () -> SpringUtil.getBean(SelfHostedDomainCacheSettingServiceImpl.class)),
    SELF_HOSTED_GLOBAL(CdnRoute.SELF_HOSTED_GLOBAL.getCode(), "全球自建 CDN", () -> SpringUtil.getBean(SelfHostedDomainCacheSettingServiceImpl.class));

    private final String route;

    private final String name;

    private final Supplier<ICdnCacheSettingPlatformService> serviceSupplier;

    /**
     * 转化
     *
     * @param route 路线
     * @return {@link CdnCacheSettingRoute}
     */
    public static CdnCacheSettingRoute convert(String route) {
        return Arrays.stream(CdnCacheSettingRoute.values())
                .filter(e -> Objects.equals(e.route, route))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported CDN cache route: " + route));
    }
}
