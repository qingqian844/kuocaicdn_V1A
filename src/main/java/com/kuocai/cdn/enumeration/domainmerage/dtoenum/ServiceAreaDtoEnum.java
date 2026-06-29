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
public enum ServiceAreaDtoEnum {

    /**
     * 国内
     */
    CHINA("mainland_china", "mainland_china", "chinese_mainland", "noUse", "china", "mainland_china"),

    /**
     * 海外
     */
    FOREIGN("outside_mainland_china", "outside_mainland_china", "outside_chinese_mainland", "noUse", "foreign", "outside_mainland_china"),

    /**
     * 全球
     */
    GLOBAL("global", "global", "global", "noUse", "global", "global");

    /**
     * 括彩CDN参数
     */
    private String param;

    private String huawei;

    private String volCenGine;

    private String yiFan;

    private String qiNiu;

    private String baiShan;

    /**
     * 获取系统参数对应的别的平台的参数
     *
     * @param param
     * @return
     */
    public static ServiceAreaDtoEnum getOtherParam(String param) throws BusinessException {
        Optional<ServiceAreaDtoEnum> first = Arrays.stream(ServiceAreaDtoEnum.values())
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
        Optional<ServiceAreaDtoEnum> first = null;
        if(ObjectUtil.equal(route, CdnRoute.HUAWEI.getCode())){
            first = Arrays.stream(ServiceAreaDtoEnum.values())
                    .filter(e -> Objects.equals(e.getHuawei(), param))
                    .findFirst();
        }else if(ObjectUtil.equal(route, CdnRoute.VOLCENGINE.getCode())){
            first = Arrays.stream(ServiceAreaDtoEnum.values())
                    .filter(e -> Objects.equals(e.getVolCenGine(), param))
                    .findFirst();
        }else if(ObjectUtil.equal(route, CdnRoute.YIFAN.getCode())){
            first = Arrays.stream(ServiceAreaDtoEnum.values())
                    .filter(e -> Objects.equals(e.getYiFan(), param))
                    .findFirst();
        }else if(ObjectUtil.equal(route, CdnRoute.QINIU.getCode())){
            first = Arrays.stream(ServiceAreaDtoEnum.values())
                    .filter(e -> Objects.equals(e.getQiNiu(), param))
                    .findFirst();
        }else if(ObjectUtil.equal(route, CdnRoute.BAISHAN.getCode())){
            first = Arrays.stream(ServiceAreaDtoEnum.values())
                    .filter(e -> Objects.equals(e.getBaiShan(), param))
                    .findFirst();
        }
        if (first.isPresent()) {
            return first.get().getParam();
        } else {
            throw new BusinessException("参数异常");
        }
    }
}

