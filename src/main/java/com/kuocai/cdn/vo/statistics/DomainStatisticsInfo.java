package com.kuocai.cdn.vo.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DomainStatisticsInfo {

    // 网络资源消耗统计信息
    private ResourceStatistics resouceStatistics;

    // 访问情况统计信息
    private VisitsStatistics visitsStatistics;

    // HTTP状态码统计信息
    private HttpCodeStatusStatistics httpCodeStatusStatistics;

    // TOP URI统计信息
    private TopUri topUri;
}
