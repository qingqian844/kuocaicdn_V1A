package com.kuocai.cdn.api.tencent.cdn.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 腾讯CDN加速服务状态
 */
@Getter
@AllArgsConstructor
public enum TencentDomainStatus {
    /**
     * 已启动
     */
    ONLINE("online"),
    /**
     * 已关闭
     */
    OFFLINE("offline"),
    /**
     * 关闭中
     */
    CLOSING("closing"),
    /**
     * 部署中
     */
    PROCESSING("processing"),
    /**
     * 域名审核未通过，域名备案过期/被注销导致
     */
    REJECTED("rejected");

    /**
     * 状态名称
     */
    private final String name;

    public static TencentDomainStatus convert(String name) {
        return Arrays.stream(TencentDomainStatus.values()).filter(e -> name.equals(e.getName())).findFirst().orElse(TencentDomainStatus.PROCESSING);
    }
}
