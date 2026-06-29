package com.kuocai.cdn.common.mongo.entity;

import com.kuocai.cdn.util.KuocaiDateUtil;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "flow_billing_logic")
public class FlowBillingLogic {

    public FlowBillingLogic() {
        this.promise = "pending";
        this.summary = 0L;
        this.createTime = KuocaiDateUtil.getCurrentTime();
        this.updateTime = this.createTime;
    }

    @Id
    private String id;
    private Long userId;
    private String domains;
    private Long summary;
    private String time;
    /**
     * 任务状态 promise
     * rejected: 拒绝
     * resolved: 解决
     * pending: 待处理 (默认)
     * cancel: 取消
     */
    private String promise;
    private String createTime;
    private String updateTime;
}
