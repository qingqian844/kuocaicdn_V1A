package com.kuocai.cdn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrderDTO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 工单编码
     */
    private String cd;

    /**
     * 分类ID
     */
    private Long typeId;

    /**
     * 分类名称
     */
    private String typeName;

    /**
     * 紧急程度【普通咨询、操作体验问题、系统异常】
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
     * 状态【0:待处理】【1:已解决】【-1:未解决】
     */
    private Integer status;

    /**
     * 评价得星【0-5】
     */
    private Integer evaluationStars;

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
}
