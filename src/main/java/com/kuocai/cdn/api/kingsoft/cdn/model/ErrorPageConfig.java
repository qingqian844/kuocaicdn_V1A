package com.kuocai.cdn.api.kingsoft.cdn.model;

import lombok.Data;

import java.util.List;

/**
 * 金山云CDN自定义错误页面配置
 */
@Data
public class ErrorPageConfig {
    
    /**
     * 域名ID
     */
    private String domainId;
    
    /**
     * 错误页面配置列表
     */
    private List<ErrorPage> errorPages;
    
    @Data
    public static class ErrorPage {
        /**
         * 错误HTTP状态码
         * 支持状态码: 400,403,404,405,406,414,416,500,501,502,503,504
         */
        private String errorHttpCode;
        
        /**
         * 自定义错误页面URL
         * 必须以 https:// 或 http:// 开头
         */
        private String customPageUrl;
    }
}