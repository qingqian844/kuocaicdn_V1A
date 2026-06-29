package com.kuocai.cdn.api.kingsoft.cdn.model;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class GetCdnDomainBasicInfoResult {

    @JSONField(name = "DomainName")
    private String domainName;

    @JSONField(name = "DomainId")
    private String domainId;

    @JSONField(name = "Cname")
    private String cname;

    @JSONField(name = "DomainStatus")
    private String domainStatus;

    @JSONField(name = "CdnType")
    private String cdnType;

    @JSONField(name = "Regions")
    private String regions;

    @JSONField(name = "OriginType")
    private String originType;

    @JSONField(name = "OriginProtocol")
    private String originProtocol;

    @JSONField(name = "Origin")
    private String origin;

    @JSONField(name = "OriginHttpPort")
    private Integer originHttpPort;

    @JSONField(name = "OriginHttpsPort")
    private Integer originHttpsPort;

    @JSONField(name = "CreatedTime")
    private String createdTime;

    @JSONField(name = "ModifiedTime")
    private String modifiedTime;
} 