package com.kuocai.cdn.entity;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.kuocai.cdn.config.SystemConfig;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.vo.EmailConfigVo;
import com.kuocai.cdn.vo.EmailTemplateVo;
import com.kuocai.cdn.vo.SmsConfigVo;
import com.kuocai.cdn.vo.SmsTemplateVo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * (AgentConfig)实体类
 *
 * @author XUEW
 * @since 2023-06-14 17:24:16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("agent_config")
public class AgentConfig implements Serializable {

    private static final long serialVersionUID = -28121568974633690L;

    /**
     * 主键ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 代理用户ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /**
     * 网站名称
     */
    private String websiteName;

    /**
     * 网站关键词
     */
    private String websiteKeyword;

    /**
     * 网站描述
     */
    private String websiteDescription;

    /**
     * 网站介绍
     */
    private String about;

    /**
     * 解析CNAME
     */
    private String cname;

    /**
     * 网站LOGO
     */
    private String logo;

    /**
     * 深色LOGO
     */
    private String logoDashboard;

    /**
     * 网站图标
     */
    private String icon;

    /**
     * 地址
     */
    private String address;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 公司名称
     */
    private String company;

    /**
     * ICP备案号
     */
    private String icp;

    /**
     * 接入域名
     */
    private String domain;

    /**
     * 网站标题
     */
    private String title;

    /**
     * 许可证
     */
    private String licence;

    /**
     * 许可证跳转地址
     */
    private String licenceUrl;

    /**
     * 微信客服地址
     */
    private String wechatServiceUrl;

    /**
     * 邮箱配置
     */
    private String emailConfig;

    /**
     * 邮件模板配置
     */
    private String emailTemplateConfig;

    /**
     * 短信配置
     */
    private String smsConfig;

    /**
     * 短信模板配置
     */
    private String smsTemplateConfig;

    public EmailConfigVo emailServiceVoConfig() {
        if (Assert.isEmpty(emailConfig) || "{}".equals(emailConfig)) {
            return new EmailConfigVo();
        }
        return JSONObject.parseObject(emailConfig, EmailConfigVo.class);
    }

    public EmailTemplateVo emailTemplateVoConfig() {
        if (Assert.isEmpty(emailTemplateConfig) || "{}".equals(emailTemplateConfig)) {
            return SystemConfig.emailTemplateConfig;
        }
        return JSONObject.parseObject(emailTemplateConfig, EmailTemplateVo.class);
    }

    public SmsConfigVo smsServiceVoConfig() {
        if (Assert.isEmpty(smsConfig) || "{}".equals(smsConfig)) {
            return new SmsConfigVo();
        }
        return JSONObject.parseObject(smsConfig, SmsConfigVo.class);
    }

    public SmsTemplateVo smsTemplateVoConfig() {
        if (Assert.isEmpty(smsTemplateConfig) || "{}".equals(smsTemplateConfig)) {
            return new SmsTemplateVo();
        }
        return JSONObject.parseObject(smsTemplateConfig, SmsTemplateVo.class);
    }

    public boolean emailConfigEmpty() {
        EmailConfigVo emailConfigVo = emailServiceVoConfig();
        return emailConfigVo.empty();
    }

    public boolean smsConfigEmpty() {
        SmsConfigVo smsConfigVo = smsServiceVoConfig();
        return smsConfigVo.empty();
    }
}
