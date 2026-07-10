package com.kuocai.cdn.common.mongo.entity;

import com.kuocai.cdn.util.KuocaiDateUtil;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Data
@Document(collection = "flow_billing_carry")
public class FlowBillingCarry {

    public FlowBillingCarry() {
        this.pendingFlow = 0L;
        this.pendingAmount = BigDecimal.ZERO;
        this.createTime = KuocaiDateUtil.getCurrentTime();
        this.updateTime = this.createTime;
    }

    @Id
    private String id;
    private Long userId;
    private Long pendingFlow;
    private BigDecimal pendingAmount;
    private String firstStartTime;
    private String lastEndTime;
    private String createTime;
    private String updateTime;
}
