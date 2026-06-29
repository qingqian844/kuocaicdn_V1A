package com.kuocai.cdn.api.huawei.cdn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UrlAuthDTO {
    private String status;
    private String type;
    private String primary_key;
    private String secondary_key;
    private Long expire_time;
}
