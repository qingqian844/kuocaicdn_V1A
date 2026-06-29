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
import java.util.Objects;

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
@TableName("login_device")
public class LoginDevice implements Serializable {

    private static final long serialVersionUID = 405572519008870730L;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoginDevice that = (LoginDevice) o;
        return Objects.equals(userId, that.userId) && Objects.equals(browser, that.browser) && Objects.equals(device, that.device) && Objects.equals(location, that.location) && Objects.equals(loginIp, that.loginIp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, browser, device, location, loginIp);
    }
}
