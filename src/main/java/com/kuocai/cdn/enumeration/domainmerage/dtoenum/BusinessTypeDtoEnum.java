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
public enum BusinessTypeDtoEnum {

    /**
     * 开
     */
    WEB("web", "web", "web", "noUse", "web", "page"),

    /**
     * 开
     */
    DOWNLOAD("download", "download", "download", "noUse", "download", "download"),

    /**
     * 关
     */
    VIDEO("video", "video", "video", "noUse", "vod", "video_demand");

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
    public static BusinessTypeDtoEnum getOtherParam(String param) throws BusinessException {
        Optional<BusinessTypeDtoEnum> first = Arrays.stream(BusinessTypeDtoEnum.values())
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
        Optional<BusinessTypeDtoEnum> first = null;
        if(ObjectUtil.equal(route, CdnRoute.HUAWEI.getCode())){
            first = Arrays.stream(BusinessTypeDtoEnum.values())
                    .filter(e -> Objects.equals(e.getHuawei(), param))
                    .findFirst();
        }else if(ObjectUtil.equal(route, CdnRoute.VOLCENGINE.getCode())){
            first = Arrays.stream(BusinessTypeDtoEnum.values())
                    .filter(e -> Objects.equals(e.getVolCenGine(), param))
                    .findFirst();
        }else if(ObjectUtil.equal(route, CdnRoute.YIFAN.getCode())){
            first = Arrays.stream(BusinessTypeDtoEnum.values())
                    .filter(e -> Objects.equals(e.getYiFan(), param))
                    .findFirst();
        }else if(ObjectUtil.equal(route, CdnRoute.QINIU.getCode())){
            first = Arrays.stream(BusinessTypeDtoEnum.values())
                    .filter(e -> Objects.equals(e.getQiNiu(), param))
                    .findFirst();
        }else if(ObjectUtil.equal(route, CdnRoute.BAISHAN.getCode())){
            first = Arrays.stream(BusinessTypeDtoEnum.values())
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

