package com.kuocai.cdn.api.yifan.cdn.dto;

// DomainDTO.java

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 创建域名
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DomainDTO {
    /**
     * 加速类型
     */
    private String businessType;
    /**
     * 域名
     */
    private String domainName;
    /**
     * 预校验配置
     */
    private List<Empty> domainSources;
    /**
     * 线路id
     */
    private String lineId;
    /**
     * 加速服务类型，目前只有国内加速
     */
    private String serviceArea;
    /**
     * 源站配置
     */
    private List<SourceElement> sources;
}






