package com.kuocai.cdn.controller.rest;

import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.config.SystemConfig;
import com.kuocai.cdn.constant.ConfigBizTypeConstants;
import com.kuocai.cdn.dto.SetupAdminRequest;
import com.kuocai.cdn.dto.SetupDomainRequest;
import com.kuocai.cdn.dto.SetupModuleRequest;
import com.kuocai.cdn.dto.SetupWebsiteRequest;
import com.kuocai.cdn.dto.resp.RespResult;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.enumeration.domainmerage.CdnRoute;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.InstallationStateService;
import com.kuocai.cdn.service.SetupDiagnosticsService;
import com.kuocai.cdn.service.SetupDomainService;
import com.kuocai.cdn.service.SetupModuleService;
import com.kuocai.cdn.service.SysConfigService;
import com.kuocai.cdn.service.SysUserService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.JedisUtil;
import com.kuocai.cdn.util.JwtUtil;
import com.kuocai.cdn.util.PasswordUtils;
import com.kuocai.cdn.util.SupportedVendorUtils;
import com.kuocai.cdn.vo.InstallationStateVo;
import com.kuocai.cdn.vo.WebsiteBaseConfigVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/setup")
public class SetupController {

    private final InstallationStateService installationStateService;
    private final SetupDiagnosticsService diagnosticsService;
    private final SetupDomainService domainService;
    private final SetupModuleService moduleService;
    private final SysUserService userService;
    private final SysConfigService sysConfigService;

    public SetupController(InstallationStateService installationStateService,
                           SetupDiagnosticsService diagnosticsService,
                           SetupDomainService domainService,
                           SetupModuleService moduleService,
                           SysUserService userService,
                           SysConfigService sysConfigService) {
        this.installationStateService = installationStateService;
        this.diagnosticsService = diagnosticsService;
        this.domainService = domainService;
        this.moduleService = moduleService;
        this.userService = userService;
        this.sysConfigService = sysConfigService;
    }

    @GetMapping("/status")
    public RespResult status() {
        if (!installationStateService.isPending()) {
            JSONObject completed = new JSONObject(true);
            completed.put("installation", installationStateService.getState());
            return RespResult.success("系统已经完成初始化", completed);
        }
        JSONObject data = new JSONObject(true);
        data.put("installation", installationStateService.getState());
        data.put("diagnostics", diagnosticsService.diagnose());
        data.put("vendors", SupportedVendorUtils.allVendorOptions().stream()
                .filter(item -> isSelectableDefaultRoute(item.get("code")))
                .collect(Collectors.toList()));
        WebsiteBaseConfigVo website = sysConfigService.getConfigContentVo(
                WebsiteBaseConfigVo.class, ConfigBizTypeConstants.WEBSITE_BASE_CONFIG);
        if (website != null) {
            // This VO has no credentials and is safe to return to the setup page.
            data.put("website", website);
        }
        return RespResult.success("初始化状态查询成功", data);
    }

    @PostMapping("/admin")
    public RespResult admin(@RequestBody SetupAdminRequest request, HttpServletRequest servletRequest) {
        try {
            Long adminId = requirePendingAdmin(servletRequest);
            SysUser admin = userService.queryById(adminId);
            if (request == null || Assert.isEmpty(request.getCurrentPassword())
                    || !userService.checkUserAccountLogin(admin, request.getCurrentPassword())) {
                return RespResult.fail("当前临时管理员密码不正确");
            }
            if (Assert.isEmpty(request.getUserName()) || request.getUserName().trim().length() < 2) {
                return RespResult.fail("管理员账号至少需要 2 个字符");
            }
            if (!strongPassword(request.getNewPassword())) {
                return RespResult.fail("新密码至少 10 位，并同时包含字母和数字");
            }
            if (Assert.notEmpty(request.getEmail()) && !request.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                return RespResult.fail("管理员邮箱格式不正确");
            }
            admin.setUserName(request.getUserName().trim());
            admin.setEmail(trimToNull(request.getEmail()));
            admin.setUserPwd(PasswordUtils.hash(request.getNewPassword()));
            admin.setPwdSalt(null);
            userService.save(admin);
            JedisUtil.delKey("user:" + adminId);
            installationStateService.update(state -> {
                state.setAdminConfigured(true);
                state.setCurrentStep(Math.max(state.getCurrentStep(), 3));
            }, adminId);
            return RespResult.success("管理员账号已更新");
        } catch (BusinessException e) {
            return RespResult.fail(e.getMessage());
        }
    }

    @PostMapping("/domain/verify")
    public RespResult verifyDomain(@RequestBody SetupDomainRequest request, HttpServletRequest servletRequest) {
        try {
            Long adminId = requirePendingAdmin(servletRequest);
            JSONObject result = domainService.verify(request == null ? null : request.getDomain());
            String domain = result.getString("domain");
            installationStateService.update(state -> {
                state.setDomain(domain);
                state.setDomainVerified(true);
                state.setProxyConfigured(false);
                state.setCurrentStep(Math.max(state.getCurrentStep(), 4));
            }, adminId);
            return RespResult.success("域名解析和 HTTP 端口验证通过", result);
        } catch (BusinessException e) {
            return RespResult.fail(e.getMessage());
        }
    }

    @PostMapping("/domain/apply")
    public RespResult applyDomain(@RequestBody SetupDomainRequest request, HttpServletRequest servletRequest) {
        try {
            Long adminId = requirePendingAdmin(servletRequest);
            String domain = domainService.normalizeDomain(request == null ? null : request.getDomain());
            InstallationStateVo state = installationStateService.getState();
            if (!Boolean.TRUE.equals(state.getDomainVerified()) || !domain.equals(state.getDomain())) {
                return RespResult.fail("请先重新验证当前域名的 DNS 解析");
            }
            domainService.apply(domain);
            installationStateService.update(value -> {
                value.setProxyConfigured(true);
                value.setCurrentStep(Math.max(value.getCurrentStep(), 5));
            }, adminId);
            JSONObject result = new JSONObject(true);
            result.put("url", "https://" + domain + "/setup");
            return RespResult.success("HTTPS 已配置并验证成功", result);
        } catch (BusinessException e) {
            return RespResult.fail(e.getMessage());
        }
    }

    @PostMapping("/website")
    public RespResult website(@RequestBody SetupWebsiteRequest request, HttpServletRequest servletRequest) {
        try {
            Long adminId = requirePendingAdmin(servletRequest);
            if (request == null || Assert.isEmpty(request.getWebsiteName())) {
                return RespResult.fail("网站名称不能为空");
            }
            if (request.getDefaultFlowPrice() == null || request.getDefaultFlowPrice().compareTo(BigDecimal.ZERO) < 0) {
                return RespResult.fail("默认流量单价不能小于 0");
            }
            if (request.getMaxDomainCount() == null || request.getMaxDomainCount() < 1) {
                return RespResult.fail("普通用户域名数量必须大于 0");
            }
            if (!isSelectableDefaultRoute(request.getDefaultUserRoute())) {
                return RespResult.fail("默认厂商不在开源版支持范围内");
            }
            WebsiteBaseConfigVo config = sysConfigService.getConfigContentVo(
                    WebsiteBaseConfigVo.class, ConfigBizTypeConstants.WEBSITE_BASE_CONFIG);
            if (config == null) {
                config = new WebsiteBaseConfigVo();
            }
            config.setWebsiteName(request.getWebsiteName().trim());
            config.setWebsiteLogoImg(trimToNull(request.getWebsiteLogoImg()));
            config.setWebsiteIconImg(trimToNull(request.getWebsiteIconImg()));
            config.setIcpNumber(trimToNull(request.getIcpNumber()));
            config.setDefaultFlowPrice(request.getDefaultFlowPrice());
            config.setMaxDomainCount(request.getMaxDomainCount());
            config.setMaxDomainCountProxy(0);
            config.setDefaultUserRoute(request.getDefaultUserRoute());
            applyWebsiteDefaults(config);
            sysConfigService.saveConfig(config, ConfigBizTypeConstants.WEBSITE_BASE_CONFIG, adminId);
            SystemConfig.websiteBaseConfig = config;
            installationStateService.update(state -> {
                state.setWebsiteConfigured(true);
                state.setCurrentStep(Math.max(state.getCurrentStep(), 6));
            }, adminId);
            return RespResult.success("网站基础配置已保存");
        } catch (BusinessException e) {
            return RespResult.fail(e.getMessage());
        }
    }

    @PostMapping("/module/{module}/test")
    public RespResult testModule(@PathVariable String module, @RequestBody SetupModuleRequest request,
                                 HttpServletRequest servletRequest) {
        try {
            Long adminId = requirePendingAdmin(servletRequest);
            return RespResult.success(moduleService.test(module, request, adminId));
        } catch (BusinessException e) {
            return RespResult.fail(e.getMessage());
        }
    }

    @PostMapping("/module/{module}/save")
    public RespResult saveModule(@PathVariable String module, @RequestBody SetupModuleRequest request,
                                 HttpServletRequest servletRequest) {
        try {
            Long adminId = requirePendingAdmin(servletRequest);
            moduleService.save(module, request, adminId);
            return RespResult.success(Boolean.TRUE.equals(request.getEnabled()) ? "模块配置已保存" : "模块已跳过");
        } catch (BusinessException e) {
            return RespResult.fail(e.getMessage());
        }
    }

    @PostMapping("/complete")
    public RespResult complete(HttpServletRequest servletRequest) {
        try {
            Long adminId = requirePendingAdmin(servletRequest);
            JSONObject diagnostics = diagnosticsService.diagnose();
            for (String service : diagnostics.keySet()) {
                JSONObject item = diagnostics.getJSONObject(service);
                if (item == null || !item.getBooleanValue("ok")) {
                    String message = item == null ? "未检测" : item.getString("message");
                    return RespResult.fail("基础服务检查未通过：" + service + "（" + message + "）");
                }
            }
            String currentHost = servletRequest.getHeader("X-Forwarded-Host");
            if (Assert.isEmpty(currentHost)) {
                currentHost = servletRequest.getHeader("Host");
            }
            String configuredDomain = installationStateService.getState().getDomain();
            if (!domainService.normalizeHost(currentHost).equals(configuredDomain)) {
                return RespResult.fail("请先通过 https://" + configuredDomain + "/kuocaiadmin 登录，再完成初始化");
            }
            WebsiteBaseConfigVo website = sysConfigService.getConfigContentVo(
                    WebsiteBaseConfigVo.class, ConfigBizTypeConstants.WEBSITE_BASE_CONFIG);
            if (website == null || Assert.isEmpty(website.getDefaultUserRoute())) {
                return RespResult.fail("请先配置网站默认厂商");
            }
            if (!isSelectableDefaultRoute(website.getDefaultUserRoute())) {
                return RespResult.fail("网站默认厂商配置无效");
            }
            installationStateService.complete(adminId);
            JSONObject result = new JSONObject(true);
            String domain = installationStateService.getState().getDomain();
            result.put("redirect", Assert.isEmpty(domain) ? "/dashboard" : "https://" + domain + "/dashboard");
            return RespResult.success("首次初始化已完成", result);
        } catch (BusinessException | IllegalStateException e) {
            return RespResult.fail(e.getMessage());
        }
    }

    private Long requirePendingAdmin(HttpServletRequest request) throws BusinessException {
        if (!installationStateService.isPending()) {
            throw new BusinessException("系统已经完成初始化");
        }
        Map<String, String> claims = JwtUtil.claimsFormRequest(request);
        if (Assert.isEmpty(claims) || !"admin".equals(claims.get("roleCode"))) {
            throw new BusinessException("只有管理员可以执行首次初始化");
        }
        return Long.valueOf(claims.get("userId"));
    }

    private boolean strongPassword(String value) {
        return value != null && value.length() >= 10
                && value.matches(".*[A-Za-z].*") && value.matches(".*[0-9].*");
    }

    private String trimToNull(String value) {
        return Assert.isEmpty(value) ? null : value.trim();
    }

    private void applyWebsiteDefaults(WebsiteBaseConfigVo config) {
        if (Assert.isEmpty(config.getDefaultAvatarImg())) config.setDefaultAvatarImg("/common/default-avatar.png");
        if (Assert.isEmpty(config.getAdminPath())) config.setAdminPath("kuocaiadmin");
        if (config.getExpireTime() == null) config.setExpireTime(30);
        config.setMaxDomainCountProxy(0);
        config.setInviteRewardGb(0);
        config.setInvitedRewardGb(0);
        config.setMonthGiftGb(0);
        config.setEdgeoneDomainQuotaEnabled(false);
        config.setEdgeoneFreeDomainQuota(0);
        config.setEdgeoneDomainQuotaPrice(BigDecimal.ZERO);
        config.setEdgeoneDomainQuotaValidDays(0);
        if (config.getHttpsRequestFeeEnabled() == null) config.setHttpsRequestFeeEnabled(false);
        if (config.getHttpsRequestFeeUnitCount() == null) config.setHttpsRequestFeeUnitCount(10000L);
        if (config.getHttpsRequestFeeUnitPrice() == null) config.setHttpsRequestFeeUnitPrice(BigDecimal.ZERO);
    }

    private static boolean isSelectableDefaultRoute(String route) {
        return Assert.notEmpty(route)
                && SupportedVendorUtils.allVendorCodes().contains(route)
                && !CdnRoute.MULTI_CDN.getCode().equals(route)
                && !CdnRoute.SELF_HOSTED.getCode().equals(route);
    }
}
