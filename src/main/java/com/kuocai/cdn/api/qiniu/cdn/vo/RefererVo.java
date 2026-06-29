package com.kuocai.cdn.api.qiniu.cdn.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RefererVo {

    /**
     * Referer防盗链类型： black/white
     */
    private String refererType;

    /**
     * Referer防盗链黑白名单
     */
    private List<String> refererValues;

    /**
     * Referer防盗链, 是否支持空referer，不填为false
     */
    private Boolean nullReferer;
}
