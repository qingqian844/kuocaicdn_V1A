package com.kuocai.cdn.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 加速域名(CdnDomain)实体类
 *
 * @author XUEW
 * @since 2023-02-26 23:30:24
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CdnDomainVo {

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
     * 用户名
     */
    private String userName;

    /**
     * 用户名
     */
    private String userImg;

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
     * 域名业务类型
     */
    private String businessTypeName;

    /**
     * 域名业务类型ID
     */
    private Integer businessTypeId;

    /**
     * 源站类型ID
     */
    private Integer originTypeId;

    /**
     * 源站IP
     */
    private String originIp;

    /**
     * 源站域名
     */
    private String originDomain;

    /**
     * 域名服务范围
     */
    private String serviceArea;

    /**
     * 域名服务范围
     */
    private String serviceAreaName;

    /**
     * 域名服务范围ID
     */
    private Integer serviceAreaId;

    /**
     * 加速域名状态
     */
    private String domainStatus;

    /**
     * 加速域名状态
     */
    private String domainStatusName;

    /**
     * 腾讯云CNAME
     */
    private String tencentCname;

    /**
     * 腾讯云DNS解析ID
     */
    private Long tencentDnsId;

    /**
     * 加速域名对应的CNAME
     */
    private String cname;

    /**
     * 是否开启HTTPS加速
     */
    private Integer httpsStatus;

    /**
     * 封禁状态（0代表未禁用；1代表禁用）
     */
    private Integer disabled;

    /**
     * 域名禁用原因。 1：该域名涉嫌违规内容（涉黄/涉赌/涉毒/涉政）已被禁用； 2：该域名因备案失效已被禁用； 3：该域名遭受攻击，已被禁用； 150：该域名涉嫌违规内容涉黄已被禁用； 151：该域名涉嫌违规内容涉政已被禁用； 152：该域名涉嫌违规内容涉暴已被禁用； 153：该域名涉嫌违规内容涉赌已被禁用。
     */
    private String bannedReason;

    /**
     * 锁定状态（0代表未锁定；1代表锁定）
     */
    private Integer locked;

    /**
     * 域名锁定原因（Changing the config, please wait）。
     */
    private String lockedReason;

    /**
     * range状态（"off"/"on"）
     */
    private String rangeStatus;

    /**
     * follow302状态（"off"/"on"）
     */
    private String followStatus;

    /**
     * 是否暂停源站回源
     */
    private String originStatus;

    /**
     * 自动刷新预热（0代表关闭；1代表打开）
     */
    private Integer autoRefreshPreheat;

    /**
     * CDN提供商
     */
    private String cdnSupplier;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改时间
     */
    private Date updateTime;

    /**
     * 节点路线
     */
    private String route;
}
