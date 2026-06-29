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
 * 菜单实体
 *
 * @author XUEW
 * @date 下午9:01 2023/2/12
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("sys_menu")
public class SysMenu implements Serializable {

    private static final long serialVersionUID = 986791003592218132L;

    /**
     * 主键ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 菜单名称
     */
    private String name;

    /**
     * 级别
     */
    private String level;

    /**
     * URL
     */
    private String url;

    /**
     * 菜单类型：only-main、only-proxy、both
     */
    private String type;

    /**
     * 排序优先级
     */
    private Integer priority;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改时间
     */
    private Date updateTime;
}