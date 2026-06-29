package com.kuocai.cdn.api.kingsoft.cdn.model;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;
import java.util.List;

@Data
public class GetCertificatesResult {

    @JSONField(name = "Certificates")
    private List<Certificate> certificates;

    @JSONField(name = "PageNum")
    private Long pageNum;

    @JSONField(name = "PageSize")
    private Long pageSize;

    @JSONField(name = "TotalCount")
    private Long totalCount;

    @Data
    public static class Certificate {
        @JSONField(name = "CertificateName")
        private String certificateName;

        @JSONField(name = "CertificateId")
        private String certificateId;

        @JSONField(name = "Enable")
        private String enable;

        @JSONField(name = "IssueDomain")
        private String issueDomain;

        @JSONField(name = "IssueTime")
        private String issueTime; // 注意：这是字符串形式的Unix时间戳

        @JSONField(name = "ExpirationTime")
        private String expirationTime; // 注意：这是字符串形式的Unix时间戳

        @JSONField(name = "CertificateContent")
        private String certificateContent;

        @JSONField(name = "CertificateType")
        private String certificateType;

        @JSONField(name = "ConfigDomainNames")
        private String configDomainNames;
    }
}