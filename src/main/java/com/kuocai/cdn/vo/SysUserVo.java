package com.kuocai.cdn.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SysUserVo {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 角色ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long RoleId;

    /**
     * 昵称
     */
    private String userName;

    /**
     * 密码
     */
    private String userPwd;

    /**
     * 旧密码
     */
    private String oldPwd;

    /**
     * 密码盐
     */
    private String pwdSalt;

    /**
     * 姓名
     */
    private String realName;

    /**
     * 身份证号
     */
    private String idCardNum;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 流量单价
     */
    private BigDecimal flowPrice;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 头像
     */
    private String img;

    /**
     * 状态
     */
    private String status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改时间
     */
    private Date updateTime;

    /**
     * 最近登录时间
     */
    private Date lastLoginTime;

    /**
     * 最近登录IP
     */
    private String lastLoginIp;

    /**
     * 微信登录标识
     */
    private String wechatOpenId;

    /**
     * QQ登录标识
     */
    private String qqOpenId;

    /**
     * 我的网站
     */
    private String myWebsite;

    /**
     * 短信验证码
     */
    private String smsCode;

    /**
     * 登陆账号
     */
    private String userAccount;

    /**
     * 记住我
     */
    private Boolean remember;

    /**
     * 流量账单自动结算
     */
    private Integer autoBalance;
}
