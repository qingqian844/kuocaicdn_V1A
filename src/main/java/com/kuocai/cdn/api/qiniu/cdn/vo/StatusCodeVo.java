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
public class StatusCodeVo {

    /**
     * 	域名列表，总数不超过100条
     */
    private List<String> domains;

    /**
     * 	粒度，可选项为 5min、1hour、1day
     */
    private String freq;
    private List<String> regions;
    private String startDate;
    private String endDate;

    /**
     * ISP运营商，比如all(所有 ISP)，telecom(电信)，unicom(联通)，mobile(中国移动)，drpeng(鹏博士)，tietong(铁通)，cernet(教育网)
     */
    private String isp;
}
