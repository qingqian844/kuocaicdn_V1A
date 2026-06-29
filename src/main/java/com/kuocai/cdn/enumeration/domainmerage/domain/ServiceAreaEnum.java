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
public enum ServiceAreaEnum {

    /**
     * 国内
     */
    MAINLAND_CHINA("mainland_china",
            "mainland_china", "chinese_mainland", "mainland_china", "mainland_china", "mainland", "", "domestic"),

    /**
     * 全球
     */
    GLOBAL("global",
            "global", "global", "global", "global", "global", "", "global"),

    /**
     * 国外
     */
    OUTSIDE_MAINLAND_CHINA("outside_mainland_china",
            "outside_mainland_china", "outside_chinese_mainland", "outside_mainland_china", "outside_mainland_china", "overseas", "overseas", "overseas");

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

    /**
     * 获取系统参数对应的别的平台的参数
     *
     * @param param 系统参数
     * @return {@code OriginTypeEnum}
     */
    public static ServiceAreaEnum getOtherParam(String param) throws BusinessException {
        return Arrays.stream(ServiceAreaEnum.values())
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
        Optional<ServiceAreaEnum> first = Optional.empty();
        if (ObjectUtil.equal(route, CdnRoute.HUAWEI.getCode())) {
            first = Arrays.stream(ServiceAreaEnum.values())
                    .filter(e -> Objects.equals(e.getHuawei(), param))
                    .findFirst();
        } else if (ObjectUtil.equal(route, CdnRoute.VOLCENGINE.getCode())) {
            first = Arrays.stream(ServiceAreaEnum.values())
                    .filter(e -> Objects.equals(e.getVolCenGine(), param))
                    .findFirst();
        } else if (ObjectUtil.equal(route, CdnRoute.YIFAN.getCode())) {
            first = Arrays.stream(ServiceAreaEnum.values())
                    .filter(e -> Objects.equals(e.getYiFan(), param))
                    .findFirst();
        } else if (ObjectUtil.equal(route, CdnRoute.BAISHAN.getCode())) {
            first = Arrays.stream(ServiceAreaEnum.values())
                    .filter(e -> Objects.equals(e.getBaiShan(), param))
                    .findFirst();
        } else if (ObjectUtil.equal(route, CdnRoute.TENCENT.getCode())) {
            first = Arrays.stream(ServiceAreaEnum.values())
                    .filter(e -> Objects.equals(e.getTencent(), param))
                    .findFirst();
        } else if (ObjectUtil.equal(route, CdnRoute.CDNETWORKS.getCode())) {
            first = Arrays.stream(ServiceAreaEnum.values())
                    .filter(e -> Objects.equals(e.getTencent(), param))
                    .findFirst();
        } else if (ObjectUtil.equal(route, CdnRoute.ALIYUN.getCode())) {
            first = Arrays.stream(ServiceAreaEnum.values())
                    .filter(e -> Objects.equals(e.getTencent(), param))
                    .findFirst();
        }
        if (first.isPresent()) {
            return first.get().getParam();
        } else {
            throw new BusinessException("参数异常");
        }
    }
}
