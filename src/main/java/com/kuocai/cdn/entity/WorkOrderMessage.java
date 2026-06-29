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
 * (WorkOrderMessage)实体类
 *
 * @author ChenWEI
 * @since 2023-02-20 21:12:34
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("work_order_message")
public class WorkOrderMessage implements Serializable {

    private static final long serialVersionUID = -33892003106933151L;

    /**
     * 主键ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 聊天内容
     */
    private String context;

    /**
     * 工单ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long workOrderId;

    /**
     * 发起人ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /**
     * 处理人ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long adminId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改时间
     */
    private Date updateTime;
}
