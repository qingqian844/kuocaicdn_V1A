package com.kuocai.cdn.config;

import com.kuocai.cdn.vo.*;

/**
 * @author xiaobo
 * @date 2023/4/5
 */

public class SystemConfig {

    /**
     * 网站基本配置
     */
    public static WebsiteBaseConfigVo websiteBaseConfig;

    /**
     * 网站权限配置
     */
    public static WebsitePermissionConfigVo websitePermissionConfig;

    /**
     * 网站用户协议
     */
    public static WebsiteAgreementConfigVo websiteAgreementConfig;

    /**
     * DNS configuration for system CNAME records.
     */
    public static DnsConfigVo dnsConfig;

    /**
     * Home page custom frontend code.
     */
    public static WebsiteHomeCodeConfigVo websiteHomeCodeConfig;

    /**
     * Footer custom frontend code.
     */
    public static WebsiteFooterCodeConfigVo websiteFooterCodeConfig;

    /**
     * SEO config.
     */
    public static WebsiteSeoConfigVo websiteSeoConfig;

    /**
     * 支付宝配置
     */
    public static AliPayConfigVo aliPayConfig;

    /**
     * 支付宝提现配置
     */
    public static AliWithdrawConfigVo aliWithdrawConfig;

    /**
     * 微信支付配置
     */
    public static WeChatConfigVo weChatConfig;

    /**
     * 微信扫码登录配置
     */
    public static WeChatCodeConfigVo weChatCodeConfig;

    /**
     * 支付宝认证配置
     */
    public static AlipayAuthenticationConfigVo alipayAuthenticationConfig;

    /**
     * 邮箱模板配置
     */
    public static EmailTemplateVo emailTemplateConfig;

    /**
     * 邮箱服务配置
     */
    public static EmailConfigVo emailConfig;

    /**
     * 短信模板配置
     */
    public static SmsTemplateVo smsTemplateConfig;

    /**
     * 短信服务配置
     */
    public static SmsConfigVo smsConfig;

    /**
     * 华为API 配置
     */
    public static HuaWeiCloudApiConfigVo huaWeiCloudApiConfig;

    /**
     * 火山API 配置
     */
    public static VolcanicCloudApiConfigVo volcanicCloudApiConfig;

    /**
     * 白山API 配置
     */
    public static WhiteMountainCloudApiConfigVo whiteMountainCloudApiConfigVo;

    /**
     * 腾讯云API 配置
     */
    public static TencentCloudApiConfigVo tencentCloudApiConfigVo;

    public static TencentEdgeOneApiConfigVo tencentEdgeOneApiConfigVo;

    /**
     * CDNetworks API 配置
     */
    public static CDNetworksApiConfigVo cdnetworksApiConfigVo;

    /**
     * 阿里云CDN配置
     */
    public static AliyunCdnConfigVo aliyunCdnConfig;

    /**
     * 网宿CDN配置
     */
    public static WangsuCdnConfigVo wangsuCdnConfig;

    /**
     * 百度CDN配置
     */
    public static BaiduCdnConfigVo baiduCdnConfig;

    /**
     * 融合CDN配置
     */
    public static MergeCdnApiConfigVo mergeCdnApiConfig;

    /**
     * 金山云CDN配置
     */
    public static KingsoftCdnConfigVo kingsoftCdnConfig;

}

