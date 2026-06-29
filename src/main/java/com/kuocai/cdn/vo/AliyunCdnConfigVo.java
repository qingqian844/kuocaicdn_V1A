package com.kuocai.cdn.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AliyunCdnConfigVo {
    private String projectName;
    private String accessKeyId;
    private String accessKeySecret;
}
