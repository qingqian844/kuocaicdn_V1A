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
public enum OriginTypeEnum {

    IPADDR("ipaddr", "ipaddr", "ip", "ipaddr", "ip", "ip", "ip", "ipaddr"),
    DOMAIN("domain", "domain", "domain", "domain", "domain", "domain", "domain", "domain");

    /**
     * 括彩CDN参数
     */
    private final String param;

    private final String huawei;

    private final String volCenGine;

    private final String yiFan;

    private final String qiNiu;

    private final String tencent;

    private final String cdnetworks;

    private final String aliyun;

    /**
     * 获取系统参数对应的别的平台的参数
     *
     * @param param 系统参数
     * @return {@code OriginTypeEnum}
     */
    public static OriginTypeEnum getOtherParam(String param) throws BusinessException {
        return Arrays.stream(OriginTypeEnum.values())
                .filter(e -> Objects.equals(e.getParam(), param))
                .findFirst()
                .orElseThrow(() -> new BusinessException("参数异常"));
    }

    /**
     * 别的平台获取系统参数
     *
     * @param param 别的平台参数
     * @param cdnOperationRoute 线路
     * @return {@code OriginTypeEnum}
     */
    public static String getSelfParam(String param, CdnOperationRoute cdnOperationRoute) throws BusinessException {
        if (Assert.isEmpty(cdnOperationRoute)) {
            throw new BusinessException("线路出错~");
        }
        String route = cdnOperationRoute.getRoute();
        Optional<OriginTypeEnum> first = Optional.empty();
        if (ObjectUtil.equal(route, CdnRoute.HUAWEI.getCode())) {
            first = Arrays.stream(OriginTypeEnum.values())
                    .filter(e -> Objects.equals(e.getHuawei(), param))
                    .findFirst();
        } else if (ObjectUtil.equal(route, CdnRoute.VOLCENGINE.getCode())) {
            first = Arrays.stream(OriginTypeEnum.values())
                    .filter(e -> Objects.equals(e.getVolCenGine(), param))
                    .findFirst();
        } else if (ObjectUtil.equal(route, CdnRoute.YIFAN.getCode())) {
            first = Arrays.stream(OriginTypeEnum.values())
                    .filter(e -> Objects.equals(e.getYiFan(), param))
                    .findFirst();
        } else if (ObjectUtil.equal(route, CdnRoute.QINIU.getCode())) {
            first = Arrays.stream(OriginTypeEnum.values())
                    .filter(e -> Objects.equals(e.getQiNiu(), param))
                    .findFirst();
        } else if (ObjectUtil.equal(route, CdnRoute.TENCENT.getCode())) {
            first = Arrays.stream(OriginTypeEnum.values())
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
