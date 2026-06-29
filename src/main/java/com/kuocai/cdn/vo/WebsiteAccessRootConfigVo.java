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
public class WebsiteAccessRootConfigVo {

    /**
     * 管理端访问根目录
     */
    private String serverRoot;

    /**
     * 客户端访问根目录
     */
    private String clientRoot;

    /**
     * 客户端地址
     */
    private String clientUrl;
}
