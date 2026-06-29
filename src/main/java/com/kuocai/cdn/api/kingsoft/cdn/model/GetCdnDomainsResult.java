package com.kuocai.cdn.api.kingsoft.cdn.model;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class GetCdnDomainsResult {

    @JSONField(name = "PageNumber")
    private Integer pageNumber;

    @JSONField(name = "PageSize")
    private Integer pageSize;

    @JSONField(name = "TotalCount")
    private Integer totalCount;

    @JSONField(name = "Domains")
    private List<DomainInfo> domains;

    @Data
    @ToString
    public static class DomainInfo {
        @JSONField(name = "DomainName")
        private String domainName;

        @JSONField(name = "DomainId")
        private String domainId;

        @JSONField(name = "Cname")
        private String cname;

        @JSONField(name = "CdnType")
        private String cdnType;

        @JSONField(name = "IcpRegistration")
        private String icpRegistration;

        @JSONField(name = "DomainStatus")
        private String domainStatus;

        @JSONField(name = "CreatedTime")
        private String createdTime;

        @JSONField(name = "ModifiedTime")
        private String modifiedTime;

        @JSONField(name = "Description")
        private String description;

        @JSONField(name = "Region")
        private String region;
    }
}