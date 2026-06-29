package com.kuocai.cdn.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WangsuCdnConfigVo {
    private String accessKey = "";
    private String secretKey = "";
}
