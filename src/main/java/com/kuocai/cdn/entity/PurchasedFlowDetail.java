package com.kuocai.cdn.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * (PurchasedFlowDetail)实体类
 *
 * @author XUEW
 * @since 2023-04-02 19:40:21
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("purchased_flow_detail")
public class PurchasedFlowDetail implements Serializable {

    private static final long serialVersionUID = 356770028768102694L;

    /**
     * 主键ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 用户ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /**
     * 已购记录ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long purchasedFlowId;

    /**
     * 本次使用流量
     */
    private Long consume;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改时间
     */
    private Date updateTime;
}
