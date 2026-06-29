package com.kuocai.cdn.api.qiniu.cdn.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TuneVo {

    private String startDate;

    private String endDate;

    /**
     * 粒度，取值：5min ／ hour ／day
     */
    private String granularity;

    /**
     * 域名列表，以 ；分割
     */
    private String domains;

}
