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

;

/**
 * (SysConfig)实体类
 *
 * @author makejava
 * @since 2023-03-22 15:41:05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("sys_config")
public class SysConfig implements Serializable {

    private static final long serialVersionUID = 205826256827114502L;

    /**
     * 主键ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 配置类型
     */
    private String bizType;

    /**
     * 配置内容
     */
    private String configContent;

    /**
     * 创建者
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long createBy;

    /**
     * 更新者
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long updateBy;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
