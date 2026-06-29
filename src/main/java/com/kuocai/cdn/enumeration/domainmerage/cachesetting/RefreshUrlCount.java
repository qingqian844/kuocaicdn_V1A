package com.kuocai.cdn.enumeration.domainmerage.cachesetting;

import com.kuocai.cdn.enumeration.domainmerage.CdnRoute;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

@Getter
@AllArgsConstructor
public enum RefreshUrlCount {

    //    TOTAL("TOTAL", "500"),
    HUAWEI(CdnRoute.HUAWEI.getCode(), "20000"),
    VOLCENGINE(CdnRoute.VOLCENGINE.getCode(), "20000"),
    //    HUAWEI_VOLCENGINE(CdnRoute.HUAWEI_VOLCENGINE.getCode(), "20000"),
    QINIU(CdnRoute.QINIU.getCode(), "500"),
    BAISHAN(CdnRoute.BAISHAN.getCode(), "20000"),
    YIFAN(CdnRoute.YIFAN.getCode(), "2000"),
    CDNETWORKS(CdnRoute.CDNETWORKS.getCode(), "500"),
    TENCENT_EDGEONE(CdnRoute.TENCENT_EDGEONE.getCode(), "20000");

    private String key;

    private String num;

    /**
     * 转化
     *
     * @param key
     * @return
     */
    public static RefreshUrlCount convert(String key) {
        return Arrays.stream(RefreshUrlCount.values())
                .filter(e -> Objects.equals(e.key, key))
                .findFirst()
                .orElse(RefreshUrlCount.HUAWEI);
    }


}
