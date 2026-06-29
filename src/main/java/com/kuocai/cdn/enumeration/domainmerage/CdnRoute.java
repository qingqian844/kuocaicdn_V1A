package com.kuocai.cdn.enumeration.domainmerage;

import lombok.Getter;

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
    KINGSOFT("kingsoft");

    private final String code;

    CdnRoute(String code) {
        this.code = code;
    }
}
