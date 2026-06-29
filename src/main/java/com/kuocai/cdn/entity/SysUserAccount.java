package com.kuocai.cdn.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.kuocai.cdn.util.Assert;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;


/**
 * @author xiaobo
 */
@AllArgsConstructor
@NoArgsConstructor
@TableName("sys_user_account")
@Builder
public class SysUserAccount implements Serializable {

    private static final long serialVersionUID = 645240356563561329L;

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
     * 账户余额
     */
    private BigDecimal accountBalance;

    /**
     * 累积充值
     */
    private BigDecimal amassRecharge;

    /**
     * 分润金额
     */
    private BigDecimal bonus;

    /**
     * 账户状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改时间
     */
    private Date updateTime;


    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public BigDecimal getAccountBalance() {
        return accountBalance;
    }

    public BigDecimal getAmassRecharge() {
        return amassRecharge;
    }

    public BigDecimal getBonus() {
        return bonus;
    }

    public Integer getStatus() {
        return status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public void addAccountBalance(BigDecimal accountBalance) {
        this.accountBalance = Assert.isEmpty(getAccountBalance()) ? accountBalance : getAccountBalance().add(accountBalance);
    }

    public void reduceAccountBalance(BigDecimal accountBalance) {
        this.accountBalance = Assert.isEmpty(getAccountBalance()) ? accountBalance : getAccountBalance().subtract(accountBalance);
    }

    public void addBonus(BigDecimal bonus) {
        this.bonus = Assert.isEmpty(getBonus()) ? bonus : getBonus().add(bonus);
    }

    public void reduceBonus(BigDecimal bonus) {
        this.bonus = Assert.isEmpty(getBonus()) ? bonus : getBonus().subtract(bonus);
    }

    public void addAmassRecharge(BigDecimal amassRecharge) {
        this.amassRecharge = Assert.isEmpty(getAmassRecharge()) ? amassRecharge : getAmassRecharge().add(amassRecharge);
    }
}
