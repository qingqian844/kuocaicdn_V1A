package com.kuocai.cdn.entity;

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
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("edgeone_root_domain_record")
public class EdgeOneRootDomainRecord implements Serializable {

    private static final long serialVersionUID = 8939351018856695285L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String rootDomain;

    private String firstDomainName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long cdnDomainId;

    private String status;

    private Date createTime;

    private Date updateTime;
}
