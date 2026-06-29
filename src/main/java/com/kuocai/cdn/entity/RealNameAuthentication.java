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
 * (RealNameAuthentication)实体类
 *
 * @author XUEW
 * @since 2023-05-05 14:12:17
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("real_name_authentication")
public class RealNameAuthentication implements Serializable {

    private static final long serialVersionUID = 618925937169262452L;

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
     * 实名类型：person、enterprise
     */
    private String authenticationType;

    /**
     * 名称
     */
    private String name;

    /**
     * 证件号
     */
    private String idCardNum;

    /**
     * 身份证正面照片
     */
    private String frontImg;

    /**
     * 身份证反面照片
     */
    private String backImg;

    /**
     * 状态：wait、success、fail
     */
    private String status;

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
