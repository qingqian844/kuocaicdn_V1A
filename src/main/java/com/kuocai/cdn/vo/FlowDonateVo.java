package com.kuocai.cdn.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;

/**
 * (FlowDonate)实体类
 *
 * @author todoitbo
 * @since 2023-05-11 19:43:31
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlowDonateVo {

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
     * 用户ID数组
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private List<Long> userIds;

    /**
     * 代理用户ID数组
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private List<Long> agentIds;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 赠送类型，user/agent
     */
    private String donateType;

    /**
     * 头像
     */
    private String img;

    /**
     * 流量包名称
     */
    private String flowPackageName;

    /**
     * 流量包大小
     */
    private Double flowPackageSize;

    /**
     * 前端展示的带单位的流量包大小
     */
    private String flowPackageSizeString;

    /**
     * 截止时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date deadline;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 状态：success、withdraw
     */
    private String status;

    /**
     * 赠送记录ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long purchasedFlowId;
}
