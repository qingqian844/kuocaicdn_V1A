package com.kuocai.cdn.component;

import com.kuocai.cdn.api.aliyun.cdn.AliyunCdnClientFactory;
import com.kuocai.cdn.api.baidu.cdn.properties.BaiduCdn;
import com.kuocai.cdn.api.baishan.cdn.properties.BsCdn;
import com.kuocai.cdn.api.cdnetworks.cdn.properties.CdnetworksCdn;
import com.kuocai.cdn.api.huawei.cdn.properties.HuaWeiCdn;
import com.kuocai.cdn.api.kingsoft.cdn.properties.KingsoftCdn;
import com.kuocai.cdn.api.tencent.cdn.properties.TencentCdn;
import com.kuocai.cdn.api.tencent.dns.properties.TencentDns;
import com.kuocai.cdn.api.volcengine.cdn.properties.VolcengineCdn;
import com.kuocai.cdn.api.wangsu.cdn.properties.WangsuCdn;
import com.kuocai.cdn.config.SystemConfig;
import com.kuocai.cdn.constant.ConfigBizTypeConstants;
import com.kuocai.cdn.service.SysConfigService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 预加载配置
 */
@Slf4j
@Component
public class PreloadComponent {

    PreloadComponent(SysConfigService sysConfigService) {
        this.sysConfigService = sysConfigService;
    }

    private final SysConfigService sysConfigService;

    @PostConstruct
    private void initSystemConfig() {
        log.info("↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓ 加载系统配置 ↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓");
        loadWebsiteBaseConfig();
        loadWebsitePermissionConfig();
        loadAuthenticationConfig();
        loadPayConfig();
        loadWechatCodeConfig();
        loadEmailConfig();
        loadSmsConfig();
        loadDnsConfig();
        loadCdnApiConfig();
        log.info("↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑ 加载系统配置完成 ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑");
    }

    /**
     * 加载系统基本配置
     */
    public void loadWebsiteBaseConfig() {
        log.info("========== 加载系统基本配置 ==========");
        SystemConfig.websiteBaseConfig = sysConfigService.getConfigContentVo(WebsiteBaseConfigVo.class, ConfigBizTypeConstants.WEBSITE_BASE_CONFIG);
        SystemConfig.websiteAgreementConfig = sysConfigService.getConfigContentVo(WebsiteAgreementConfigVo.class, ConfigBizTypeConstants.WEBSITE_AGREEMENT_CONFIG);
        SystemConfig.websiteHomeCodeConfig = sysConfigService.getPlainConfigContentVo(WebsiteHomeCodeConfigVo.class, ConfigBizTypeConstants.WEBSITE_HOME_CODE_CONFIG);
        SystemConfig.websiteFooterCodeConfig = sysConfigService.getPlainConfigContentVo(WebsiteFooterCodeConfigVo.class, ConfigBizTypeConstants.WEBSITE_FOOTER_CODE_CONFIG);
        loadWebsiteSeoConfig();
    }

    /**
     * 加载SEO配置
     */
    public void loadWebsiteSeoConfig() {
        log.info("========== 加载SEO配置 ==========");
        SystemConfig.websiteSeoConfig = sysConfigService.getConfigContentVo(WebsiteSeoConfigVo.class, ConfigBizTypeConstants.WEBSITE_SEO_CONFIG);
    }

    /**
     * 加载系统权限配置
     */
    public void loadWebsitePermissionConfig() {
        log.info("========== 加载系统权限配置 ==========");
        SystemConfig.websitePermissionConfig = sysConfigService.getConfigContentVo(WebsitePermissionConfigVo.class, ConfigBizTypeConstants.WEBSITE_PERMISSION_CONFIG);
    }

    /**
     * 加载系统支付配置
     */
    public void loadPayConfig() {
        log.info("========== 加载系统支付配置 ==========");
        SystemConfig.aliPayConfig = sysConfigService.getConfigContentVo(AliPayConfigVo.class, ConfigBizTypeConstants.ALIPAY_CONFIG);
        SystemConfig.weChatConfig = sysConfigService.getConfigContentVo(WeChatConfigVo.class, ConfigBizTypeConstants.WECHAT_CONFIG);
        SystemConfig.aliWithdrawConfig = sysConfigService.getConfigContentVo(AliWithdrawConfigVo.class, ConfigBizTypeConstants.ALI_WITHDRAW_CONFIG);
    }

    /**
     * 加载微信扫码登录配置
     */
    public void loadWechatCodeConfig() {
        log.info("========== 加载微信扫码登录配置 ==========");
        SystemConfig.weChatCodeConfig = sysConfigService.getConfigContentVo(WeChatCodeConfigVo.class, ConfigBizTypeConstants.WECHAT_CODE_CONFIG);
    }

    /**
     * 加载系统实名配置
     */
    public void loadAuthenticationConfig() {
        log.info("========== 加载系统实名配置 ==========");
        SystemConfig.alipayAuthenticationConfig = sysConfigService.getConfigContentVo(AlipayAuthenticationConfigVo.class, ConfigBizTypeConstants.ALIPAY_AUTHENTICATION_CONFIG);
    }

    /**
     * 加载CDN API 配置
     */
    public void loadDnsConfig() {
        log.info("========== 加载DNS解析配置 ==========");
        SystemConfig.dnsConfig = sysConfigService.getConfigContentVo(DnsConfigVo.class, ConfigBizTypeConstants.DNS_CONFIG);
        if (!Assert.isEmpty(SystemConfig.dnsConfig)) {
            TencentDns.applyConfiguration(
                    SystemConfig.dnsConfig.getSecretId(),
                    SystemConfig.dnsConfig.getSecretKey(),
                    SystemConfig.dnsConfig.getPrimaryDomain()
            );
        }
    }

    public void loadCdnApiConfig() {
        log.info("========== 加载CDN配置 ==========");
        SystemConfig.huaWeiCloudApiConfig = sysConfigService.getConfigContentVo(HuaWeiCloudApiConfigVo.class, ConfigBizTypeConstants.HUAWEI_CLOUD_API_CONFIG);
        HuaWeiCdn.ACCESS_KEY = SystemConfig.huaWeiCloudApiConfig.getHuaWeiCloudAk();
        HuaWeiCdn.SECRET_ACCESS_KEY = SystemConfig.huaWeiCloudApiConfig.getHuaWeiCloudSk();
        SystemConfig.volcanicCloudApiConfig = sysConfigService.getConfigContentVo(VolcanicCloudApiConfigVo.class, ConfigBizTypeConstants.VOLCANIC_CLOUD_API_CONFIG);
        VolcengineCdn.Project = VolcengineCdn.normalizeProject(SystemConfig.volcanicCloudApiConfig.getVolcanicCloudProjectName());
        VolcengineCdn.AK = SystemConfig.volcanicCloudApiConfig.getVolcanicCloudAk();
        VolcengineCdn.SK = SystemConfig.volcanicCloudApiConfig.getVolcanicCloudSk();
        SystemConfig.whiteMountainCloudApiConfigVo = sysConfigService.getConfigContentVo(WhiteMountainCloudApiConfigVo.class, ConfigBizTypeConstants.WHITE_MOUNTAIN_CLOUD_API_CONFIG);
        if (!Assert.isEmpty(SystemConfig.whiteMountainCloudApiConfigVo)) {
            BsCdn.API = SystemConfig.whiteMountainCloudApiConfigVo.getWhiteMountainCloudBaseApi();
            BsCdn.TOKEN = SystemConfig.whiteMountainCloudApiConfigVo.getWhiteMountainCloudToken();
        }
        SystemConfig.tencentCloudApiConfigVo = sysConfigService.getConfigContentVo(TencentCloudApiConfigVo.class, ConfigBizTypeConstants.TENCENT_CLOUD_API_CONFIG);
        if (!Assert.isEmpty(SystemConfig.tencentCloudApiConfigVo)) {
            TencentCdn.SecretId = SystemConfig.tencentCloudApiConfigVo.getTencentCloudSecretId();
            TencentCdn.SecretKey = SystemConfig.tencentCloudApiConfigVo.getTencentCloudSecretKey();
        }
        SystemConfig.tencentEdgeOneApiConfigVo = sysConfigService.getConfigContentVo(TencentEdgeOneApiConfigVo.class, ConfigBizTypeConstants.TENCENT_EDGEONE_API_CONFIG);
        SystemConfig.cdnetworksApiConfigVo = sysConfigService.getConfigContentVo(CDNetworksApiConfigVo.class, ConfigBizTypeConstants.CDNETWORKS_API_CONFIG);
        if (!Assert.isEmpty(SystemConfig.cdnetworksApiConfigVo)) {
            CdnetworksCdn.AccessKey = SystemConfig.cdnetworksApiConfigVo.getCdnetworksAccessKey();
            CdnetworksCdn.SecretKey = SystemConfig.cdnetworksApiConfigVo.getCdnetworksSecretKey();
        }
        SystemConfig.aliyunCdnConfig = sysConfigService.getConfigContentVo(AliyunCdnConfigVo.class, ConfigBizTypeConstants.ALIYUN_CDN_CONFIG);
        if (!Assert.isEmpty(SystemConfig.aliyunCdnConfig)) {
            AliyunCdnClientFactory.applyConfiguration(
                    SystemConfig.aliyunCdnConfig.getAccessKeyId(),
                    SystemConfig.aliyunCdnConfig.getAccessKeySecret()
            );
        }
        SystemConfig.wangsuCdnConfig = sysConfigService.getConfigContentVo(WangsuCdnConfigVo.class, ConfigBizTypeConstants.WANGSU_CDN_CONFIG);
        if (!Assert.isEmpty(SystemConfig.wangsuCdnConfig)) {
            WangsuCdn.AccessKey = SystemConfig.wangsuCdnConfig.getAccessKey();
            WangsuCdn.SecretKey = SystemConfig.wangsuCdnConfig.getSecretKey();
        }
        SystemConfig.baiduCdnConfig = sysConfigService.getConfigContentVo(BaiduCdnConfigVo.class, ConfigBizTypeConstants.BAIDU_CDN_CONFIG);
        if (!Assert.isEmpty(SystemConfig.baiduCdnConfig)) {
            BaiduCdn.AccessKeyId = SystemConfig.baiduCdnConfig.getAccessKeyId();
            BaiduCdn.SecretAccessKy = SystemConfig.baiduCdnConfig.getSecretAccessKey();
        }
        SystemConfig.kingsoftCdnConfig = sysConfigService.getConfigContentVo(KingsoftCdnConfigVo.class, ConfigBizTypeConstants.KINGSOFT_CDN_CONFIG);
        if (!Assert.isEmpty(SystemConfig.kingsoftCdnConfig)) {
            KingsoftCdn.applyConfiguration(
                    SystemConfig.kingsoftCdnConfig.getAccessKey(),
                    SystemConfig.kingsoftCdnConfig.getSecretKey(),
                    SystemConfig.kingsoftCdnConfig.getEndpoint(),
                    SystemConfig.kingsoftCdnConfig.getRegion(),
                    SystemConfig.kingsoftCdnConfig.getServiceName()
            );
        }
        SystemConfig.mergeCdnApiConfig = sysConfigService.getConfigContentVo(MergeCdnApiConfigVo.class, ConfigBizTypeConstants.MERGE_CDN_API_CONFIG);
    }


    /**
     * 加载邮箱配置
     */
    public void loadEmailConfig() {
        log.info("========== 加载邮箱配置 ==========");
        SystemConfig.emailTemplateConfig = sysConfigService.getConfigContentVo(EmailTemplateVo.class, ConfigBizTypeConstants.EMAIL_TEMPLATE_CONFIG);
        SystemConfig.emailConfig = sysConfigService.getConfigContentVo(EmailConfigVo.class, ConfigBizTypeConstants.EMAIL_CONFIG);
        EmailClient.fromEmail = SystemConfig.emailConfig.getSenderMailbox();
        EmailClient.fromTitle = SystemConfig.emailConfig.getSenderTitle();
        EmailClient.userName = SystemConfig.emailConfig.getSenderMailbox();
        EmailClient.password = SystemConfig.emailConfig.getAuthorizationPassword();
        EmailClient.host = SystemConfig.emailConfig.getSmtpServer();
        EmailClient.port = SystemConfig.emailConfig.getServerPort();
    }

    /**
     * 加载短信配置
     */
    public void loadSmsConfig() {
        log.info("========== 加载短信配置 ==========");
        SystemConfig.smsTemplateConfig = sysConfigService.getConfigContentVo(SmsTemplateVo.class, ConfigBizTypeConstants.SMS_TEMPLATE_CONFIG);
        SystemConfig.smsConfig = sysConfigService.getConfigContentVo(SmsConfigVo.class, ConfigBizTypeConstants.SMS_CONFIG);
        SmsClient.smsAppId = SystemConfig.smsConfig.getSdkAppId();
        SmsClient.smsAppKey = SystemConfig.smsConfig.getSecretKey();
        SmsClient.smsSign = SystemConfig.smsConfig.getSmsSign();
    }
}
