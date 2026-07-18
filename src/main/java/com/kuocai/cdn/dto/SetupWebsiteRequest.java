package com.kuocai.cdn.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SetupWebsiteRequest {
    private String websiteName;
    private String websiteLogoImg;
    private String websiteIconImg;
    private String icpNumber;
    private BigDecimal defaultFlowPrice;
    private Integer maxDomainCount;
    private Integer maxDomainCountProxy;
    private String defaultUserRoute;
}
