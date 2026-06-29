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
public class ReqCountVo {

    /**
     * 	域名列表，总数不超过100条
     */
    private List<String> domains;
    private String freq;
    private String region;
    private String startDate;
    private String endDate;
}
