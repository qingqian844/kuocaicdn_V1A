package com.kuocai.cdn.api.yifan.cdn.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 域名回源
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Accessors(chain = true)
public class SourceElement {
    /**
     * 回原host
     */
    private String hostName;
    /**
     * http 端口
     */
    private Integer httpPort;
    /**
     * https 端口
     */
    private Integer httpsPort;
    /**
     * 回原ip或回原域名
     */
    private String ipOrDomain;
    /**
     * 加速域名回原地址类型
     */
    private String originType;
}