package com.kuocai.cdn.api.volcengine.cdn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 域名操作
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class DomainBodyDTO {
    private String Domain;
    private String ServiceType;
    private String ServiceRegion;
}
