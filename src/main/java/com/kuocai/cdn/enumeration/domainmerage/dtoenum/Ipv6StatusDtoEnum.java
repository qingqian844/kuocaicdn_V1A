package com.kuocai.cdn.enumeration.domainmerage.dtoenum;

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
public enum Ipv6StatusDtoEnum {

    /**
     * 开
     */
    ON("1", "1", "true", "noUse", "3.0"),

    /**
     * 关
     */
    OFF("0", "0", "false", "noUse", "1.0");

    /**
     * 括彩CDN参数
     */
    private String param;

    private String huawei;

    private String volCenGine;

    private String yiFan;

    private String qiNiu;

    /**
     * 获取系统参数对应的别的平台的参数
     *
     * @param param
     * @return
     */
    public static Ipv6StatusDtoEnum getOtherParam(String param) throws BusinessException {
        Optional<Ipv6StatusDtoEnum> first = Arrays.stream(Ipv6StatusDtoEnum.values())
                .filter(e -> Objects.equals(e.getParam(), param))
                .findFirst();
        if (first.isPresent()) {
            return first.get();
        } else {
            throw new BusinessException("参数异常");
        }
    }

    /**
     * 别的平台获取系统参数
     *
     * @param param
     * @return
     */
    public static String getSelfParam(String param, CdnOperationRoute cdnOperationRoute) throws BusinessException {
        if (Assert.isEmpty(cdnOperationRoute)){
            throw new BusinessException("线路出错~");
        }
        String route = cdnOperationRoute.getRoute();
        Optional<Ipv6StatusDtoEnum> first = null;
        if(ObjectUtil.equal(route, CdnRoute.HUAWEI.getCode())){
            first = Arrays.stream(Ipv6StatusDtoEnum.values())
                    .filter(e -> Objects.equals(e.getHuawei(), param))
                    .findFirst();
        }else if(ObjectUtil.equal(route, CdnRoute.VOLCENGINE.getCode())){
            first = Arrays.stream(Ipv6StatusDtoEnum.values())
                    .filter(e -> Objects.equals(e.getVolCenGine(), param))
                    .findFirst();
        }else if(ObjectUtil.equal(route, CdnRoute.YIFAN.getCode())){
            first = Arrays.stream(Ipv6StatusDtoEnum.values())
                    .filter(e -> Objects.equals(e.getYiFan(), param))
                    .findFirst();
        }else if(ObjectUtil.equal(route, CdnRoute.QINIU.getCode())){
            first = Arrays.stream(Ipv6StatusDtoEnum.values())
                    .filter(e -> Objects.equals(e.getQiNiu(), param))
                    .findFirst();
        }
        if (first.isPresent()) {
            return first.get().getParam();
        } else {
            throw new BusinessException("参数异常");
        }
    }
}

