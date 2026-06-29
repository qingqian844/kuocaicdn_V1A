package com.kuocai.cdn.api.huawei.cdn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * User-Agent黑白名单
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class UserAgentBlackAndWhiteListDTO {

    /**
     * 设置黑白名单和开启状态
     * 0 关闭
     * 1 黑名单
     * 2 白名单
     */
    private Integer type;

    /**
     * 设置规则
     */
    private List<String> ua_list;
}
