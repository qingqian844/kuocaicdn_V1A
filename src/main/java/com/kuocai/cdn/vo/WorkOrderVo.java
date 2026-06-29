package com.kuocai.cdn.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * (WorkOrder)实体类
 *
 * @author XUEW
 * @since 2023-02-20 21:06:04
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrderVo {

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
     * 用户名
     */
    private String userName;

    /**
     * 头像
     */
    private String userImg;

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
     * 类型名称
     */
    private String typeName;

    /**
     * 结果
     */
    private String result;

    /**
     * 紧急程度【普通咨询、操作体验问题、系统异常】
     */
    private String urgentLevel;

    /**
     * 标题
     */
    private String title;

    /**
     * 问题域名
     */
    private String domain;

    /**
     * 问题描述
     */
    private String remark;

    /**
     * 状态【undispose:待处理】【finish:已解决】【unfinish:未解决】
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
     * 搜索参数
     */
    private String search;

    /**
     * 查询的工单状态
     */
    private String statusPk;

    /**
     * 搜索的开始时间
     */
    private Date startTime;

    /**
     * 搜索的结束时间
     */
    private Date endTime;

    /**
     * 默认为false 表示倒叙
     * 如果为true 表示升序
     */
    private boolean dateSort = false;

    /**
     * 分页页码
     */
    private int startRecord = 1;

    /**
     * 分页数据大小
     */
    private int limitRecord = 10;
}
