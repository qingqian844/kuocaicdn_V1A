package com.kuocai.cdn.dto;

import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.entity.SysUserAccount;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 用户列表记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class UserRecordDTO {

    /**
     * 原始用户记录
     */
    private SysUser user;

    /**
     * 用户账户
     */
    private SysUserAccount account;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 代理用户
     */
    private SysUser agentUser;

    /**
     * 被封禁
     */
    private boolean banned;

    /**
     * 线路
     */
    private String route;
}
