package com.kuocai.cdn.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EdgeOneDomainQuotaSummaryVo {

    private Boolean enabled;

    private Integer freeQuota;

    private Integer paidQuota;

    private Integer packageQuota;

    private Integer totalQuota;

    private Integer usedQuota;

    private Integer remainingQuota;

    private BigDecimal unitPrice;

    private Integer quotaValidDays;

    private Boolean overQuota;
}
