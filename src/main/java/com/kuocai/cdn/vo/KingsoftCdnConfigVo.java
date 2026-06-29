package com.kuocai.cdn.vo;

import lombok.Data;

/**
 * 金山云CDN配置VO
 * 
 * @author system
 * @date 2025/07/16
 */
@Data
public class KingsoftCdnConfigVo {

    /**
     * 金山云Access Key
     */
    private String accessKey;

    /**
     * 金山云Secret Key
     */
    private String secretKey;

    /**
     * API端点地址，默认为 http://cdn.api.ksyun.com
     */
    private String endpoint = "http://cdn.api.ksyun.com";

    /**
     * 区域，默认为 cn-shanghai-1
     */
    private String region = "cn-shanghai-1";

    /**
     * 服务名称，默认为 cdn
     */
    private String serviceName = "cdn";

    private String projectId;
}
