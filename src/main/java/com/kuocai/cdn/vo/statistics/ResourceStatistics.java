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
public class ResourceStatistics {
    private ResourceSummary resource_summary;

    private ResourceDetail resource_detail;

    private List<String> labels;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ResourceSummary {

        /**
         * 访问带宽
         */
        private long bw;

        /**
         * 回源带宽
         */
        private long bs_bw;

        /**
         * 访问流量
         */
        private long flux;

        /**
         * 回源流量
         */
        private long bs_flux;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ResourceDetail {

        private List<Long> bw;

        private List<Long> bs_bw;

        private List<Long> flux;

        private List<Long> bs_flux;
    }
}
