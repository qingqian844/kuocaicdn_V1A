package com.kuocai.cdn.api.kingsoft.cdn.model;

import lombok.Data;

import java.util.List;

/**
 * 获取自定义错误页面配置结果
 */
@Data
public class GetErrorPageConfigResult {
    
    /**
     * 域名ID
     */
    private String domainId;
    
    /**
     * 错误页面配置列表
     */
    private List<ErrorPageConfigInfo> errorPages;
    
    @Data
    public static class ErrorPageConfigInfo {
        /**
         * 错误HTTP状态码
         */
        private String errorHttpCode;
        
        /**
         * 自定义错误页面URL
         */
        private String customPageUrl;
        
        /**
         * 配置状态
         */
        private String status;
        
        /**
         * 创建时间
         */
        private String createTime;
        
        /**
         * 更新时间
         */
        private String updateTime;
    }
}