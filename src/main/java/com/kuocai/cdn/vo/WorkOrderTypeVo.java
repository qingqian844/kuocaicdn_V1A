package com.kuocai.cdn.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 工单类型(WorkOrderType)实体类
 *
 * @author ChenWEI
 * @since 2023-02-20 21:12:34
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrderTypeVo {

    /**
     * 主键ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 类型名称
     */
    private String typeName;

    /**
     * 类型描述
     */
    private String remark;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改时间
     */
    private Date updateTime;
}
