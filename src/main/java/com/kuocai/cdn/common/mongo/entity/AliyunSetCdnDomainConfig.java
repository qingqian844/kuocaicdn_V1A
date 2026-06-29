package com.kuocai.cdn.common.mongo.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "aliyun_set_cdn_domain_config")
public class AliyunSetCdnDomainConfig {

    public AliyunSetCdnDomainConfig() {
        this.promise = "pending";
    }

    @Id
    private String id;
    private String domain;
    private String functionNames;
    private String functions;
    /**
     * 任务状态 promise
     * rejected: 拒绝
     * resolved: 解决
     * pending: 待处理 (默认)
     * cancel: 取消
     */
    private String promise;
}
