package com.kuocai.cdn.component;

import com.kuocai.cdn.entity.SysUser;

/**
 *
 * @date 2025-08-28
 */
public final class UserContextHolder {

    /**
     */
    private static final ThreadLocal<SysUser> USER_THREAD_LOCAL = new ThreadLocal<>();

    /**
     */
    private UserContextHolder() {
    }

    /**
     */
    public static void setUser(SysUser user) {
        USER_THREAD_LOCAL.set(user);
    }

    /**
     */
    public static SysUser getUser() {
        return USER_THREAD_LOCAL.get();
    }

    /**
     *
     */
    public static Long getUserId() {
        SysUser user = getUser();
        return user != null ? user.getId() : null;
    }

    /**
     *
     */
    public static String getUserRoleCode() {
        SysUser user = getUser();
        if (user == null || user.getRoleId() == null) {
            return null;
        }
        return user.getRoleId() == 1 ? "admin" : "user";
    }

    /**
     */
    public static void clear() {
        USER_THREAD_LOCAL.remove();
    }

}