package com.kuocai.cdn.api.huawei.cdn.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 用户计费管理参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ChargeModeDTO {

    /**
     * GET
     * 加速类型，base（基础加速）
     * 是否必选：是
     * PUT
     * 产品模式，仅支持base（基础加速）
     * 是否必选：是
     */
    private String product_type;

    /**
     * 查询计费模式状态，active（已生效），upcoming（待生效），不传默认为active(已生效)
     * 是否必选：否
     */
    private String status;

    /**
     * 服务区域，mainland_china（国内），outside_mainland_china（海外），不传默认为mainland_china(国内)
     * 是否必选：否
     * PUT
     * 服务区域，仅支持mainland_china（国内）
     * 是否必选：是
     */
    private String service_area;

    /**
     * PUT
     * 计费模式，支持flux（流量），bw（带宽）
     * 是否必选：是
     */
    private String charge_mode;


}
