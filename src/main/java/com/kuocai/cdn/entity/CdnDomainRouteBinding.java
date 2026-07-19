package com.kuocai.cdn.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("cdn_domain_route_binding")
public class CdnDomainRouteBinding implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long domainId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String domainName;

    private String serviceArea;

    private String route;

    private String targetKey;

    private String upstreamDomainId;

    private String upstreamCname;

    private String domainSnapshotJson;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long localDomainId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long dnsRecordId;

    private Integer primaryBinding;

    private String status;

    private Date createTime;

    private Date updateTime;
}
