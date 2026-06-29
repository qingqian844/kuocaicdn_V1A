package com.kuocai.cdn.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kuocai.cdn.api.tencent.dns.properties.TencentDns;
import com.kuocai.cdn.config.SystemConfig;
import com.kuocai.cdn.constant.ConfigBizTypeConstants;
import com.kuocai.cdn.dao.SysConfigDao;
import com.kuocai.cdn.entity.SysConfig;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.ConfigureRsaUtils;
import com.kuocai.cdn.vo.*;
import com.kuocai.cdn.api.aliyun.cdn.AliyunCdnClientFactory;
import com.kuocai.cdn.api.kingsoft.cdn.properties.KingsoftCdn;
import com.kuocai.cdn.api.volcengine.cdn.properties.VolcengineCdn;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;


/**
 * (SysConfig)服务
 *
 * @author makejava
 * @since 2023-03-22 15:41:05
 */
@Slf4j
@Service
public class SysConfigService extends BaseService<SysConfig> {

    @Resource
    protected SysConfigDao dao;

    /**
     * 获取配置实体的Vo对象
     */
    public <T> T getConfigContentVo(Class<T> beanClass, String bizType) {
        SysConfig sysConfig = dao.selectOne(new QueryWrapper<SysConfig>().eq("biz_type", bizType));
        if (Assert.isEmpty(sysConfig) || Assert.isEmpty(sysConfig.getConfigContent())) {
            return null;
        }
        try {
            return ConfigureRsaUtils.decryptConfigStr(sysConfig.getConfigContent(), beanClass);
        } catch (RuntimeException e) {
            try {
                return JSON.parseObject(sysConfig.getConfigContent(), beanClass);
            } catch (RuntimeException jsonException) {
                log.warn("Failed to parse system config [{}] as encrypted or plain JSON", bizType, jsonException);
                throw e;
            }
        }
    }

    /**
     * Get plain JSON config content. Use this only for non-secret large configs.
     */
    public <T> T getPlainConfigContentVo(Class<T> beanClass, String bizType) {
        SysConfig sysConfig = dao.selectOne(new QueryWrapper<SysConfig>().eq("biz_type", bizType));
        return Assert.isEmpty(sysConfig) ? null : JSON.parseObject(sysConfig.getConfigContent(), beanClass);
    }

    /**
     * 获取配置的Vo对象
     */
    public JSONObject getConfigContentVo(String bizType) {
        SysConfig sysConfig = dao.selectOne(new QueryWrapper<SysConfig>().eq("biz_type", bizType));
        return JSON.parseObject(sysConfig.getConfigContent());
    }

    /**
     * description: 保存配置公共方法封装
     *
     * @param object  配置主体
     * @param bizType 配置类型
     * @author bo
     * @date 2023/3/27 11:30
     */
    public Boolean saveConfig(Object object, String bizType, Long userId) {
        String encryptConfigStr = ConfigureRsaUtils.encryptConfigStr(object);
        SysConfig sysConfig = new SysConfig();
        List<SysConfig> sysConfigList = queryByWrapper(new QueryWrapper<SysConfig>().eq("biz_type", bizType));
        // 判断是否为新增
        if (sysConfigList.size() > 0) {
            sysConfig = sysConfigList.get(0);
        } else {
            sysConfig.setCreateBy(userId);
        }
        sysConfig.setUpdateBy(userId);
        sysConfig.setBizType(bizType);
        sysConfig.setConfigContent(encryptConfigStr);
        save(sysConfig);
        // 更新缓存配置
        updateCacheConfig(bizType);
        log.info("更新系统配置[{}]，当前登录用户[{}]，更新内容：[{}]", bizType, userId, object);
        return true;
    }

    /**
     * 更新缓存配置
     *
     * @param bizType
     */
    /**
     * Save plain JSON config content. Use this only for non-secret large configs.
     */
    public Boolean savePlainConfig(Object object, String bizType, Long userId) {
        SysConfig sysConfig = new SysConfig();
        List<SysConfig> sysConfigList = queryByWrapper(new QueryWrapper<SysConfig>().eq("biz_type", bizType));
        if (sysConfigList.size() > 0) {
            sysConfig = sysConfigList.get(0);
        } else {
            sysConfig.setCreateBy(userId);
        }
        sysConfig.setUpdateBy(userId);
        sysConfig.setBizType(bizType);
        sysConfig.setConfigContent(JSON.toJSONString(object));
        save(sysConfig);
        updateCacheConfig(bizType);
        log.info("Update plain system config [{}], login user [{}]", bizType, userId);
        return true;
    }

    private void updateCacheConfig(String bizType) {
        log.info("更新缓存配置[{}]", bizType);
        switch (bizType) {
            case ConfigBizTypeConstants.WEBSITE_BASE_CONFIG:
                SystemConfig.websiteBaseConfig = getConfigContentVo(WebsiteBaseConfigVo.class, ConfigBizTypeConstants.WEBSITE_BASE_CONFIG);
                break;
            case ConfigBizTypeConstants.WEBSITE_PERMISSION_CONFIG:
                SystemConfig.websitePermissionConfig = getConfigContentVo(WebsitePermissionConfigVo.class, ConfigBizTypeConstants.WEBSITE_PERMISSION_CONFIG);
                break;
            case ConfigBizTypeConstants.WEBSITE_AGREEMENT_CONFIG:
                SystemConfig.websiteAgreementConfig = getConfigContentVo(WebsiteAgreementConfigVo.class, ConfigBizTypeConstants.WEBSITE_AGREEMENT_CONFIG);
                break;
            case ConfigBizTypeConstants.DNS_CONFIG:
                SystemConfig.dnsConfig = getConfigContentVo(DnsConfigVo.class, ConfigBizTypeConstants.DNS_CONFIG);
                if (!Assert.isEmpty(SystemConfig.dnsConfig)) {
                    TencentDns.applyConfiguration(
                            SystemConfig.dnsConfig.getSecretId(),
                            SystemConfig.dnsConfig.getSecretKey(),
                            SystemConfig.dnsConfig.getPrimaryDomain()
                    );
                }
                break;
            case ConfigBizTypeConstants.WEBSITE_HOME_CODE_CONFIG:
                SystemConfig.websiteHomeCodeConfig = getPlainConfigContentVo(WebsiteHomeCodeConfigVo.class, ConfigBizTypeConstants.WEBSITE_HOME_CODE_CONFIG);
                break;
            case ConfigBizTypeConstants.WEBSITE_FOOTER_CODE_CONFIG:
                SystemConfig.websiteFooterCodeConfig = getPlainConfigContentVo(WebsiteFooterCodeConfigVo.class, ConfigBizTypeConstants.WEBSITE_FOOTER_CODE_CONFIG);
                break;
            case ConfigBizTypeConstants.ALIPAY_CONFIG:
                SystemConfig.aliPayConfig = getConfigContentVo(AliPayConfigVo.class, ConfigBizTypeConstants.ALIPAY_CONFIG);
                break;
            case ConfigBizTypeConstants.WECHAT_CONFIG:
                SystemConfig.weChatConfig = getConfigContentVo(WeChatConfigVo.class, ConfigBizTypeConstants.WECHAT_CONFIG);
                break;
            case ConfigBizTypeConstants.WECHAT_CODE_CONFIG:
                SystemConfig.weChatCodeConfig = getConfigContentVo(WeChatCodeConfigVo.class, ConfigBizTypeConstants.WECHAT_CODE_CONFIG);
                break;
            case ConfigBizTypeConstants.ALIPAY_AUTHENTICATION_CONFIG:
                SystemConfig.alipayAuthenticationConfig = getConfigContentVo(AlipayAuthenticationConfigVo.class, ConfigBizTypeConstants.ALIPAY_AUTHENTICATION_CONFIG);
                break;
            case ConfigBizTypeConstants.EMAIL_TEMPLATE_CONFIG:
                SystemConfig.emailTemplateConfig = getConfigContentVo(EmailTemplateVo.class, ConfigBizTypeConstants.EMAIL_TEMPLATE_CONFIG);
                break;
            case ConfigBizTypeConstants.EMAIL_CONFIG:
                SystemConfig.emailConfig = getConfigContentVo(EmailConfigVo.class, ConfigBizTypeConstants.EMAIL_CONFIG);
                break;
            case ConfigBizTypeConstants.SMS_TEMPLATE_CONFIG:
                SystemConfig.smsTemplateConfig = getConfigContentVo(SmsTemplateVo.class, ConfigBizTypeConstants.SMS_TEMPLATE_CONFIG);
                break;
            case ConfigBizTypeConstants.SMS_CONFIG:
                SystemConfig.smsConfig = getConfigContentVo(SmsConfigVo.class, ConfigBizTypeConstants.SMS_CONFIG);
                break;
            case ConfigBizTypeConstants.VOLCANIC_CLOUD_API_CONFIG:
                SystemConfig.volcanicCloudApiConfig = getConfigContentVo(VolcanicCloudApiConfigVo.class, ConfigBizTypeConstants.VOLCANIC_CLOUD_API_CONFIG);
                if (!Assert.isEmpty(SystemConfig.volcanicCloudApiConfig)) {
                    VolcengineCdn.Project = VolcengineCdn.normalizeProject(SystemConfig.volcanicCloudApiConfig.getVolcanicCloudProjectName());
                    VolcengineCdn.AK = SystemConfig.volcanicCloudApiConfig.getVolcanicCloudAk();
                    VolcengineCdn.SK = SystemConfig.volcanicCloudApiConfig.getVolcanicCloudSk();
                }
                break;
            case ConfigBizTypeConstants.TENCENT_EDGEONE_API_CONFIG:
                SystemConfig.tencentEdgeOneApiConfigVo = getConfigContentVo(TencentEdgeOneApiConfigVo.class, ConfigBizTypeConstants.TENCENT_EDGEONE_API_CONFIG);
                break;
            case ConfigBizTypeConstants.KINGSOFT_CDN_CONFIG:
                SystemConfig.kingsoftCdnConfig = getConfigContentVo(KingsoftCdnConfigVo.class, ConfigBizTypeConstants.KINGSOFT_CDN_CONFIG);
                if (!Assert.isEmpty(SystemConfig.kingsoftCdnConfig)) {
                    KingsoftCdn.applyConfiguration(
                            SystemConfig.kingsoftCdnConfig.getAccessKey(),
                            SystemConfig.kingsoftCdnConfig.getSecretKey(),
                            SystemConfig.kingsoftCdnConfig.getEndpoint(),
                            SystemConfig.kingsoftCdnConfig.getRegion(),
                            SystemConfig.kingsoftCdnConfig.getServiceName()
                    );
                }
                break;
            case ConfigBizTypeConstants.ALIYUN_CDN_CONFIG:
                SystemConfig.aliyunCdnConfig = getConfigContentVo(AliyunCdnConfigVo.class, ConfigBizTypeConstants.ALIYUN_CDN_CONFIG);
                if (!Assert.isEmpty(SystemConfig.aliyunCdnConfig)) {
                    AliyunCdnClientFactory.applyConfiguration(
                            SystemConfig.aliyunCdnConfig.getAccessKeyId(),
                            SystemConfig.aliyunCdnConfig.getAccessKeySecret()
                    );
                }
                break;
        }
    }
}
