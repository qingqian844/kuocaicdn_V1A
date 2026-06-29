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
public class TopUri {

    private List<DataNum> reqNumResult;
    private List<DataNum> fluxResult;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DataNum {

        private String url;

        private Long value;
    }

}
