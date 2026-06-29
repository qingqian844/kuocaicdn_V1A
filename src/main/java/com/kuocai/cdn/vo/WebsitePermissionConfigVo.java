package com.kuocai.cdn.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author xiaobo
 * @date 2023/4/1
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebsitePermissionConfigVo {

    /**
     * 是否强制实名认证
     */
    private boolean forceRealAuthentication;

    /**
     * 是否强制绑定手机号
     */
    private boolean forceBindingTel;

    /**
     * 关闭注册功能
     */
    private boolean closeRegister;

}
