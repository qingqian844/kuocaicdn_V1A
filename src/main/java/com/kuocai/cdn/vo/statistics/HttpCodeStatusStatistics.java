package com.kuocai.cdn.vo.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HttpCodeStatusStatistics {

    private List<Long> status_summary;

    private List<Long> bs_status_summary;

    private List<Object[]> status_detail;

    private List<Object[]> bs_status_detail;

    private List<String> labels;
}
