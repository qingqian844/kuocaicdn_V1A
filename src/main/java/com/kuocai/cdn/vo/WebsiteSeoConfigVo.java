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
public class WebsiteSeoConfigVo {

    /**
     * 首页标题
     */
    private String homeTitle;

    /**
     * 关键字
     */
    private String searchKeyword;

    /**
     * 网站说明
     */
    private String websiteInfo;

}
