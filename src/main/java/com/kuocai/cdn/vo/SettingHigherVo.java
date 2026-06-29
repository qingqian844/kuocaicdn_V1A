package com.kuocai.cdn.vo;

import com.kuocai.cdn.api.huawei.cdn.dto.CompressDTO;
import com.kuocai.cdn.api.huawei.cdn.dto.ErrorCodeRedirectRulesDTO;
import com.kuocai.cdn.api.huawei.cdn.dto.HttpResponseHeaderDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 域名管理-高级配置VO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SettingHigherVo {

    /**
     * 加速域名ID
     */
    private String doMainId;

    /**
     * 智能压缩
     */
    private CompressDTO compress;

    /**
     * HTTP header配置信息
     */
    private List<HttpResponseHeaderDTO> httpResponseHeaders;

    /**
     * 自定义错误页面配置信息
     */
    private List<ErrorCodeRedirectRulesDTO> errorCodeRedirectRules;
    
    /**
     * 自定义错误页面配置信息（通用格式，支持金山云等）
     */
    private List<ErrorPage> errorPages;
    
    /**
     * 通用错误页面配置
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
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
