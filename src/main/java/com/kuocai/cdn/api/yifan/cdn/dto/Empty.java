package com.kuocai.cdn.api.yifan.cdn.dto;

import com.kuocai.cdn.api.yifan.cdn.enums.Type;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 预校验配置
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Empty {
    /**
     * 校验关联账号
     */
    private String accountId;
    /**
     * 校验节点类型
     */
    private Type type;

    private Boolean skipCheck;

    private String domainUserId;
}
