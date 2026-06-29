package com.kuocai.cdn.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.kuocai.cdn.constant.FlowPackageChargeType;
import com.kuocai.cdn.util.KuocaiBaseUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

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
@TableName("flow_package")
public class FlowPackage implements Serializable {

    private static final long serialVersionUID = -60604638414938763L;

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
    private Long size;

    /**
     * 价格
     */
    private BigDecimal price;

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
     * 排序优先级
     */
    private Integer sort;

    private Integer edgeoneDomainQuota;

    /**
     * 购买权限
     */
    @TableField(exist = false)
    private Boolean purchaseAuthority;

    /**
     * 今日剩余购买次数
     */
    @TableField(exist = false)
    private Integer dayLastCount;

    /**
     * 用户剩余购买次数
     */
    @TableField(exist = false)
    private Integer userLastCount;

    /**
     * 标题
     */
    @TableField(exist = false)
    private String title;

    /**
     * 返回描述信息
     */
    public String detail() {
        return String.format("流量包名称：《%s》，描述：《%s》，计费方式：《%s》，大小：《%s》，当前价格：《%.2f》",
                packageName, remark, FlowPackageChargeType.flowPackageChargeTypeNameMap.get(chargeType), showSizeName(), price.doubleValue());
    }

    /**
     * description: 获取流量大小(实际)
     */
    public String showSizeName() {
        return KuocaiBaseUtil.autoReducedFlowUnit(size);
    }

    public BigDecimal unitPrice() {
        BigDecimal gb = BigDecimal.valueOf(size).divide(BigDecimal.valueOf(Math.pow(1024, 3)), 2, RoundingMode.HALF_UP);
        return price.divide(gb, 2, RoundingMode.HALF_UP);
    }

    public String priceText() {
        return formatAmount(price);
    }

    public String price3Text() {
        return formatAmount(price3);
    }

    public String price12Text() {
        return formatAmount(price12);
    }

    public String unitPriceText() {
        return formatAmount(unitPrice());
    }

    public String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        return amount.stripTrailingZeros().toPlainString();
    }
}
