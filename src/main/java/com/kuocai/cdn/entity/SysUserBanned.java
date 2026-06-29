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
 * 用户禁用表
 *
 * @TableName sys_user_banned
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("sys_user_banned")
@Builder
public class SysUserBanned implements Serializable {

    private static final long serialVersionUID = 7561046430868339163L;

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
     * 禁用原因
     */
    private String bannedReason;

    /**
     * 禁用时间
     */
    private Date bannedTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改时间
     */
    private Date updateTime;
}
