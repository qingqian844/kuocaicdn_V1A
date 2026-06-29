package com.kuocai.cdn.vo;

import com.kuocai.cdn.entity.SysUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserVo extends SysUser {

    /**
     * 验证码
     */
    private String code;

    /**
     * 推荐码
     */
    private String recommend;
}
