package com.kuocai.cdn.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * (RealNameAuthentication)实体类
 *
 * @author XUEW
 * @since 2023-05-05 14:12:18
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RealNameAuthenticationVo {

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

    /**
     * 用户头像
     */
    private String img;

    /**
     * 用户头像
     */
    private String userName;
}
