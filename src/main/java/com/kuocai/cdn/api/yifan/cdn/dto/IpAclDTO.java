package com.kuocai.cdn.api.yifan.cdn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Accessors(chain = true)
public class IpAclDTO {

    /**
     * 关闭 0
     * 黑名单 1
     * 白名单 2
     */
    private Integer type;

    /**
     * 配置规则
     */
    private List<String> list;
}
