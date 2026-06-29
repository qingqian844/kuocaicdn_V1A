package com.kuocai.cdn.component;

import cn.hutool.core.util.RandomUtil;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.vo.EmailConfigVo;
import com.kuocai.cdn.vo.EmailTemplateVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.regex.Matcher;

/**
 * 邮件服务
 *
 * @author XUEW
 * @date 下午8:59 2023/2/12
 */
@Slf4j
@Component
public class EmailClient {

    public static String fromEmail;
    public static String fromTitle;
    public static String userName;
    public static String password;
    public static String host;
    public static Integer port;
    public static Integer valid = 5;

    /**
     * 发送邮件验证码
     *
     * @param targetEmail 目标邮箱
     * @return 验证码
     */
    public String sendEmailCode(EmailConfigVo serverConfig, EmailTemplateVo templateConfig, String targetEmail, String verifyCode) throws MessagingException {
        if (Assert.isEmpty(verifyCode)) {
            // 生成随机验证码
            verifyCode = RandomUtil.randomNumbers(4);
        }
        sendEmailWithSubject(serverConfig, "用户验证", targetEmail, templateConfig.getNotifyTemplateContent(), verifyCode, valid);
        return verifyCode;
    }

    /**
     * 发送邮件密码
     *
     * @param targetEmail 目标邮箱
     * @return 验证码
     */
    public void sendEmailPassword(EmailConfigVo serverConfig, EmailTemplateVo templateConfig, String targetEmail, String password) throws MessagingException, BusinessException {
        throw new BusinessException("Plaintext password delivery is disabled");
    }

    /**
     * 发送邮箱
     *
     * @param targetEmail 目标邮箱
     * @param content     发送内容
     */
    public void sendEmail(EmailConfigVo serverConfig, EmailTemplateVo templateConfig, String targetEmail, String content, String... params) throws MessagingException {
        sendEmailWithSubject(serverConfig, "系统通知", targetEmail, content, params);
    }

    /**
     * 发送邮箱
     *
     * @param targetEmail 目标邮箱
     * @param content     发送内容
     */
    public void sendEmailWithSubject(EmailConfigVo serverConfig, String subject, String targetEmail, String content, Object... params) throws MessagingException {
        JavaMailSenderImpl javaMailSender = getJavaMailSender(serverConfig);
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
        helper.setSubject(subject);
        for (Object param : params) {
            content = content.replaceFirst("\\%s", Matcher.quoteReplacement(String.valueOf(param)));
        }
        helper.setText(content, true);
        try {
            helper.setFrom(serverConfig.getSenderMailbox(), serverConfig.getSenderTitle());
        } catch (UnsupportedEncodingException e) {
            throw new MessagingException(e.getMessage());
        }
        helper.setTo(targetEmail);
        javaMailSender.send(mimeMessage);
    }

    /**
     * 初始化邮箱发送对象
     */
    public JavaMailSenderImpl getJavaMailSender(EmailConfigVo serverConfig) {
        JavaMailSenderImpl newMailSender = new JavaMailSenderImpl();
        newMailSender.setUsername(serverConfig.getSenderMailbox());
        newMailSender.setPassword(serverConfig.getAuthorizationPassword());
        newMailSender.setHost(serverConfig.getSmtpServer());
        newMailSender.setPort(serverConfig.getServerPort());
        newMailSender.setDefaultEncoding("UTF-8");
        Properties properties = new Properties();
        //开启认证
        properties.setProperty("mail.smtp.auth", "true");
        //设置链接超时
        properties.setProperty("mail.smtp.timeout", "1000");
        // 设置端口
        properties.setProperty("mail.smtp.port", Integer.toString(serverConfig.getServerPort()));
        //设置ssl端口
        properties.setProperty("mail.smtp.socketFactory.port", Integer.toString(serverConfig.getServerPort()));
        properties.setProperty("mail.smtp.socketFactory.fallback", "false");
        properties.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        newMailSender.setJavaMailProperties(properties);
        return newMailSender;
    }
}
