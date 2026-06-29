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
public class VisitsStatistics {

    private VisitsSummary visits_summary;

    private VisitsDetail visits_detail;

    private List<String> labels;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VisitsSummary {

        /**
         * 请求总数
         */
        private Long req_num;

        /**
         * 命中流量
         */
        private Long hit_flux;

        /**
         * 请求命中次数
         */
        private Long hit_num;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VisitsDetail {

        /**
         * 请求总数
         */
        private List<Long> req_num;

        /**
         * 命中流量
         */
        private List<Long> hit_flux;

        /**
         * 请求命中次数
         */
        private List<Long> hit_num;
    }
}
