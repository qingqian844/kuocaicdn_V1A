package com.kuocai.cdn.component;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.RandomUtil;
import com.github.qcloudsms.SmsSingleSender;
import com.github.qcloudsms.SmsSingleSenderResult;
import com.github.qcloudsms.httpclient.HTTPException;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.RuntimeConfigUtils;
import com.kuocai.cdn.vo.SmsConfigVo;
import com.kuocai.cdn.vo.SmsTemplateVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 短信服务
 * 1797034 找回密码
 * 1536915 通用验证码
 *
 * @author XUEW
 * @date 下午8:59 2023/2/12
 */
@Slf4j
@Component
public class SmsClient {

    public static Integer smsAppId = Integer.valueOf(RuntimeConfigUtils.optional("sms.app-id", "SMS_APP_ID", "0"));

    public static String smsAppKey = RuntimeConfigUtils.optional("sms.app-key", "SMS_APP_KEY", "");

    public static String smsSign = RuntimeConfigUtils.optional("sms.sign", "SMS_SIGN", "");

    public static Integer smsValidMin = 5;

    /**
     * 发送短信验证码
     *
     * @param phone      手机号
     * @param verifyCode 验证码
     * @return 验证码
     */
    public String sendCode(SmsConfigVo smsConfig, SmsTemplateVo smsTemplateConfig, String phone, String verifyCode) throws HTTPException, IOException, BusinessException {
        // 生成随机验证码
        if (Assert.isEmpty(verifyCode)) {
            // 生成随机验证码
            verifyCode = RandomUtil.randomNumbers(4);
        }
        if (sendSms(smsConfig, smsTemplateConfig.getNotifyTemplateTitle(), phone, verifyCode, String.valueOf(smsValidMin))) {
            return verifyCode;
        } else {
            log.error("验证码发送失败，手机号：{}", phone);
            throw new BusinessException("验证码发送失败，手机号：{}", phone);
        }
    }

    /**
     * 发送短信密码
     *
     * @return 验证码
     */
    public void sendSmsPassword(SmsConfigVo smsConfig, SmsTemplateVo smsTemplateConfig, String targetPhone, String password) throws BusinessException, HTTPException, IOException {
        throw new BusinessException("Plaintext password delivery is disabled");
    }

    /**
     * 发送短信
     *
     * @param templateId  模板ID
     * @param targetPhone 目标手机号
     * @param params      参数
     * @return 是否成功
     */
    public Boolean sendSms(SmsConfigVo smsConfig, Integer templateId, String targetPhone, String... params) throws HTTPException, IOException, BusinessException {
        SmsSingleSender sender = new SmsSingleSender(smsConfig.getSdkAppId(), smsConfig.getSecretKey());
        SmsSingleSenderResult result = sender.sendWithParam("86", targetPhone, templateId, ListUtil.toList(params), smsConfig.getSmsSign(), "", "");
        if (result.result == 0) {
            log.info("短信发送成功！模板ID：{}，手机号：{}，参数：{}", templateId, targetPhone, params);
            return true;
        } else {
            log.error("短信发送失败，手机号：{}，错误信息：{}", targetPhone, result.errMsg);
            throw new BusinessException("短信发送失败，手机号：{}，错误信息：{}", targetPhone, result.errMsg);
        }
    }
}
