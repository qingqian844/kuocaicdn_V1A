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
 * (WorkOrder)实体类
 *
 * @author XUEW
 * @since 2023-02-20 21:06:03
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("work_order")
public class WorkOrder implements Serializable {

    private static final long serialVersionUID = 983015478978511271L;

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
     * 工单编码
     */
    private String cd;

    /**
     * 分类ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long typeId;

    /**
     * 紧急程度
     */
    private String urgentLevel;

    /**
     * 标题
     */
    private String title;

    /**
     * 问题描述
     */
    private String remark;

    /**
     * 状态
     */
    private String status;

    /**
     * 反馈信息
     */
    private String feedback;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改时间
     */
    private Date updateTime;

    /**
     * 结果
     */
    private String result;

    /**
     * 问题域名
     */
    private String domain;
}
