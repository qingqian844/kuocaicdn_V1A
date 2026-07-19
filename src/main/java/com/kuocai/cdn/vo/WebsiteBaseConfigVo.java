package com.kuocai.cdn.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author xiaobo
 * @date 2023/4/1
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebsiteBaseConfigVo {

    /**
     * 网站名称
     */
    private String websiteName;

    /**
     * 网站公告
     */
    private String websiteAnnouncement;

    /**
     * 默认流量价格
     */
    private BigDecimal defaultFlowPrice;

    /**
     * 备案号
     */
    private String icpNumber;

    /**
     * 网站图标
     */
    private String websiteIconImg;

    /**
     * 未设置自定义头像时使用的默认头像
     */
    private String defaultAvatarImg;

    /**
     * 管理员登录地址后缀
     */
    private String adminPath;

    /**
     * 网站Logo
     */
    private String websiteLogoImg;

    /**
     * 页尾公众号二维码
     */
    private String wechatQrCodeImg;

    /**
     * 页尾QQ群二维码
     */
    private String qqGroupQrCodeImg;

    private Integer expireTime;

    /**
     * 更新时间
     */
    private String updateTime;

    /**
     * 普通用户最多创建域名数量
     */
    private Integer maxDomainCount;

    /**
     * 代理用户最多创建域名数量
     */
    private Integer maxDomainCountProxy;

    /**
     * 邀请注册奖励
     */
    private Integer inviteRewardGb;

    /**
     * 受邀请注册奖励
     */
    private Integer invitedRewardGb;

    /**
     * 每月赠送流量
     */
    private Integer monthGiftGb;

    private Boolean edgeoneDomainQuotaEnabled;

    private Integer edgeoneFreeDomainQuota;

    private BigDecimal edgeoneDomainQuotaPrice;

    private Integer edgeoneDomainQuotaValidDays;

    private String defaultUserRoute;

    private List<String> mainlandEnabledTargets;

    private List<String> overseasEnabledRoutes;

    private List<String> globalEnabledRoutes;

    private List<String> overseasEnabledTargets;

    private List<String> globalEnabledTargets;

    private String mainlandRouteMode;

    private String overseasRouteMode;

    private String globalRouteMode;

    private Boolean httpsRequestFeeEnabled;

    private String httpsRequestFeeRoutes;

    private Long httpsRequestFeeUnitCount;

    private BigDecimal httpsRequestFeeUnitPrice;
}
