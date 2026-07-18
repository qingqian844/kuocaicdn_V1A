package com.kuocai.cdn.controller.login;

import com.kuocai.cdn.async.SmsAsync;
import com.kuocai.cdn.config.SystemConfig;
import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.dto.resp.RespResult;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.SysUserService;
import com.kuocai.cdn.service.InstallationStateService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.GeetestUtils;
import com.kuocai.cdn.util.ValidatorUtils;
import com.kuocai.cdn.vo.SysUserVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Map;

/**
 * 登录控制器
 *
 * @author XUEW
 * @date 下午9:00 2023/2/12
 */
@Slf4j
@Controller
@Scope(value = "session")
public class LoginController extends BaseController {

    LoginController(SysUserService userService, SmsAsync sendSmsCode,
                    InstallationStateService installationStateService) {
        this.userService = userService;
        this.sendSmsCode = sendSmsCode;
        this.installationStateService = installationStateService;
    }

    private final SysUserService userService;

    private final SmsAsync sendSmsCode;
    private final InstallationStateService installationStateService;

    /**
     * 管理员登录
     *
     * @param userVo 登录信息
     * @return 响应
     */
    @ResponseBody
    @PostMapping("/login/loginAdmin")
    public RespResult loginAdmin(SysUserVo userVo, HttpServletRequest request) {
        if (Assert.isEmpty(userVo.getUserAccount())) {
            return RespResult.paramEmpty("账户");
        }
        if (Assert.isEmpty(userVo.getUserPwd())) {
            return RespResult.paramEmpty("密码");
        }
        userVo.setRoleId(1L);
        try {
            String token = userService.loginUser(userVo, request);
            addAuthCookie(token, Boolean.TRUE.equals(userVo.getRemember()), request);
            return RespResult.success("登录成功", installationStateService.isPending() ? "/setup" : "/dashboard");
        } catch (BusinessException e) {
            return RespResult.fail(e.getMessage());
        }
    }

    /**
     * 普通用户登录
     *
     * @param userVo 登录信息
     * @return 响应
     */
    @ResponseBody
    @PostMapping("/login/loginUser")
    public RespResult loginUser(SysUserVo userVo, HttpServletRequest request) {
        if (Assert.isEmpty(userVo.getUserAccount())) {
            return RespResult.paramEmpty("账户");
        }
        if (Assert.isEmpty(userVo.getUserPwd())) {
            return RespResult.paramEmpty("密码");
        }
        userVo.setRoleId(2L);
        try {
            String token = userService.loginUser(userVo, request);
            addAuthCookie(token, Boolean.TRUE.equals(userVo.getRemember()), request);
            return RespResult.success("登录成功");
        } catch (BusinessException e) {
            return RespResult.fail(e.getMessage());
        }
    }

    /**
     * 退出登录
     */
    @ResponseBody
    @PostMapping("/logout")
    public RespResult logout(HttpServletRequest request) {
        revokeAuthToken(request);
        try {
            session.invalidate();
        } catch (Exception e) {
            log.error("销毁Session失败，错误原因：{}", e.getMessage());
        }
        log.info("用户退出登录，用户[{}]", loginUserId);
        return RespResult.success("退出登录成功");
    }

    /**
     * 发送短信验证码
     */
    @PostMapping("/login/sendSmsCode")
    @ResponseBody
    public RespResult sendSmsCode(String userPhone, String verify) {
        if (Assert.isEmpty(loginUser)) {
            return RespResult.fail("请先登录，再进行操作");
        }
        if (!GeetestUtils.validate(verify)) {
            return RespResult.fail("人机验证失败，请重试");
        }
        if (!ValidatorUtils.isPhone(userPhone)) {
            return RespResult.fail("手机号码格式错误");
        }
        if (userPhone.equals(loginUser.getPhone())) {
            return RespResult.fail("手机号码尚未修改");
        }
        SysUser existUser = userService.queryByPhone(userPhone);
        if (Assert.notEmpty(existUser)) {
            return RespResult.fail("手机号码已被占用");
        }
        try {
            if (Assert.notEmpty(agentConfig) && !agentConfig.smsConfigEmpty()) {
                sendSmsCode.sendSmsCode(agentConfig.smsServiceVoConfig(), agentConfig.smsTemplateVoConfig(), loginUserId, userPhone);
            } else {
                sendSmsCode.sendSmsCode(SystemConfig.smsConfig, SystemConfig.smsTemplateConfig, loginUserId, userPhone);
            }
        } catch (Exception e) {
            return RespResult.fail(e.getMessage());
        }
        return RespResult.success("发送成功");
    }

    /**
     * 发送短信验证码用户注册
     */
    @PostMapping("/login/sendSmsCodeTemplate")
    @ResponseBody
    public RespResult sendSmsCodeTemplate(String userPhone, String verify) {
        if (!GeetestUtils.validate(verify)) {
            return RespResult.fail("人机验证失败，请重试");
        }
        if (!ValidatorUtils.isPhone(userPhone)) {
            return RespResult.fail("手机格式错误");
        }
        if (Assert.notEmpty(userService.queryByPhone(userPhone))) {
            return RespResult.fail("此手机号已注册，快去登录吧");
        }
        try {
            if (Assert.notEmpty(agentConfig) && !agentConfig.smsConfigEmpty()) {
                sendSmsCode.sendSmsCodeRegister(agentConfig.smsServiceVoConfig(), agentConfig.smsTemplateVoConfig(), userPhone);
            } else {
                sendSmsCode.sendSmsCodeRegister(SystemConfig.smsConfig, SystemConfig.smsTemplateConfig, userPhone);
            }
        } catch (Exception e) {
            return RespResult.fail("验证码发送失败"+e.getMessage());
        }
        return RespResult.success("发送成功");
    }

    /**
     * 发送短信验证码用户注册
     */
    @PostMapping("/login/sendEmailCodeTemplate")
    @ResponseBody
    public RespResult sendEmailCodeTemplate(String userEmail, String verify) {
        if (!GeetestUtils.validate(verify)) {
            return RespResult.fail("人机验证失败，请重试");
        }
        if (!ValidatorUtils.isEmail(userEmail)) {
            return RespResult.fail("邮箱地址格式错误");
        }
        SysUser existUser = userService.queryByEmail(userEmail);
        if (Assert.notEmpty(existUser)) {
            return RespResult.fail("邮箱地址已被占用");
        }
        try {
            if (Assert.notEmpty(agentConfig) && !agentConfig.emailConfigEmpty()) {
                sendSmsCode.sendEmailCodeRegister(agentConfig.emailServiceVoConfig(), agentConfig.emailTemplateVoConfig(), userEmail);
            } else {
                sendSmsCode.sendEmailCodeRegister(SystemConfig.emailConfig, SystemConfig.emailTemplateConfig, userEmail);
            }
        } catch (Exception e) {
            return RespResult.fail("验证码发送失败");
        }
        return RespResult.success("发送成功");
    }

    /**
     * 发送邮箱验证码
     */
    @PostMapping("/login/sendEmailCode")
    @ResponseBody
    public RespResult sendEmailCode(String userEmail, String verify) {
        if (Assert.isEmpty(loginUser)) {
            return RespResult.fail("请先登录，再进行操作");
        }
        if (!GeetestUtils.validate(verify)) {
            return RespResult.fail("人机验证失败，请重试");
        }
        if (!ValidatorUtils.isEmail(userEmail)) {
            return RespResult.fail("邮箱地址格式错误");
        }
        if (userEmail.equals(loginUser.getEmail())) {
            return RespResult.fail("邮箱地址尚未修改");
        }
        SysUser existUser = userService.queryByEmail(userEmail);
        if (Assert.notEmpty(existUser)) {
            return RespResult.fail("邮箱地址已被占用");
        }
        try {
            if (Assert.notEmpty(agentConfig) && !agentConfig.emailConfigEmpty()) {
                sendSmsCode.sendEmailCode(agentConfig.emailServiceVoConfig(), agentConfig.emailTemplateVoConfig(), loginUserId, userEmail);
            } else {
                sendSmsCode.sendEmailCode(SystemConfig.emailConfig, SystemConfig.emailTemplateConfig, loginUserId, userEmail);
            }
        } catch (Exception e) {
            return RespResult.fail("验证码发送失败");
        }
        return RespResult.success("发送成功");
    }

    /**
     * 获取登录密码
     */
    @PostMapping("/login/getPassword")
    @ResponseBody
    public RespResult getPassword(String userAccount, String verify) {
        if (!GeetestUtils.validate(verify)) {
            return RespResult.fail("人机验证失败，请重试");
        }
        if (Assert.isEmpty(userAccount)) {
            return RespResult.fail("账户信息不可为空");
        }
        try {
            sysUserService.sendPassword(userAccount);
        } catch (BusinessException e) {
            return RespResult.fail(e.getMessage());
        }
        return RespResult.success("我们已将您的登录密码发送至 " + userAccount + ", 请注意查收");
    }

    @PostMapping("/login/sendPasswordResetCode")
    @ResponseBody
    public RespResult sendPasswordResetCode(String userAccount, String verify) {
        if (!GeetestUtils.validate(verify)) {
            return RespResult.fail("人机验证失败，请重试");
        }
        if (Assert.isEmpty(userAccount)) {
            return RespResult.fail("账号信息不可为空");
        }
        SysUser user = userAccount.contains("@")
                ? userService.queryByEmail(userAccount)
                : userService.queryByPhone(userAccount);
        if (Assert.isEmpty(user)) {
            return RespResult.fail("账号不存在或尚未绑定");
        }
        try {
            if (userAccount.contains("@")) {
                if (Assert.notEmpty(agentConfig) && !agentConfig.emailConfigEmpty()) {
                    sendSmsCode.sendPasswordResetEmail(
                            agentConfig.emailServiceVoConfig(),
                            agentConfig.emailTemplateVoConfig(),
                            userAccount
                    );
                } else {
                    sendSmsCode.sendPasswordResetEmail(
                            SystemConfig.emailConfig,
                            SystemConfig.emailTemplateConfig,
                            userAccount
                    );
                }
            } else {
                if (!ValidatorUtils.isPhone(userAccount)) {
                    return RespResult.fail("手机号码格式错误");
                }
                if (Assert.notEmpty(agentConfig) && !agentConfig.smsConfigEmpty()) {
                    sendSmsCode.sendPasswordResetSms(
                            agentConfig.smsServiceVoConfig(),
                            agentConfig.smsTemplateVoConfig(),
                            userAccount
                    );
                } else {
                    sendSmsCode.sendPasswordResetSms(
                            SystemConfig.smsConfig,
                            SystemConfig.smsTemplateConfig,
                            userAccount
                    );
                }
            }
        } catch (Exception e) {
            return RespResult.fail("验证码发送失败：" + e.getMessage());
        }
        return RespResult.success("验证码已发送，5分钟内有效");
    }

    @PostMapping("/login/resetPassword")
    @ResponseBody
    public RespResult resetPassword(String userAccount, String code, String newPassword,
                                    String confirmPassword) {
        if (Assert.isEmpty(userAccount) || Assert.isEmpty(code)
                || Assert.isEmpty(newPassword) || Assert.isEmpty(confirmPassword)) {
            return RespResult.fail("请完整填写重置信息");
        }
        if (!newPassword.equals(confirmPassword)) {
            return RespResult.fail("两次输入的密码不一致");
        }
        try {
            userService.resetPassword(userAccount, code, newPassword);
            return RespResult.success("密码重置成功，请使用新密码登录");
        } catch (BusinessException e) {
            return RespResult.fail(e.getMessage());
        }
    }

    /**
     * 忘记密码
     */
    @GetMapping("/forget")
    public String forget(Map<String, Object> map) {
        return "user/forget";
    }

    /**
     * 注册页面
     */
    @GetMapping("/register")
    public String register(String code, Map<String, Object> map) {
        if (Assert.notEmpty(code)) {
            map.put("code", code);
        }
        return "user/register";
    }


    /**
     * 邮箱注册页面
     */
    @GetMapping("/register-email")
    public String registerEmail(String code, Map<String, Object> map) {
        if (Assert.notEmpty(code)) {
            map.put("code", code);
        }
        return "user/register-email";
    }


    /**
     * 管理员-登录
     */
    @GetMapping("/kuocaiadmin")
    public String adminLogin(Map<String, Object> map) {
        return "admin/login";
    }

    /**
     * 普通用户-登陆
     */
    @GetMapping("/user-login")
    public String userLogin(String callback, Map<String, Object> map) {
        map.put("callback", sanitizeCallback(callback));
        return "user/login";
    }

    private String sanitizeCallback(String callback) {
        if (Assert.isEmpty(callback)) {
            return null;
        }
        String value = callback.trim();
        if (value.startsWith("//") || value.contains("\\") || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
            return null;
        }
        try {
            URI uri = URI.create(value);
            if (uri.isAbsolute() || uri.getRawAuthority() != null) {
                return null;
            }
        } catch (IllegalArgumentException e) {
            return null;
        }
        return value;
    }

    /**
     * 验证手机号
     */
    @GetMapping("/api/verify/phone")
    @ResponseBody
    public RespResult verifyPhone() {
        if (Assert.isEmpty(loginUser)) {
            return RespResult.fail("请先登录，再进行操作");
        }
        String phone = loginUser.getPhone();
        if (Assert.isEmpty(phone)) {
            return RespResult.success("请绑定手机号码");
        }
        return RespResult.success("已绑定手机号码");
    }
}
