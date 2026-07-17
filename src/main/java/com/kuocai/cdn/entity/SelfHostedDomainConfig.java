package com.kuocai.cdn.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
@TableName("self_hosted_domain_config")
public class SelfHostedDomainConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long cdnDomainId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long nodeGroupId;
    private String originType;
    private String originAddress;
    private String originProtocol;
    private Integer httpPort;
    private Integer httpsPort;
    private String originHost;
    private String originConfigJson;
    private String cacheConfigJson;
    @JsonIgnore
    private String accessConfigCipher;
    private String advancedConfigJson;
    private String httpsConfigJson;
    private Integer ipv6Enabled;
    private Integer httpsEnabled;
    @JsonIgnore
    private String certificateCipher;
    @JsonIgnore
    private String privateKeyCipher;
    private String forceRedirect;
    private Long desiredConfigVersion;
    private String status;
    private String lastError;
    private Date createTime;
    private Date updateTime;
}
