package com.kuocai.cdn.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.PasswordUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户实体
 *
 * @author XUEW
 * @date 下午9:01 2023/2/12
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("sys_user")
public class SysUser implements Serializable {

    private static final long serialVersionUID = 986791003592218132L;

    /**
     * 主键ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 角色ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long roleId;

    /**
     * 流量单价
     */
    private BigDecimal flowPrice;

    /**
     * 虚量率，默认为1
     */
    private BigDecimal virtualRate;

    /**
     * 创建域名最大数量
     */
    private Integer maxDomainCount;

    /**
     * 昵称
     */
    private String userName;

    /**
     * 密码
     */
    private String userPwd;

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
     * 我的网站
     */
    private String myWebsite;

    /**
     * 手机号
     */
    private String phone;

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
     * 微博登录标识
     */
    private String weiboOpenId;

    /**
     * 流量账单自动结算
     */
    private Integer autoBalance;

    /**
     * 路线：huawei、volcengine、huawei_volcengine、yifan
     */
    private String route;

    /**
     * 是否开启海外线路
     */
    private Integer enableOverseas;

    /**
     * 是否开启全球
     */
    private Integer enableGlobal;

    /**
     * 推荐人ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long referrerId;

    /**
     * 代理用户ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long agentUserId;

    /**
     * 代理等级ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long agentLevelId;

    public boolean enableOverseas() {
        return enableOverseas == 1;
    }

    public boolean enableGlobal() {
        return enableGlobal != null && enableGlobal == 1;
    }

    /**
     * 是否开启流量账单自动结算
     */
    public boolean openAutoBalance() {
        if (Assert.isEmpty(autoBalance)) {
            return false;
        }
        return autoBalance == 1;
    }

    /**
     * 信息脱敏
     */
    public void desensitize() {
        this.userPwd = null;
        this.pwdSalt = null;
        this.idCardNum = null;
    }

    /**
     * 获取明文密码
     */
    public String showPlaintextPwd() {
        throw new UnsupportedOperationException("Plaintext password recovery is disabled");
    }

    /**
     * 加密自身用户的密码
     */
    public SysUser encrypt() {
        this.userPwd = PasswordUtils.hash(userPwd);
        this.pwdSalt = null;
        return this;
    }

    /**
     * 是否绑定微信
     */
    public boolean bindWechat() {
        return Assert.isEmpty(wechatOpenId);
    }

    /**
     * 获取加密名称
     *
     * @return
     */
    public String getEncryptName() {
        if (Assert.isEmpty(realName)) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(realName.charAt(0));
        for (int i = 1; i < realName.length(); i++) {
            sb.append("*");
        }
        return sb.toString();
    }

    public String getEncryptCardNum() {
        if (Assert.isEmpty(idCardNum)) {
            return null;
        }
        if (idCardNum.length() <= 10) {
            return idCardNum;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(idCardNum.charAt(0));
        sb.append(idCardNum.charAt(1));
        sb.append(idCardNum.charAt(2));
        sb.append(idCardNum.charAt(3));
        for (int i = 4; i < idCardNum.length() - 4; i++) {
            sb.append("*");
        }
        sb.append(idCardNum.charAt(idCardNum.length() - 4));
        sb.append(idCardNum.charAt(idCardNum.length() - 3));
        sb.append(idCardNum.charAt(idCardNum.length() - 2));
        sb.append(idCardNum.charAt(idCardNum.length() - 1));
        return sb.toString();
    }

    public boolean isAdmin() {
        return roleId == 1;
    }
}
