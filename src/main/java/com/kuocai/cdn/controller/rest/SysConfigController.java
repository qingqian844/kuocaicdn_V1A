package com.kuocai.cdn.controller.rest;

import cn.hutool.core.date.DateUtil;
import com.kuocai.cdn.annotation.AuthorLimiter;
import com.kuocai.cdn.annotation.RateLimiter;
import com.kuocai.cdn.annotation.SysLog;
import com.kuocai.cdn.component.OssClient;
import com.kuocai.cdn.component.PreloadComponent;
import com.kuocai.cdn.constant.ConfigBizTypeConstants;
import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.dto.resp.RespResult;
import com.kuocai.cdn.api.volcengine.cdn.properties.VolcengineCdn;
import com.kuocai.cdn.service.SysConfigService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.vo.*;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * (SysConfig)控制器
 *
 * @author makejava
 * @since 2023-03-22 15:41:05
 */
@RestController
@RequestMapping(value = "SysConfig")
@Scope(value = "session")
public class SysConfigController extends BaseController {

    @Resource
    protected SysConfigService service;

    @Resource
    private OssClient ossClient;

    @Resource
    private PreloadComponent preloadComponent;

    /**
     * description: 保存或更新网站基本配置
     *
     * @param
     * @return com.kuocai.cdn.resp.RespResult
     * @author bo
     * @date 2023/4/1 09:48
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("saveWebsiteBaseConfig")
    @SysLog(module = "系统设置管理", describe = "保存或更新网站基本配置")
    public RespResult saveWebsiteBaseConfig(String websiteName, String websiteAnnouncement, Double defaultFlowPrice, Integer maxDomainCount,
                                            Integer maxDomainCountProxy, String icpNumber, String websiteIconImgUrl,
                                            String websiteLogoImgUrl,
                                            String wechatQrCodeImgUrl, String qqGroupQrCodeImgUrl,
                                            Integer inviteRewardGb, Integer invitedRewardGb, Integer monthGiftGb,
                                            MultipartFile websiteIconImg, MultipartFile websiteLogoImg,
                                            MultipartFile wechatQrCodeImg, MultipartFile qqGroupQrCodeImg,
                                            Integer expireTime, Boolean edgeoneDomainQuotaEnabled,
                                            Integer edgeoneFreeDomainQuota, BigDecimal edgeoneDomainQuotaPrice,
                                            Integer edgeoneDomainQuotaValidDays) {
        // 这里所有参数都不做非null校验
        WebsiteBaseConfigVo websiteBaseConfigVo = null;
        try {
            websiteBaseConfigVo = WebsiteBaseConfigVo.builder().websiteName(websiteName).websiteAnnouncement(websiteAnnouncement).maxDomainCountProxy(maxDomainCountProxy)
                    .inviteRewardGb(inviteRewardGb)
                    .invitedRewardGb(invitedRewardGb)
                    .monthGiftGb(monthGiftGb)
                    .edgeoneDomainQuotaEnabled(edgeoneDomainQuotaEnabled == null || edgeoneDomainQuotaEnabled)
                    .edgeoneFreeDomainQuota(edgeoneFreeDomainQuota == null ? 1 : edgeoneFreeDomainQuota)
                    .edgeoneDomainQuotaPrice(edgeoneDomainQuotaPrice == null ? new BigDecimal("30") : edgeoneDomainQuotaPrice)
                    .edgeoneDomainQuotaValidDays(edgeoneDomainQuotaValidDays == null ? 30 : edgeoneDomainQuotaValidDays)
                    .maxDomainCount(maxDomainCount)
                    .defaultFlowPrice(BigDecimal.valueOf(defaultFlowPrice)).icpNumber(icpNumber)
                    // 判断文件是否已经存在且没被修改，是直接保存路径，否则进行转换
                    .websiteIconImg("false".equals(websiteIconImgUrl) ? (Assert.isEmpty(websiteIconImg) ? "" : ossClient.upload(websiteIconImg)) : ossClient.normalizePublicUrl(websiteIconImgUrl))
                    .websiteLogoImg("false".equals(websiteLogoImgUrl) ? (Assert.isEmpty(websiteLogoImg) ? "" : ossClient.upload(websiteLogoImg)) : ossClient.normalizePublicUrl(websiteLogoImgUrl))
                    .wechatQrCodeImg("false".equals(wechatQrCodeImgUrl) ? (Assert.isEmpty(wechatQrCodeImg) ? "" : ossClient.upload(wechatQrCodeImg)) : ossClient.normalizePublicUrl(wechatQrCodeImgUrl))
                    .qqGroupQrCodeImg("false".equals(qqGroupQrCodeImgUrl) ? (Assert.isEmpty(qqGroupQrCodeImg) ? "" : ossClient.upload(qqGroupQrCodeImg)) : ossClient.normalizePublicUrl(qqGroupQrCodeImgUrl))
                    .expireTime(expireTime).updateTime(DateUtil.now()).build();
        } catch (Exception e) {
            return RespResult.fail(e.getMessage());
        }
        service.saveConfig(websiteBaseConfigVo, ConfigBizTypeConstants.WEBSITE_BASE_CONFIG, loginUserId);
        preloadComponent.loadWebsiteBaseConfig();
        return RespResult.success("更新成功");
    }

    /**
     * description: 保存或更新网站权限配置
     *
     * @param websitePermissionConfigVo
     * @return com.kuocai.cdn.resp.RespResult
     * @throws
     * @author bo
     * @date 2023/4/1 09:48
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("saveWebsitePermissionConfig")
    @SysLog(module = "系统设置管理", describe = "保存或更新网站权限配置")
    public RespResult saveWebsitePermissionConfig(WebsitePermissionConfigVo websitePermissionConfigVo) {
        service.saveConfig(websitePermissionConfigVo, ConfigBizTypeConstants.WEBSITE_PERMISSION_CONFIG, loginUserId);
        preloadComponent.loadWebsitePermissionConfig();
        return RespResult.success("更新成功");
    }

    /**
     * description: 保存或更新SEO配置
     *
     * @param websiteSeoConfigVo
     * @return com.kuocai.cdn.resp.RespResult
     * @author bo
     * @date 2023/4/1 09:49
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("saveWebsiteSeoConfig")
    @SysLog(module = "系统设置管理", describe = "保存或更新SEO配置")
    public RespResult saveWebsiteSeoConfig(WebsiteSeoConfigVo websiteSeoConfigVo) {
        service.saveConfig(websiteSeoConfigVo, ConfigBizTypeConstants.WEBSITE_SEO_CONFIG, loginUserId);
        preloadComponent.loadWebsiteSeoConfig();
        return RespResult.success("更新成功");
    }

    /**
     * description: 保存或更新网站联系方式配置
     *
     * @param websiteContactConfigVo
     * @return com.kuocai.cdn.resp.RespResult
     * @throws
     * @author bo
     * @date 2023/4/1 09:51
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("saveWebsiteContactConfig")
    @SysLog(module = "系统设置管理", describe = "保存或更新网站联系方式配置")
    public RespResult saveWebsiteContactConfig(WebsiteContactConfigVo websiteContactConfigVo) {
        service.saveConfig(websiteContactConfigVo, ConfigBizTypeConstants.WEBSITE_CONTACT_CONFIG, loginUserId);
        return RespResult.success("更新成功");
    }

    /**
     * description: 保存或更新网站同意协议配置
     *
     * @param websiteAgreementConfigVo
     * @return com.kuocai.cdn.resp.RespResult
     * @throws
     * @author bo
     * @date 2023/4/1 21:24
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("saveWebsiteAgreementConfig")
    @SysLog(module = "系统设置管理", describe = "保存或更新网站同意协议配置")
    public RespResult saveWebsiteAgreementConfig(WebsiteAgreementConfigVo websiteAgreementConfigVo) {
        service.saveConfig(websiteAgreementConfigVo, ConfigBizTypeConstants.WEBSITE_AGREEMENT_CONFIG, loginUserId);
        preloadComponent.loadWebsiteBaseConfig();
        return RespResult.success("更新成功");
    }

    /**
     * 保存或更新首页前端代码
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("saveWebsiteHomeCodeConfig")
    @SysLog(module = "系统设置管理", describe = "保存或更新首页前端代码")
    public RespResult saveWebsiteHomeCodeConfig(WebsiteHomeCodeConfigVo websiteHomeCodeConfigVo) {
        if (Boolean.TRUE.equals(websiteHomeCodeConfigVo.getEnabled()) && Assert.isEmpty(websiteHomeCodeConfigVo.getHtmlCode())) {
            return RespResult.fail("启用自定义首页时，HTML 内容不能为空");
        }
        service.savePlainConfig(websiteHomeCodeConfigVo, ConfigBizTypeConstants.WEBSITE_HOME_CODE_CONFIG, loginUserId);
        preloadComponent.loadWebsiteBaseConfig();
        return RespResult.success("更新成功");
    }

    /**
     * 保存或更新底部前端代码
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("saveWebsiteFooterCodeConfig")
    @SysLog(module = "系统设置管理", describe = "保存或更新底部前端代码")
    public RespResult saveWebsiteFooterCodeConfig(WebsiteFooterCodeConfigVo websiteFooterCodeConfigVo) {
        if (Boolean.TRUE.equals(websiteFooterCodeConfigVo.getEnabled()) && Assert.isEmpty(websiteFooterCodeConfigVo.getHtmlCode())) {
            return RespResult.fail("启用自定义底部时，HTML 内容不能为空");
        }
        service.savePlainConfig(websiteFooterCodeConfigVo, ConfigBizTypeConstants.WEBSITE_FOOTER_CODE_CONFIG, loginUserId);
        preloadComponent.loadWebsiteBaseConfig();
        return RespResult.success("更新成功");
    }


    /**
     * description: 保存或更新站点访问根目录配置
     *
     * @param websiteAccessRootConfigVo
     * @return com.kuocai.cdn.resp.RespResult
     * @throws
     * @author bo
     * @date 2023/4/1 09:52
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("saveWebsiteAccessRootConfig")
    @SysLog(module = "系统设置管理", describe = "保存或更新站点访问根目录配置")
    public RespResult saveWebsiteAccessRootConfig(WebsiteAccessRootConfigVo websiteAccessRootConfigVo) {
        service.saveConfig(websiteAccessRootConfigVo, ConfigBizTypeConstants.WEBSITE_ACCESS_CONFIG, loginUserId);
        return RespResult.success("更新成功");
    }


    /**
     * description: 保存或更新微信配置
     *
     * @param weChatConfigVo 微信配置类
     * @return com.kuocai.cdn.resp.RespResult
     * @author bo
     * @date 2023/3/24 16:48
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("saveWechatConfig")
    @SysLog(module = "系统设置管理", describe = "保存或更新微信配置")
    public RespResult saveWechatConfig(WeChatConfigVo weChatConfigVo) {
        service.saveConfig(weChatConfigVo, ConfigBizTypeConstants.WECHAT_CONFIG, loginUserId);
        preloadComponent.loadPayConfig();
        return RespResult.success("更新成功");
    }

    /**
     * description: 保存或更新支付宝配置
     *
     * @param aliPayConfigVo 支付宝配置类
     * @return com.kuocai.cdn.resp.RespResult
     * @author bo
     * @date 2023/3/24 16:47
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("saveAliPayConfig")
    @SysLog(module = "系统设置管理", describe = "保存或更新支付宝配置")
    public RespResult saveAliPayConfig(AliPayConfigVo aliPayConfigVo) {
        try {
            service.saveConfig(aliPayConfigVo, ConfigBizTypeConstants.ALIPAY_CONFIG, loginUserId);
            preloadComponent.loadPayConfig();
            return RespResult.success("更新成功");
        } catch (RuntimeException e) {
            return RespResult.fail("保存支付宝配置失败：" + e.getMessage());
        }
    }

    /**
     * description: 保存或更新支付宝提现配置
     *
     * @param aliWithdrawConfigVo 支付宝提现配置类
     * @return com.kuocai.cdn.resp.RespResult
     * @author bo
     * @date 2023/3/24 16:47
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("saveAliWithdrawConfig")
    @SysLog(module = "系统设置管理", describe = "保存或更新支付宝提现配置")
    public RespResult saveAliWithdrawConfig(AliWithdrawConfigVo aliWithdrawConfigVo) {
        service.saveConfig(aliWithdrawConfigVo, ConfigBizTypeConstants.ALI_WITHDRAW_CONFIG, loginUserId);
        preloadComponent.loadPayConfig();
        return RespResult.success("更新成功");
    }


    /**
     * description: 保存或更新支付宝人脸认证配置
     *
     * @param alipayAuthenticationConfigVo 支付宝人脸认证配置类
     * @return com.kuocai.cdn.resp.RespResult
     * @author bo
     * @date 2023/3/27 09:46
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("saveAliPayAuthenticationConfig")
    @SysLog(module = "系统设置管理", describe = "保存或更新支付宝人脸认证配置")
    public RespResult saveAliPayAuthenticationConfig(AlipayAuthenticationConfigVo alipayAuthenticationConfigVo) {
        service.saveConfig(alipayAuthenticationConfigVo, ConfigBizTypeConstants.ALIPAY_AUTHENTICATION_CONFIG, loginUserId);
        preloadComponent.loadAuthenticationConfig();
        return RespResult.success("更新成功");
    }

    /**
     * description: 保存或更新腾讯人脸认证配置
     *
     * @param tencentFaceConfigVo 腾讯人脸认证配置类
     * @return com.kuocai.cdn.resp.RespResult
     * @author bo
     * @date 2023/3/27 09:48
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("saveTencentFaceConfig")
    @SysLog(module = "系统设置管理", describe = "保存或更新腾讯人脸认证配置")
    public RespResult saveTencentFaceConfig(TencentFaceConfigVo tencentFaceConfigVo) {
        service.saveConfig(tencentFaceConfigVo, ConfigBizTypeConstants.TENCENT_FACE_CONFIG, loginUserId);
        return RespResult.success("更新成功");
    }

    /**
     * description: 保存或更新邮箱配置
     *
     * @param emailConfigVo 邮箱配置类
     * @return com.kuocai.cdn.resp.RespResult
     * @author bo
     * @date 2023/3/27 10:38
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("saveEmailConfig")
    @SysLog(module = "系统设置管理", describe = "保存或更新邮箱配置")
    public RespResult saveEmailConfig(EmailConfigVo emailConfigVo) {
        service.saveConfig(emailConfigVo, ConfigBizTypeConstants.EMAIL_CONFIG, loginUserId);
        preloadComponent.loadEmailConfig();
        return RespResult.success("更新成功");
    }

    /**
     * description: 保存或更新邮箱模版配置(类型前端传)
     *
     * @param emailTemplateVo
     * @return com.kuocai.cdn.resp.RespResult
     * @author bo
     * @date 2023/3/27 10:42
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("saveEmailTemplateConfig")
    @SysLog(module = "系统设置管理", describe = "保存或更新邮箱模版配置")
    public RespResult saveEmailTemplateConfig(EmailTemplateVo emailTemplateVo) {
        // TODO （这里因为模版很多，如果都放在一起加密解密太浪费时间，分开可扩展性更高）
        service.saveConfig(emailTemplateVo, ConfigBizTypeConstants.EMAIL_TEMPLATE_CONFIG, loginUserId);
        preloadComponent.loadEmailConfig();
        return RespResult.success("更新成功");
    }

    /**
     * description: 保存或更新短信基本配置
     *
     * @param smsConfigVo
     * @return com.kuocai.cdn.resp.RespResult
     * @author bo
     * @date 2023/3/27 11:05
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("saveSmsConfig")
    @SysLog(module = "系统设置管理", describe = "保存或更新短信配置")
    public RespResult saveSmsConfig(SmsConfigVo smsConfigVo) {
        service.saveConfig(smsConfigVo, ConfigBizTypeConstants.SMS_CONFIG, loginUserId);
        preloadComponent.loadSmsConfig();
        return RespResult.success("更新成功");
    }

    /**
     * description: 保存或更新短信模版配置(类型前端传)
     *
     * @param smsTemplateVo
     * @return com.kuocai.cdn.resp.RespResult
     * @author bo
     * @date 2023/3/27 11:02
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("saveSmsTemplateConfig")
    @SysLog(module = "系统设置管理", describe = "保存或更新短信模版配置")
    public RespResult saveSmsTemplateConfig(SmsTemplateVo smsTemplateVo) {
        // TODO （这里因为模版很多，如果都放在一起加密解密太浪费时间，分开可扩展性更高）
        service.saveConfig(smsTemplateVo, ConfigBizTypeConstants.SMS_TEMPLATE_CONFIG, loginUserId);
        preloadComponent.loadSmsConfig();
        return RespResult.success("更新成功");
    }

    /**
     * description: 保存或更新微信扫码登录注册配置
     *
     * @param weChatCodeConfigVo
     * @return com.kuocai.cdn.resp.RespResult
     * @author bo
     * @date 2023/3/30 17:15
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("saveWechatCodeConfig")
    @SysLog(module = "系统设置管理", describe = "保存或更新微信扫码登录注册配置")
    public RespResult saveWechatCodeConfig(WeChatCodeConfigVo weChatCodeConfigVo) {
        if (Integer.valueOf(1).equals(weChatCodeConfigVo.getWechatStatus())) {
            if (Assert.isEmpty(weChatCodeConfigVo.getAppId()) || Assert.isEmpty(weChatCodeConfigVo.getAppSecret()) || Assert.isEmpty(weChatCodeConfigVo.getToken())) {
                return RespResult.fail("启用微信登录时，AppID、AppSecret、Token 都不能为空");
            }
        }
        service.saveConfig(weChatCodeConfigVo, ConfigBizTypeConstants.WECHAT_CODE_CONFIG, loginUserId);
        preloadComponent.loadWechatCodeConfig();
        return RespResult.success("更新成功");
    }

    /**
     * description: 保存或更新API 配置
     *
     * @param apiConfigVo
     * @return com.kuocai.cdn.resp.RespResult
     * @author bo
     * @date 2023/3/27 11:21
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("saveApiConfig")
    @SysLog(module = "系统设置管理", describe = "保存或更新API 配置")
    public RespResult saveApiConfig(ApiConfigVo apiConfigVo) {
        service.saveConfig(apiConfigVo, ConfigBizTypeConstants.API_CONFIG, loginUserId);
        return RespResult.success("更新成功");
    }

    /**
     * description: 保存或更新DNS配置
     *
     * @param dnsConfigVo
     * @return com.kuocai.cdn.resp.RespResult
     * @author bo
     * @date 2023/3/27 11:27
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("saveDnsConfig")
    @SysLog(module = "系统设置管理", describe = "保存或更新DNS配置")
    public RespResult saveDnsConfig(DnsConfigVo dnsConfigVo) {
        service.saveConfig(dnsConfigVo, ConfigBizTypeConstants.DNS_CONFIG, loginUserId);
        return RespResult.success("更新成功");
    }

    /**
     * description: 保存或更新华为云API 配置
     *
     * @param huaWeiCloudApiConfigVo 华为云API 配置类
     * @return com.kuocai.cdn.resp.RespResult
     * @author bo
     * @date 2023/3/24 16:48
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("saveHuaWeiCloudConfig")
    @SysLog(module = "系统设置管理", describe = "保存或更新华为云API 配置")
    public RespResult saveHuaWeiCloudConfig(HuaWeiCloudApiConfigVo huaWeiCloudApiConfigVo) {
        service.saveConfig(huaWeiCloudApiConfigVo, ConfigBizTypeConstants.HUAWEI_CLOUD_API_CONFIG, loginUserId);
        preloadComponent.loadCdnApiConfig();
        return RespResult.success("更新成功");
    }

    /**
     * description: 保存或更新火山云API 配置
     *
     * @param volcanicCloudApiConfigVo 火山云API 配置类
     * @return com.kuocai.cdn.resp.RespResult
     * @author bo
     * @date 2023/3/24 16:48
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("saveVolcanicCloudConfig")
    @SysLog(module = "系统设置管理", describe = "保存或更新火山云API 配置")
    public RespResult saveVolcanicCloudConfig(VolcanicCloudApiConfigVo volcanicCloudApiConfigVo) {
        volcanicCloudApiConfigVo.setVolcanicCloudProjectName(
                VolcengineCdn.normalizeProject(volcanicCloudApiConfigVo.getVolcanicCloudProjectName())
        );
        service.saveConfig(volcanicCloudApiConfigVo, ConfigBizTypeConstants.VOLCANIC_CLOUD_API_CONFIG, loginUserId);
        preloadComponent.loadCdnApiConfig();
        return RespResult.success("更新成功");
    }

    /**
     * description: saveVolcanicCloudConfig
     *
     * @param whiteMountainCloudApiConfigVo 白山云配置VO
     * @return com.kuocai.cdn.dto.resp.RespResult
     * @author <link>todoitbo@163.com</link>
     * @date 2023/7/30 17:07
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("saveWhiteMountainCloudConfig")
    @SysLog(module = "系统设置管理", describe = "保存或更新白山云API 配置")
    public RespResult saveWhiteMountainCloudConfig(WhiteMountainCloudApiConfigVo whiteMountainCloudApiConfigVo) {
        service.saveConfig(whiteMountainCloudApiConfigVo, ConfigBizTypeConstants.WHITE_MOUNTAIN_CLOUD_API_CONFIG, loginUserId);
        preloadComponent.loadCdnApiConfig();
        return RespResult.success("更新成功");
    }

    /**
     * 保存或更新腾讯云 API 配置
     *
     * @param tencentCloudApiConfigVo 腾讯云 API 配置类
     * @return com.kuocai.cdn.resp.RespResult
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("saveTencentCloudConfig")
    @SysLog(module = "系统设置管理", describe = "保存或更新腾讯云API 配置")
    public RespResult saveTencentCloudConfig(TencentCloudApiConfigVo tencentCloudApiConfigVo) {
        service.saveConfig(tencentCloudApiConfigVo, ConfigBizTypeConstants.TENCENT_CLOUD_API_CONFIG, loginUserId);
        preloadComponent.loadCdnApiConfig();
        return RespResult.success("更新成功");
    }

    /**
     * 保存或更新 CDNetworks API 配置
     *
     * @param cdnetworksApiConfigVo CDNetworks API 配置类
     * @return com.kuocai.cdn.resp.RespResult
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("saveTencentEdgeOneConfig")
    @SysLog(module = "绯荤粺璁剧疆绠＄悊", describe = "save Tencent EdgeOne API config")
    public RespResult saveTencentEdgeOneConfig(TencentEdgeOneApiConfigVo tencentEdgeOneApiConfigVo) {
        service.saveConfig(tencentEdgeOneApiConfigVo, ConfigBizTypeConstants.TENCENT_EDGEONE_API_CONFIG, loginUserId);
        preloadComponent.loadCdnApiConfig();
        return RespResult.success("鏇存柊鎴愬姛");
    }

    @AuthorLimiter
    @RateLimiter
    @PostMapping("saveCDNetworksConfig")
    @SysLog(module = "系统设置管理", describe = "保存或更新 CDNetworks API 配置")
    public RespResult saveCDNetworksConfig(CDNetworksApiConfigVo cdnetworksApiConfigVo) {
        service.saveConfig(cdnetworksApiConfigVo, ConfigBizTypeConstants.CDNETWORKS_API_CONFIG, loginUserId);
        preloadComponent.loadCdnApiConfig();
        return RespResult.success("更新成功");
    }

    /**
     * description: 保存或更新阿里云CDN配置
     *
     * @param aliyunCdnConfigVo 阿里云CDN配置类
     * @return com.kuocai.cdn.resp.RespResult
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("saveAliyunCdnConfig")
    @SysLog(module = "系统设置管理", describe = "保存或更新阿里云CDN配置")
    public RespResult saveAliyunCdnConfig(AliyunCdnConfigVo aliyunCdnConfigVo) {
        service.saveConfig(aliyunCdnConfigVo, ConfigBizTypeConstants.ALIYUN_CDN_CONFIG, loginUserId);
        preloadComponent.loadCdnApiConfig();
        return RespResult.success("更新成功");
    }

    /**
     * description: 保存或更新网宿CDN配置
     *
     * @param wangsuCdnConfigVo 网宿CDN配置类
     * @return com.kuocai.cdn.resp.RespResult
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("saveWangsuCdnConfig")
    @SysLog(module = "系统设置管理", describe = "保存或更新网宿CDN配置")
    public RespResult saveAliyunCdnConfig(WangsuCdnConfigVo wangsuCdnConfigVo) {
        service.saveConfig(wangsuCdnConfigVo, ConfigBizTypeConstants.WANGSU_CDN_CONFIG, loginUserId);
        preloadComponent.loadCdnApiConfig();
        return RespResult.success("更新成功");
    }

    /**
     * description: 保存或更新百度CDN配置
     *
     * @param baiduCdnConfigVo 百度CDN配置类
     * @return com.kuocai.cdn.resp.RespResult
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("saveBaiduCdnConfig")
    @SysLog(module = "系统设置管理", describe = "保存或更新百度CDN配置")
    public RespResult saveAliyunCdnConfig(BaiduCdnConfigVo baiduCdnConfigVo) {
        service.saveConfig(baiduCdnConfigVo, ConfigBizTypeConstants.BAIDU_CDN_CONFIG, loginUserId);
        preloadComponent.loadCdnApiConfig();
        return RespResult.success("更新成功");
    }

    /**
     * description: 保存或更新金山云CDN配置
     *
     * @param kingsoftCdnConfigVo 金山云CDN配置类
     * @return com.kuocai.cdn.resp.RespResult
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("saveKingsoftCdnConfig")
    @SysLog(module = "系统设置管理", describe = "保存或更新金山云CDN配置")
    public RespResult saveKingsoftCdnConfig(KingsoftCdnConfigVo kingsoftCdnConfigVo) {
        service.saveConfig(kingsoftCdnConfigVo, ConfigBizTypeConstants.KINGSOFT_CDN_CONFIG, loginUserId);
        preloadComponent.loadCdnApiConfig();
        return RespResult.success("更新成功");
    }

    /**
     * description: 保存或更新融合配置
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("saveMergeCdnApiConfig")
    @SysLog(module = "系统设置管理", describe = "保存或更新融合CDN配置")
    public RespResult saveMergeCdnApiConfig(MergeCdnApiConfigVo mergeCdnApiConfigVo) {
        String huaweiWorkHours = mergeCdnApiConfigVo.getHuaweiWorkHours();
        String volcanicWorkHours = mergeCdnApiConfigVo.getVolcanicWorkHours();
        if (huaweiWorkHours.endsWith(";") || volcanicWorkHours.endsWith(";")) {
            return RespResult.fail("时间规则不能以;结尾");
        }
        String pattern = "^(0?[0-9]|1[0-9]|2[0-3])(;(0?[0-9]|1[0-9]|2[0-3]))*$";
        if (!huaweiWorkHours.matches(pattern) || !volcanicWorkHours.matches(pattern)) {
            return RespResult.fail("时间规则配置不符合规范，输入内容只包含0到23的数字，并且每个数字以;分割");
        }
        Set<String> set1 = new HashSet<>(Arrays.asList(huaweiWorkHours.split(";")));
        Set<String> set2 = new HashSet<>(Arrays.asList(volcanicWorkHours.split(";")));
        Set<String> mergedSet = new HashSet<>(set1);
        mergedSet.addAll(set2);
        if (mergedSet.size() < set1.size() + set2.size()) {
            return RespResult.fail("时间规则不可以包含相同的时间");
        }
        service.saveConfig(mergeCdnApiConfigVo, ConfigBizTypeConstants.MERGE_CDN_API_CONFIG, loginUserId);
        preloadComponent.loadCdnApiConfig();
        return RespResult.success("更新成功");
    }
}
