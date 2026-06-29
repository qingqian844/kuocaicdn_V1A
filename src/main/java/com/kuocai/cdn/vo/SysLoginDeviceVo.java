package com.kuocai.cdn.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 登录设备(SysLoginDevice)实体类
 *
 * @author XUEW
 * @since 2023-02-15 20:09:23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SysLoginDeviceVo {

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
     * 浏览器
     */
    private String browser;

    /**
     * 设备
     */
    private String device;

    /**
     * 位置
     */
    private String location;

    /**
     * 登录IP
     */
    private String loginIp;

    /**
     * 登录时间
     */
    private Date loginTime;
}
