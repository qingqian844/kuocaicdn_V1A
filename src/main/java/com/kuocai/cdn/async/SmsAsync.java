package com.kuocai.cdn.async;

import com.kuocai.cdn.component.EmailClient;
import com.kuocai.cdn.component.SmsClient;
import com.kuocai.cdn.config.SystemConfig;
import com.kuocai.cdn.entity.AgentConfig;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.SysUserService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.JedisUtil;
import com.kuocai.cdn.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import cn.hutool.core.util.RandomUtil;

@Component
@Slf4j
public class SmsAsync {

    public static final String SMS_LIMIT_KEY = "SMS_LIMIT";
    public static final Integer SMS_LIMIT_COUNT = 120;

    @Autowired
    private EmailClient emailClient;

    @Autowired
    private SmsClient smsClient;

    @Lazy
    @Resource
    private SysUserService sysUserService;

    /**
     * 发送邮箱验证码
     *
     * @param userId 用户ID
     * @param email  邮箱
     */
    public void sendEmailCode(EmailConfigVo serverConfig, EmailTemplateVo templateConfig, Long userId, String email) throws Exception {
        String key = userId + ":" + email;
        String cacheCode = JedisUtil.getStr(key);
        String emailCode = emailClient.sendEmailCode(serverConfig, templateConfig, email, cacheCode);
        log.info("成功发送验证码[{}]至邮箱[{}]，登录用户ID[{}]", emailCode, email, userId);
        JedisUtil.setStr(key, emailCode, 5 * 60);
    }

    /**
     * 发送邮箱账户密码
     *
     * @param password 用户密码
     * @param email    邮箱
     */
    public void sendEmailPassword(EmailConfigVo serverConfig, EmailTemplateVo templateConfig, String email, String password) throws Exception {
        throw new BusinessException("Plaintext password delivery is disabled").log();
    }

    /**
     * 发送短信账户密码
     *
     * @param password 用户密码
     * @param phone    手机号
     */
    public void sendSmsPassword(SmsConfigVo smsConfig, SmsTemplateVo smsTemplateConfig, String phone, String password) throws Exception {
        throw new BusinessException("Plaintext password delivery is disabled").log();
    }

    /**
     * 发送手机证码
     *
     * @param userId 用户ID
     * @param phone  手机号码
     */
    public void sendSmsCode(SmsConfigVo smsConfig, SmsTemplateVo smsTemplateConfig, Long userId, String phone) throws Exception {
        String smsLimit = JedisUtil.getStr(SMS_LIMIT_KEY);
        if (Assert.notEmpty(smsLimit)) {
            if (Integer.parseInt(smsLimit) >= SMS_LIMIT_COUNT) {
                throw new BusinessException("每分钟发送短信数量已达系统限额！请稍后再试").log();
            }
        } else {
            JedisUtil.setIncr(SMS_LIMIT_KEY, 60);
        }
        String key = userId + ":" + phone;
        String cacheCode = JedisUtil.getStr(key);
        String emailCode = smsClient.sendCode(smsConfig, smsTemplateConfig, phone, cacheCode);
        log.info("成功发送验证码[{}]至手机[{}]，登录用户ID[{}]", emailCode, phone, userId);
        JedisUtil.setStr(key, emailCode, 5 * 60);
        JedisUtil.incr(SMS_LIMIT_KEY);
    }

    /**
     * 发送手机证码
     *
     * @param phone 手机号码
     */
    public void sendSmsCodeRegister(SmsConfigVo smsConfig, SmsTemplateVo smsTemplateConfig, String phone) throws Exception {
        String smsLimit = JedisUtil.getStr(SMS_LIMIT_KEY);
        if (Assert.notEmpty(smsLimit)) {
            if (Integer.parseInt(smsLimit) >= SMS_LIMIT_COUNT) {
                throw new BusinessException("每分钟发送短信数量已达系统限额！请稍后再试").log();
            }
        } else {
            JedisUtil.setIncr(SMS_LIMIT_KEY, 60);
        }
        String key = "register:" + phone;
        String cacheCode = JedisUtil.getStr(key);
        String emailCode = smsClient.sendCode(smsConfig, smsTemplateConfig, phone, cacheCode);
        log.info("成功发送验证码[{}]至手机[{}]", emailCode, phone);
        JedisUtil.setStr(key, emailCode, 5 * 60);
        JedisUtil.incr(SMS_LIMIT_KEY);
    }

    /**
     * 发送邮箱验证码
     *
     * @param email 手机号码
     */
    public void sendEmailCodeRegister(EmailConfigVo serverConfig, EmailTemplateVo templateConfig, String email) throws Exception {
        String key = "register:" + email;
        String cacheCode = JedisUtil.getStr(key);
        String emailCode = emailClient.sendEmailCode(serverConfig, templateConfig, email, cacheCode);
        log.info("成功发送验证码[{}]至电子邮箱[{}]", emailCode, email);
        JedisUtil.setStr(key, emailCode, 5 * 60);
    }

    public void sendPasswordResetEmail(EmailConfigVo serverConfig, EmailTemplateVo templateConfig,
                                       String email) throws Exception {
        String key = "password-reset:" + email;
        String code = JedisUtil.getStr(key);
        if (Assert.isEmpty(code)) {
            code = RandomUtil.randomNumbers(4);
        }
        String content = templateConfig.getForgetPasswordTemplateContent();
        if (Assert.isEmpty(content)) {
            content = "您的密码重置验证码为：%s，%s分钟内有效。";
        }
        emailClient.sendEmailWithSubject(serverConfig, "重置密码验证码", email, content, code, EmailClient.valid);
        JedisUtil.setStr(key, code, 5 * 60);
    }

    public void sendPasswordResetSms(SmsConfigVo smsConfig, SmsTemplateVo smsTemplateConfig,
                                     String phone) throws Exception {
        String smsLimit = JedisUtil.getStr(SMS_LIMIT_KEY);
        if (Assert.notEmpty(smsLimit) && Integer.parseInt(smsLimit) >= SMS_LIMIT_COUNT) {
            throw new BusinessException("每分钟发送短信数量已达系统限额，请稍后再试").log();
        }
        if (Assert.isEmpty(smsLimit)) {
            JedisUtil.setIncr(SMS_LIMIT_KEY, 60);
        }
        String key = "password-reset:" + phone;
        String code = JedisUtil.getStr(key);
        if (Assert.isEmpty(code)) {
            code = RandomUtil.randomNumbers(4);
        }
        smsClient.sendSms(
                smsConfig,
                smsTemplateConfig.getForgetPasswordTemplateTitle(),
                phone,
                code,
                String.valueOf(SmsClient.smsValidMin)
        );
        JedisUtil.setStr(key, code, 5 * 60);
        JedisUtil.incr(SMS_LIMIT_KEY);
    }

    /**
     * 流量包即将过期通知
     *
     * @param userId      用户ID
     * @param packageName 流量包名称
     */
    @Async
    public void notifyPacketWillExpiration(Long userId, String packageName) throws Exception {
        if (packageName.contains("赠送")||packageName.contains("受邀")||packageName.contains("邀请")||packageName.contains("福利")||packageName.contains("注册")) {
            return;
        }
        SysUser sysUser = sysUserService.queryById(userId);
        AgentConfig agentConfig = sysUserService.queryAgentConfigByJuniorUser(userId);
        if (Assert.notEmpty(sysUser.getEmail())) {
            // 调用 emailClient 的 sendEmail 方法发送通知，如果未绑定邮箱则忽略
            if (Assert.notEmpty(agentConfig) && !agentConfig.emailConfigEmpty()) {
                emailClient.sendEmail(agentConfig.emailServiceVoConfig(), agentConfig.emailTemplateVoConfig(), sysUser.getEmail(), agentConfig.emailTemplateVoConfig().getPacketWillExpirationTemplateContent(), packageName);
            } else {
                emailClient.sendEmail(SystemConfig.emailConfig, SystemConfig.emailTemplateConfig, sysUser.getEmail(), SystemConfig.emailTemplateConfig.getPacketWillExpirationTemplateContent(), packageName);
            }
        }
        if (!Assert.isEmpty(sysUser.getPhone())) {
            // 调用 smsClient 的 sendSms 方法发送通知，如果未绑定手机号则忽略
            if (Assert.notEmpty(agentConfig) && !agentConfig.smsConfigEmpty()) {
                smsClient.sendSms(agentConfig.smsServiceVoConfig(), agentConfig.smsTemplateVoConfig().getPacketWillExpirationTemplateTitle(), sysUser.getPhone(), packageName);
            } else {
                smsClient.sendSms(SystemConfig.smsConfig, SystemConfig.smsTemplateConfig.getPacketWillExpirationTemplateTitle(), sysUser.getPhone(), packageName);
            }
        }
    }

    /**
     * 流量包已过期通知
     *
     * @param userId      用户ID
     * @param packageName 流量包名称
     */
    @Async
    public void notifyPacketExpiration(Long userId, String packageName) throws Exception {
        if (packageName.contains("赠送")||packageName.contains("受邀")||packageName.contains("邀请")||packageName.contains("福利")||packageName.contains("注册")) {
            return;
        }
        SysUser sysUser = sysUserService.queryById(userId);
        AgentConfig agentConfig = sysUserService.queryAgentConfigByJuniorUser(userId);
        if (Assert.notEmpty(sysUser.getEmail())) {
            // 调用 emailClient 的 sendEmail 方法发送通知，如果未绑定邮箱则忽略
            if (Assert.notEmpty(agentConfig) && !agentConfig.emailConfigEmpty()) {
                emailClient.sendEmail(agentConfig.emailServiceVoConfig(), agentConfig.emailTemplateVoConfig(), sysUser.getEmail(), agentConfig.emailTemplateVoConfig().getPacketExpirationTemplateContent(), packageName);
            } else {
                emailClient.sendEmail(SystemConfig.emailConfig, SystemConfig.emailTemplateConfig, sysUser.getEmail(), SystemConfig.emailTemplateConfig.getPacketExpirationTemplateContent(), packageName);
            }
        }
        if (!Assert.isEmpty(sysUser.getPhone())) {
            // 调用 smsClient 的 sendSms 方法发送通知，如果未绑定手机号则忽略
            if (Assert.notEmpty(agentConfig) && !agentConfig.smsConfigEmpty()) {
                smsClient.sendSms(agentConfig.smsServiceVoConfig(), agentConfig.smsTemplateVoConfig().getPacketExpirationTemplateTitle(), sysUser.getPhone(), packageName);
            } else {
                smsClient.sendSms(SystemConfig.smsConfig, SystemConfig.smsTemplateConfig.getPacketExpirationTemplateTitle(), sysUser.getPhone(), packageName);
            }
        }
    }

    /**
     * 流量包已用尽通知
     *
     * @param userId      用户ID
     * @param packageName 流量包名称
     */
    @Async
    public void notifyPacketGiveOut(Long userId, String packageName) throws Exception {
        if (packageName.contains("赠送")||packageName.contains("受邀")||packageName.contains("邀请")||packageName.contains("福利")||packageName.contains("注册")) {
            return;
        }
        String notifyKey = "notify:give_out:" + userId + ":" + packageName.hashCode();
        if (JedisUtil.exists(notifyKey)) {
            log.info("用户[{}]的流量包[{}]已用尽通知在24小时内已发送，本次跳过。", userId, packageName);
            return;
        }


        SysUser sysUser = sysUserService.queryById(userId);
        AgentConfig agentConfig = sysUserService.queryAgentConfigByJuniorUser(userId);
        if (Assert.notEmpty(sysUser.getEmail())) {
            // 调用 emailClient 的 sendEmail 方法发送通知，如果未绑定邮箱则忽略
            if (Assert.notEmpty(agentConfig) && !agentConfig.emailConfigEmpty()) {
                emailClient.sendEmail(agentConfig.emailServiceVoConfig(), agentConfig.emailTemplateVoConfig(), sysUser.getEmail(), agentConfig.emailTemplateVoConfig().getPacketGiveOutTemplateContent(), packageName);
            } else {
                emailClient.sendEmail(SystemConfig.emailConfig, SystemConfig.emailTemplateConfig, sysUser.getEmail(), SystemConfig.emailTemplateConfig.getPacketGiveOutTemplateContent(), packageName);
            }
        }
        if (!Assert.isEmpty(sysUser.getPhone())) {
            // 调用 smsClient 的 sendSms 方法发送通知，如果未绑定手机号则忽略
            if (Assert.notEmpty(agentConfig) && !agentConfig.smsConfigEmpty()) {
                smsClient.sendSms(agentConfig.smsServiceVoConfig(), agentConfig.smsTemplateVoConfig().getPacketGiveOutTemplateTitle(), sysUser.getPhone(), packageName);
            } else {
                smsClient.sendSms(SystemConfig.smsConfig, SystemConfig.smsTemplateConfig.getPacketGiveOutTemplateTitle(), sysUser.getPhone(), packageName);
            }
        }
        JedisUtil.setStr(notifyKey, "sent", 24 * 60 * 60);
    }

    /**
     * 流量包即将用尽通知
     *
     * @param userId      用户ID
     * @param packageName 流量包名称
     */
    @Async
    public void notifyPacketWillGiveOut(Long userId, String packageName) throws Exception {
        if (packageName.contains("赠送")||packageName.contains("受邀")||packageName.contains("邀请")||packageName.contains("福利")||packageName.contains("注册")) {
            return;
        }
        String notifyKey = "notify:give_out:" + userId + ":" + packageName.hashCode();
        if (JedisUtil.exists(notifyKey)) {
            log.info("用户[{}]的流量包[{}]已用尽通知在24小时内已发送，本次跳过。", userId, packageName);
            return;
        }

        SysUser sysUser = sysUserService.queryById(userId);
        AgentConfig agentConfig = sysUserService.queryAgentConfigByJuniorUser(userId);
        if (Assert.notEmpty(sysUser.getEmail())) {
            // 调用 emailClient 的 sendEmail 方法发送通知，如果未绑定邮箱则忽略
            if (Assert.notEmpty(agentConfig) && !agentConfig.emailConfigEmpty()) {
                emailClient.sendEmail(agentConfig.emailServiceVoConfig(), agentConfig.emailTemplateVoConfig(), sysUser.getEmail(), agentConfig.emailTemplateVoConfig().getPacketWillGiveOutTemplateContent(), packageName);
            } else {
                emailClient.sendEmail(SystemConfig.emailConfig, SystemConfig.emailTemplateConfig, sysUser.getEmail(), SystemConfig.emailTemplateConfig.getPacketWillGiveOutTemplateContent(), packageName);
            }
        }
        if (!Assert.isEmpty(sysUser.getPhone())) {
            // 调用 smsClient 的 sendSms 方法发送通知，如果未绑定手机号则忽略
            if (Assert.notEmpty(agentConfig) && !agentConfig.smsConfigEmpty()) {
                smsClient.sendSms(agentConfig.smsServiceVoConfig(), agentConfig.smsTemplateVoConfig().getPacketWillGiveOutTemplateTitle(), sysUser.getPhone(), packageName);
            } else {
                smsClient.sendSms(SystemConfig.smsConfig, SystemConfig.smsTemplateConfig.getPacketWillGiveOutTemplateTitle(), sysUser.getPhone(), packageName);
            }
        }
        JedisUtil.setStr(notifyKey, "sent", 24 * 60 * 60);

    }

    /**
     * 删除长时间未使用的域名通知
     *
     * @param userId     用户ID
     * @param domainName 域名名称
     */
    @Async
    public void notifyLongTimeNoUseDomain(Long userId, String domainName, String dateTime) throws Exception {
        SysUser sysUser = sysUserService.queryById(userId);
        AgentConfig agentConfig = sysUserService.queryAgentConfigByJuniorUser(userId);
        String template = "请注意！您的域名 %s 停用时长即将满七天，系统将在 %s 将此域名删除。";
        if (Assert.notEmpty(sysUser.getEmail())) {
            // 调用 emailClient 的 sendEmail 方法发送通知，如果未绑定邮箱则忽略
            if (Assert.notEmpty(agentConfig) && !agentConfig.emailConfigEmpty()) {
                emailClient.sendEmail(agentConfig.emailServiceVoConfig(), agentConfig.emailTemplateVoConfig(), sysUser.getEmail(), template, domainName, dateTime);
            } else {
                emailClient.sendEmail(SystemConfig.emailConfig, SystemConfig.emailTemplateConfig, sysUser.getEmail(), template, domainName, dateTime);
            }
        }
    }

    /**
     * description: 工单消息提醒
     *
     * @param workOrderMessageRemindVo 工单消息提醒VO
     * @throws Exception e
     */
    @Async
    public void workOrderMessageRemind(WorkOrderMessageRemindVo workOrderMessageRemindVo) throws Exception {
        emailClient.sendEmail(SystemConfig.emailConfig, SystemConfig.emailTemplateConfig, workOrderMessageRemindVo.getEmail(), SystemConfig.emailTemplateConfig.getWorkOrderMessageTemplateContent(), workOrderMessageRemindVo.getUserName(), workOrderMessageRemindVo.getWorkOrderTitle(), workOrderMessageRemindVo.getWorkOrderContent());
    }
}
