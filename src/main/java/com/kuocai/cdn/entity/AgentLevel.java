package com.kuocai.cdn.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * (AgentLevel)实体类
 *
 * @author todoitbo
 * @since 2023-05-22 21:02:30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("agent_level")
public class AgentLevel implements Serializable {

    private static final long serialVersionUID = 444110697444778226L;

    /**
     * 主键ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 代理等级名称
     */
    private String name;

    /**
     * 流量订单分润比率
     */
    private BigDecimal flowOrderRate;

    /**
     * 流量包分润比率
     */
    private BigDecimal packageRate;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
