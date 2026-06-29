package com.kuocai.cdn.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


/**
 * (FlowPackage)实体类
 *
 * @author makejava
 * @since 2023-03-17 15:36:38
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlowPackageVo {

    /**
     * 主键ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 流量包名称
     */
    private String packageName;

    /**
     * 描述信息
     */
    private String remark;

    /**
     * 流量包状态：enable、disable
     */
    private String status;

    /**
     * 计费方式：month、quarter、year
     */
    private String chargeType;

    /**
     * 流量包类型：通用common用于计算器使用、activity活动，通用类型计费方式只能是月表示月单价
     */
    private String packageType;

    /**
     * 每日限量
     */
    private Integer dayLimit;

    /**
     * 用户限量
     */
    private Integer userLimit;

    /**
     * 购买人群
     */
    private String buyerRule;

    /**
     * 流量包大小
     */
    private Double size;

    /**
     * 价格
     */
    private Double price;

    /**
     * 价格
     */
    private BigDecimal price3;

    /**
     * 价格
     */
    private BigDecimal price12;

    /**
     * 购买次数
     */
    private Integer buyCount;

    /**
     * 流量包大小，已自动换算单位
     */
    private String flowPackageSizeString;

    /**
     * 排序优先级
     */
    private Integer sort;

    private Integer edgeoneDomainQuota;
}
