package com.kuocai.cdn.enumeration.domainmerage.domain;

import cn.hutool.core.util.ObjectUtil;
import com.kuocai.cdn.enumeration.domainmerage.CdnRoute;
import com.kuocai.cdn.enumeration.domainmerage.route.CdnOperationRoute;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.util.Assert;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

@Getter
@AllArgsConstructor
public enum BusinessTypeEnum {

    /**
     * 网页
     */
    WEB("web", "web", "web", "web", "page", "web", "web", "web", "image", "page"),

    /**
     * 下载
     */
    DOWNLOAD("download", "download", "download", "download", "download", "download", "", "download", "download", "download"),

    /**
     * 点播
     */
    VIDEO("video", "video", "video", "video", "video_demand", "hybrid", "", "video", "media", "video");

    /**
     * 括彩CDN参数
     */
    private final String param;

    private final String huawei;

    private final String volCenGine;

    private final String yiFan;

    private final String baiShan;

    private final String tencent;

    private final String cdnetworks;

    private final String aliyun;

    private final String baidu;

    private final String kingsoft;

    /**
     * 获取系统参数对应的别的平台的参数
     *
     * @param param 系统参数
     * @return {@code OriginTypeEnum}
     */
    public static BusinessTypeEnum getOtherParam(String param) throws BusinessException {
        return Arrays.stream(BusinessTypeEnum.values())
                .filter(e -> Objects.equals(e.getParam(), param))
                .findFirst()
                .orElseThrow(() -> new BusinessException("参数异常"));
    }

    /**
     * 别的平台获取系统参数
     *
     * @param param 别的平台参数
     * @return {@code OriginTypeEnum}
     */
    public static String getSelfParam(String param, CdnOperationRoute cdnOperationRoute) throws BusinessException {
        if (Assert.isEmpty(cdnOperationRoute)) {
            throw new BusinessException("线路出错~");
        }
        String route = cdnOperationRoute.getRoute();
        Optional<BusinessTypeEnum> first = Optional.empty();
        if (ObjectUtil.equal(route, CdnRoute.HUAWEI.getCode())) {
            first = Arrays.stream(BusinessTypeEnum.values())
                    .filter(e -> Objects.equals(e.getHuawei(), param))
                    .findFirst();
        } else if (ObjectUtil.equal(route, CdnRoute.VOLCENGINE.getCode())) {
            first = Arrays.stream(BusinessTypeEnum.values())
                    .filter(e -> Objects.equals(e.getVolCenGine(), param))
                    .findFirst();
        } else if (ObjectUtil.equal(route, CdnRoute.YIFAN.getCode())) {
            first = Arrays.stream(BusinessTypeEnum.values())
                    .filter(e -> Objects.equals(e.getYiFan(), param))
                    .findFirst();
        } else if (ObjectUtil.equal(route, CdnRoute.BAISHAN.getCode())) {
            first = Arrays.stream(BusinessTypeEnum.values())
                    .filter(e -> Objects.equals(e.getBaiShan(), param))
                    .findFirst();
        } else if (ObjectUtil.equal(route, CdnRoute.TENCENT.getCode())) {
            first = Arrays.stream(BusinessTypeEnum.values())
                    .filter(e -> Objects.equals(e.getTencent(), param))
                    .findFirst();
        } else if (ObjectUtil.equal(route, CdnRoute.KINGSOFT.getCode())) {
            first = Arrays.stream(BusinessTypeEnum.values())
                    .filter(e -> Objects.equals(e.getKingsoft(), param))
                    .findFirst();
        }
        if (first.isPresent()) {
            return first.get().getParam();
        } else {
            throw new BusinessException("参数异常");
        }
    }
}
