package com.kuocai.cdn.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TencentEdgeOneApiConfigVo {

    private String projectName;

    private String tagValue;

    private String secretId;

    private String secretKey;

    private String planId;

    private String zoneId;
}
