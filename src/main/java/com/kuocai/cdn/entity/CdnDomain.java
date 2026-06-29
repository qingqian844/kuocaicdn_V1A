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

/**
 * 加速域名(CdnDomain)实体类
 *
 * @author XUEW
 * @since 2023-03-06 13:43:40
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("cdn_domain")
public class CdnDomain implements Serializable {

    private static final long serialVersionUID = 915491866068629459L;

    /**
     * 主键ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 用户ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /**
     * 加速域名ID
     */
    private String domainId;

    /**
     * 加速域名
     */
    private String domainName;

    /**
     * 域名业务类型
     */
    private String businessType;

    /**
     * 域名服务范围
     */
    private String serviceArea;

    /**
     * 加速域名状态
     */
    private String domainStatus;

    /**
     * 腾讯云DNS解析ID
     */
    private Long tencentDnsId;

    /**
     * 域名路线
     */
    private String route;

    /**
     * 系统生成的CNAME
     */
    private String cname;

    /**
     * 华为CNAME
     */
    private String cnameHuawei;

    /**
     * 火山CNAME
     */
    private String cnameVolcengine;

    /**
     * 易凡CNAME
     */
    private String cnameYifan;

    /**
     * 腾讯CNAME
     */
    private String cnameTencent;

    /**
     * Cdnetworks CNAME
     */
    private String cnameCdnetworks;

    /**
     * 阿里云 CNAME
     */
    private String cnameAliyun;

    /**
     * 百度 CNAME
     */
    private String cnameBaidu;

    /**
     * 网宿 CNAME
     */
    private String cnameWangsu;

    /**
     * 金山云 CNAME
     */
    private String cnameKingsoft;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改时间
     */
    private Date updateTime;
}
