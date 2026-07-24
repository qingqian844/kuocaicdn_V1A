package com.kuocai.cdn.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.component.EmailClient;
import com.kuocai.cdn.constant.ConfigBizTypeConstants;
import com.kuocai.cdn.dto.SetupModuleRequest;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.vo.AlipayAuthenticationConfigVo;
import com.kuocai.cdn.vo.DnsConfigVo;
import com.kuocai.cdn.vo.EmailConfigVo;
import com.kuocai.cdn.vo.SmsConfigVo;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.dnspod.v20210323.DnspodClient;
import com.tencentcloudapi.dnspod.v20210323.models.DescribeDomainListRequest;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
public class SetupModuleService {

    private static final Set<String> MODULES = new HashSet<>(Arrays.asList(
            "dns", "email", "sms", "realname"));

    private final SysConfigService sysConfigService;
    private final InstallationStateService installationStateService;
    private final EmailClient emailClient;

    public SetupModuleService(SysConfigService sysConfigService,
                              InstallationStateService installationStateService,
                              EmailClient emailClient) {
        this.sysConfigService = sysConfigService;
        this.installationStateService = installationStateService;
        this.emailClient = emailClient;
    }

    public String test(String module, SetupModuleRequest request, Long userId) throws BusinessException {
        requireModule(module);
        if (!Boolean.TRUE.equals(request.getEnabled())) {
            return "模块未启用，无需测试";
        }
        JSONObject config = requireConfig(request);
        validate(module, config);
        String message;
        switch (module) {
            case "email":
                testEmail(config);
                message = "SMTP 连接和账号认证成功";
                break;
            case "dns":
                testDnsPod(config);
                message = "DNSPod API 连接和账号认证成功";
                break;
            case "realname":
                testUrl(defaultText(config.getString("gatewayUrlAlipay"), "https://openapi.alipay.com/gateway.do"));
                message = "支付宝网关连接正常，配置格式有效";
                break;
            case "sms":
                testUrl("https://sms.tencentcloudapi.com");
                message = "腾讯云短信 API 网络连接正常，配置格式有效";
                break;
            default:
                throw new BusinessException("不支持的初始化模块：" + module);
        }
        String hash = hash(module, request);
        installationStateService.update(state -> state.getModuleTestHashes().put(module, hash), userId);
        return message;
    }

    public void save(String module, SetupModuleRequest request, Long userId) throws BusinessException {
        requireModule(module);
        boolean enabled = Boolean.TRUE.equals(request.getEnabled());
        JSONObject config = request.getConfig() == null ? new JSONObject() : request.getConfig();
        if (enabled) {
            validate(module, config);
            String tested = installationStateService.getState().getModuleTestHashes().get(module);
            if (!hash(module, request).equals(tested)) {
                throw new BusinessException("配置已变化，请重新测试连接后再保存");
            }
            saveEnabled(module, config, userId);
        } else {
            saveDisabled(module, userId);
        }
        installationStateService.update(state -> {
            state.getModules().put(module, enabled);
            state.setCurrentStep(Math.max(state.getCurrentStep(), 7));
        }, userId);
    }

    private void saveEnabled(String module, JSONObject config, Long userId) throws BusinessException {
        switch (module) {
            case "dns":
                sysConfigService.saveConfig(config.toJavaObject(DnsConfigVo.class), ConfigBizTypeConstants.DNS_CONFIG, userId);
                break;
            case "email":
                sysConfigService.saveConfig(config.toJavaObject(EmailConfigVo.class), ConfigBizTypeConstants.EMAIL_CONFIG, userId);
                break;
            case "sms":
                sysConfigService.saveConfig(config.toJavaObject(SmsConfigVo.class), ConfigBizTypeConstants.SMS_CONFIG, userId);
                break;
            case "realname":
                putDefault(config, "gatewayUrlAlipay", "https://openapi.alipay.com/gateway.do");
                putDefault(config, "signTypeAlipay", "RSA2");
                putDefault(config, "charsetAlipay", "utf-8");
                putDefault(config, "formatAlipay", "json");
                sysConfigService.saveConfig(config.toJavaObject(AlipayAuthenticationConfigVo.class),
                        ConfigBizTypeConstants.ALIPAY_AUTHENTICATION_CONFIG, userId);
                break;
            default:
                throw new BusinessException("不支持的初始化模块：" + module);
        }
    }

    private void saveDisabled(String module, Long userId) {
        switch (module) {
            case "dns":
                sysConfigService.saveConfig(new DnsConfigVo(), ConfigBizTypeConstants.DNS_CONFIG, userId);
                break;
            case "email":
                sysConfigService.saveConfig(new EmailConfigVo(), ConfigBizTypeConstants.EMAIL_CONFIG, userId);
                break;
            case "sms":
                sysConfigService.saveConfig(new SmsConfigVo(), ConfigBizTypeConstants.SMS_CONFIG, userId);
                break;
            case "realname":
                sysConfigService.saveConfig(new AlipayAuthenticationConfigVo(),
                        ConfigBizTypeConstants.ALIPAY_AUTHENTICATION_CONFIG, userId);
                break;
            default:
                break;
        }
    }

    private void validate(String module, JSONObject config) throws BusinessException {
        switch (module) {
            case "dns":
                required(config, "primaryDomain", "selectDns", "secretId", "secretKey");
                break;
            case "email":
                required(config, "smtpServer", "senderMailbox", "senderTitle", "authorizationPassword", "serverPort");
                break;
            case "sms":
                required(config, "sdkAppId", "secretId", "secretKey", "smsSign");
                break;
            case "realname":
                required(config, "appIdAlipay", "privateKeyAlipay", "publicKeyAlipay", "alipayCertificationUrl");
                break;
            default:
                throw new BusinessException("不支持的初始化模块：" + module);
        }
    }

    private JSONObject requireConfig(SetupModuleRequest request) throws BusinessException {
        if (request == null || request.getConfig() == null) {
            throw new BusinessException("模块配置不能为空");
        }
        return request.getConfig();
    }

    private void required(JSONObject config, String... keys) throws BusinessException {
        for (String key : keys) {
            if (Assert.isEmpty(config.get(key))) {
                throw new BusinessException(key + " 不能为空");
            }
        }
    }

    private void requireModule(String module) throws BusinessException {
        if (!MODULES.contains(module)) {
            throw new BusinessException("不支持的初始化模块：" + module);
        }
    }

    private String hash(String module, SetupModuleRequest request) throws BusinessException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] value = digest.digest((module + ":" + JSON.toJSONString(request)).getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte item : value) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (Exception e) {
            throw new BusinessException("无法校验模块配置：" + e.getMessage());
        }
    }

    private void testUrl(String value) throws BusinessException {
        try {
            URI uri = URI.create(value);
            int port = uri.getPort() > 0 ? uri.getPort() : ("http".equalsIgnoreCase(uri.getScheme()) ? 80 : 443);
            testSocket(uri.getHost(), port);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("服务地址格式错误：" + value);
        }
    }

    private void testDnsPod(JSONObject config) throws BusinessException {
        try {
            Credential credential = new Credential(config.getString("secretId"), config.getString("secretKey"));
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("dnspod.tencentcloudapi.com");
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            DnspodClient client = new DnspodClient(credential, "", clientProfile);
            DescribeDomainListRequest request = new DescribeDomainListRequest();
            request.setLimit(1L);
            client.DescribeDomainList(request);
        } catch (Exception e) {
            throw new BusinessException("DNSPod 凭证验证失败：" + compactMessage(e));
        }
    }

    private void testEmail(JSONObject config) throws BusinessException {
        try {
            EmailConfigVo emailConfig = config.toJavaObject(EmailConfigVo.class);
            emailClient.getJavaMailSender(emailConfig).testConnection();
        } catch (Exception e) {
            throw new BusinessException("SMTP 登录验证失败：" + compactMessage(e));
        }
    }

    private void testSocket(String host, int port) throws BusinessException {
        if (Assert.isEmpty(host) || port <= 0) {
            throw new BusinessException("服务主机或端口无效");
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 5000);
        } catch (Exception e) {
            throw new BusinessException("无法连接 " + host + ":" + port + "：" + e.getMessage());
        }
    }

    private String compactMessage(Exception exception) {
        Throwable current = exception;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    private String defaultText(String value, String fallback) {
        return Assert.isEmpty(value) ? fallback : value;
    }

    private void putDefault(JSONObject config, String key, String value) {
        if (Assert.isEmpty(config.getString(key))) {
            config.put(key, value);
        }
    }
}
