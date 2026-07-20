package com.kuocai.cdn.controller.base;

import com.kuocai.cdn.api.aliyun.authentication.AuthenticationService;
import com.kuocai.cdn.component.AccessTrack;
import com.kuocai.cdn.service.FaceCertifyVerifyService;
import com.kuocai.cdn.component.EmailClient;
import com.kuocai.cdn.component.OssClient;
import com.kuocai.cdn.component.SmsClient;
import com.kuocai.cdn.config.SystemConfig;
import com.kuocai.cdn.constant.ConfigBizTypeConstants;
import com.kuocai.cdn.dto.resp.RespResult;
import com.kuocai.cdn.entity.AgentConfig;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.entity.Message;
import com.kuocai.cdn.entity.PurchasedFlow;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.entity.TransactionOrder;
import com.kuocai.cdn.entity.WorkOrder;
import com.kuocai.cdn.service.*;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.JedisUtil;
import com.kuocai.cdn.util.JwtUtil;
import com.kuocai.cdn.util.SupportedVendorUtils;
import com.kuocai.cdn.vo.WebsiteBaseConfigVo;
import com.kuocai.cdn.vo.WebsiteFooterCodeConfigVo;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * 基础控制器
 *
 * @author XUEW
 * @date 下午9:00 2023/2/12
 */
@Slf4j
public class BaseController {

    @Autowired
    protected SysUserService sysUserService;
    @Autowired
    protected SysMenuService sysMenuService;
    @Autowired
    protected SysRoleService sysRoleService;
    @Autowired
    protected SysUserBannedService sysUserBannedService;
    @Autowired
    protected LoginDeviceService loginDeviceService;
    @Autowired
    protected WorkOrderService workOrderService;
    @Autowired
    protected WorkOrderTypeService workOrderTypeService;
    @Autowired
    protected CdnDomainService cdnDomainService;
    @Autowired
    protected SysUserAccountService sysUserAccountService;
    @Autowired
    protected AuthenticationService authenticationService;
    @Autowired
    protected FaceCertifyVerifyService faceCertifyVerifyService;
    @Autowired
    protected OperationLogService operationLogService;
    @Autowired
    protected HttpsCertificateService httpsCertificateService;
    @Resource
    protected SysConfigService sysConfigService;
    @Autowired
    protected FlowPackageService flowPackageService;
    @Autowired
    protected PurchasedFlowService purchasedFlowService;
    @Autowired
    protected WorkOrderMessageService workOrderMessageService;
    @Autowired
    protected PurchasedFlowDetailService purchasedFlowDetailService;
    @Autowired
    protected TransactionOrderService transactionOrderService;
    @Autowired
    protected AnnouncementService announcementService;
    @Autowired
    protected AgentLevelService agentLevelService;
    @Autowired
    protected RealNameAuthenticationService realNameAuthenticationService;
    @Autowired
    protected MessageService messageService;
    @Autowired
    protected AgentConfigService agentConfigService;
    @Autowired
    protected CacheTaskService cacheTaskService;
    @Autowired
    protected FlowDonateService flowDonateService;
    @Autowired
    protected SelfHostedPortForwardService selfHostedPortForwardService;


    @Autowired
    protected EmailClient emailClient;
    @Autowired
    protected SmsClient smsClient;
    @Autowired
    protected OssClient ossClient;
    /** @Autowired
    protected AccessTrack accessTrack; **/


    protected HttpServletRequest request;
    protected HttpServletResponse response;
    protected HttpSession session;

    @Setter
    @Getter
    protected Long loginUserId;
    @Setter
    @Getter
    protected String route;
    @Setter
    @Getter
    protected String loginUserRoleCode;
    @Setter
    @Getter
    protected SysUser loginUser;
    protected Long agentId;
    protected AgentConfig agentConfig;
    protected WebsiteBaseConfigVo currentWebsiteBaseConfig;

    /**
     * 在每个子类方法调用之前先调用
     */
    @ModelAttribute
    public void setReqAndRes(HttpServletRequest request, HttpServletResponse response, Map<String, Object> map) throws IOException {
        this.request = request;
        this.response = response;
        agentId = null;
        agentConfig = null;
        currentWebsiteBaseConfig = null;
        loginUserId = null;
        loginUserRoleCode = null;
        route = null;
        loginUser = null;
        this.session = request.getSession(true);
        /** Object claims = request.getAttribute(JwtUtil.CLAIMS_KEY);
         if (null != claims) {

         } **/
        Map<String, String> userMap = JwtUtil.claimsFormRequest(request);
        if (Assert.notEmpty(userMap)) {
            loginUserId = Long.valueOf(userMap.get("userId"));
            // 每一次从数据库中获取当前用户的路线
            loginUser = sysUserService.queryCacheUserById(loginUserId);
            if (Assert.notEmpty(loginUser)) {
                route = loginUser.getRoute();
                loginUserRoleCode = loginUser.getRoleId() == 1 ? "admin" : "user";
                session.setAttribute("loginUser", loginUser);
                session.setAttribute("loginUserRoleCode", loginUserRoleCode);
            }
        }
        map.put("loginUser", loginUser);
        map.put("loginUserRoleCode", loginUserRoleCode);
        map.put("authorizedVendors", SupportedVendorUtils.allVendorOptions());
        map.put("authorizedVendorCodes", SupportedVendorUtils.allVendorCodes());
        map.put("vendorNameMap", SupportedVendorUtils.vendorNameMap());
        map.put("defaultAuthorizedVendor", SupportedVendorUtils.defaultVendor());
        // accessTrack.add(request, loginUser);
        if (isPostAndNotSign(request)) {
            return;
        }
        currentWebsiteBaseConfig = sysConfigService.getConfigContentVo(WebsiteBaseConfigVo.class, ConfigBizTypeConstants.WEBSITE_BASE_CONFIG);
        if (Assert.notEmpty(currentWebsiteBaseConfig)) {
            SystemConfig.websiteBaseConfig = currentWebsiteBaseConfig;
        } else {
            currentWebsiteBaseConfig = SystemConfig.websiteBaseConfig;
        }
        map.put("websiteBaseConfig", currentWebsiteBaseConfig);
        map.put("selfHostedPortForwardEnabled",
                selfHostedPortForwardService.isAvailable(route, isAdmin()));
        map.put("websitePermissionConfig", SystemConfig.websitePermissionConfig);
        map.put("websiteAgreementConfig", SystemConfig.websiteAgreementConfig);
        map.put("websiteHomeCodeConfig", SystemConfig.websiteHomeCodeConfig);
        map.put("customHomeEnabled", Assert.notEmpty(SystemConfig.websiteHomeCodeConfig)
                && Boolean.TRUE.equals(SystemConfig.websiteHomeCodeConfig.getEnabled())
                && Assert.notEmpty(SystemConfig.websiteHomeCodeConfig.getHtmlCode()));
        map.put("websiteSeoConfig", SystemConfig.websiteSeoConfig);
        map.put("websiteFooterCodeConfig", SystemConfig.websiteFooterCodeConfig);
        map.put("websiteFooterHtmlCode", resolveWebsiteFooterHtml(SystemConfig.websiteFooterCodeConfig, currentWebsiteBaseConfig));
        map.put("customFooterEnabled", Assert.notEmpty(SystemConfig.websiteFooterCodeConfig)
                && Boolean.TRUE.equals(SystemConfig.websiteFooterCodeConfig.getEnabled())
                && Assert.notEmpty(SystemConfig.websiteFooterCodeConfig.getHtmlCode()));
        map.put("weChatCodeConfig", SystemConfig.weChatCodeConfig);
        map.put("weChatLoginEnabled", Assert.notEmpty(SystemConfig.weChatCodeConfig)
                && Integer.valueOf(1).equals(SystemConfig.weChatCodeConfig.getWechatStatus())
                && Assert.notEmpty(SystemConfig.weChatCodeConfig.getAppId())
                && Assert.notEmpty(SystemConfig.weChatCodeConfig.getAppSecret()));
        map.put("unReadMessages", messageService.queryUnReadMessagesVo(loginUserId));
        map.put("mainLevel1Menus", sysMenuService.queryMainLevel1Menus());
        map.put("mainLevel2Menus", sysMenuService.queryMainLevel2Menus());
        map.put("proxyLevel2Menus", Collections.emptyList());
        if ("admin".equals(loginUserRoleCode)) {
            map.put("newMessageIds", JedisUtil.getListString("admin_work_order_new_message"));
            map.put("countWaitingWorkOrder", workOrderService.countWaiting());
            map.put("countWaitingAuthentication", realNameAuthenticationService.countWaiting());
        }
        boolean openAgent = false;
        if (Assert.notEmpty(loginUser)) {
            // 未绑定手机号
            if ("GET".equals(request.getMethod()) && Assert.isEmpty(loginUser.getPhone()) && !isPhoneBindingRequest(request.getRequestURI())) {
                response.sendRedirect("/user-info");
            }
        }
        map.put("openAgent", openAgent);
        map.put("dashboardLogo", resolveDashboardLogo(openAgent));
        map.put("dashboardIcon", resolveDashboardIcon(openAgent));
    }

    /**
     * 判断是否是POST请求且不是注册和登录请求
     * @param request 请求
     * @return 是否是POST请求且不是注册和登录请求
     */
    private boolean isPostAndNotSign(HttpServletRequest request) {
        return "POST".equals(request.getMethod()) && !request.getRequestURI().contains("register") && !request.getRequestURI().contains("login");
    }

    private boolean isPhoneBindingRequest(String requestURI) {
        return "/api/verify/phone".equals(requestURI)
                || "/beta/verify/phone".equals(requestURI)
                || "/user-info".equals(requestURI);
    }

    private String resolveDashboardLogo(boolean openAgent) {
        if (openAgent && Assert.notEmpty(agentConfig)) {
            if (isCustomLogo(agentConfig.getLogoDashboard())) {
                return agentConfig.getLogoDashboard();
            }
            if (isCustomLogo(agentConfig.getLogo())) {
                return agentConfig.getLogo();
            }
        }
        if (Assert.notEmpty(currentWebsiteBaseConfig)
                && isCustomLogo(currentWebsiteBaseConfig.getWebsiteLogoImg())) {
            return currentWebsiteBaseConfig.getWebsiteLogoImg();
        }
        return null;
    }

    private String resolveDashboardIcon(boolean openAgent) {
        if (openAgent && Assert.notEmpty(agentConfig) && isCustomLogo(agentConfig.getIcon())) {
            return agentConfig.getIcon();
        }
        if (Assert.notEmpty(currentWebsiteBaseConfig)
                && isCustomLogo(currentWebsiteBaseConfig.getWebsiteIconImg())) {
            return currentWebsiteBaseConfig.getWebsiteIconImg();
        }
        return null;
    }

    private boolean isCustomLogo(String logo) {
        return Assert.notEmpty(logo)
                && !logo.contains("dashboard/assets/svg/logos/")
                && !logo.contains("dashboard/assets/svg/logos-light/")
                && !logo.contains("front/assets/svg/logos/");
    }

    public static String extractString(String input) {
        int startIndex = input.indexOf("//") + 2;
        int endIndex = input.indexOf("/", startIndex);
        if (startIndex == -1 || endIndex == -1 || endIndex <= startIndex) {
            return "";
        }
        return input.substring(startIndex, endIndex).trim();
    }

    protected boolean isAdmin() {
        return "admin".equals(loginUserRoleCode);
    }

    protected boolean canAccessDomain(CdnDomain cdnDomain) {
        return Assert.notEmpty(cdnDomain) && (isAdmin() || (loginUserId != null && loginUserId.equals(cdnDomain.getUserId())));
    }

    protected RespResult checkDomainAccess(CdnDomain cdnDomain) {
        if (Assert.isEmpty(cdnDomain)) {
            return RespResult.notFound("domain");
        }
        if (!canAccessDomain(cdnDomain)) {
            return RespResult.fail("FORBIDDEN");
        }
        return null;
    }

    protected RespResult checkDomainNameAccess(Collection<String> domainNames) {
        if (Assert.isEmpty(domainNames)) {
            return RespResult.paramEmpty("domain");
        }
        if (isAdmin()) {
            return null;
        }
        Set<String> uniqueDomainNames = new HashSet<>(domainNames);
        for (String domainName : uniqueDomainNames) {
            CdnDomain cdnDomain = cdnDomainService.queryByDomainName(domainName);
            if (!canAccessDomain(cdnDomain)) {
                return RespResult.fail("FORBIDDEN");
            }
        }
        return null;
    }

    protected boolean canAccessTransactionOrder(TransactionOrder transactionOrder) {
        return Assert.notEmpty(transactionOrder) && (isAdmin() || (loginUserId != null && loginUserId.equals(transactionOrder.getUserId())));
    }

    protected RespResult checkTransactionOrderAccess(TransactionOrder transactionOrder) {
        if (Assert.isEmpty(transactionOrder)) {
            return RespResult.notFound("order");
        }
        if (!canAccessTransactionOrder(transactionOrder)) {
            return RespResult.fail("FORBIDDEN");
        }
        return null;
    }

    protected boolean canAccessPurchasedFlow(PurchasedFlow purchasedFlow) {
        return Assert.notEmpty(purchasedFlow) && (isAdmin() || (loginUserId != null && loginUserId.equals(purchasedFlow.getUserId())));
    }

    protected RespResult checkPurchasedFlowAccess(PurchasedFlow purchasedFlow) {
        if (Assert.isEmpty(purchasedFlow)) {
            return RespResult.notFound("purchasedFlow");
        }
        if (!canAccessPurchasedFlow(purchasedFlow)) {
            return RespResult.fail("FORBIDDEN");
        }
        return null;
    }

    protected boolean canAccessWorkOrder(WorkOrder workOrder) {
        return Assert.notEmpty(workOrder) && (isAdmin() || (loginUserId != null && loginUserId.equals(workOrder.getUserId())));
    }

    protected RespResult checkWorkOrderAccess(WorkOrder workOrder) {
        if (Assert.isEmpty(workOrder)) {
            return RespResult.notFound("workOrder");
        }
        if (!canAccessWorkOrder(workOrder)) {
            return RespResult.fail("FORBIDDEN");
        }
        return null;
    }

    protected RespResult checkWorkOrderIdsAccess(Collection<Long> workOrderIds) {
        if (Assert.isEmpty(workOrderIds)) {
            return RespResult.paramEmpty("workOrder");
        }
        Set<Long> uniqueWorkOrderIds = new HashSet<>(workOrderIds);
        if (uniqueWorkOrderIds.contains(null)) {
            return RespResult.paramEmpty("workOrder");
        }
        Collection<WorkOrder> workOrders = workOrderService.queryByIds(uniqueWorkOrderIds);
        if (workOrders == null || workOrders.size() != uniqueWorkOrderIds.size()) {
            return RespResult.notFound("workOrder");
        }
        for (WorkOrder workOrder : workOrders) {
            RespResult access = checkWorkOrderAccess(workOrder);
            if (access != null) {
                return access;
            }
        }
        return null;
    }

    protected boolean canAccessMessage(Message message) {
        return Assert.notEmpty(message) && (isAdmin() || (loginUserId != null && loginUserId.equals(message.getReceiveUserId())));
    }

    protected RespResult checkMessageAccess(Message message) {
        if (Assert.isEmpty(message)) {
            return RespResult.notFound("message");
        }
        if (!canAccessMessage(message)) {
            return RespResult.fail("FORBIDDEN");
        }
        return null;
    }

    protected RespResult checkMessageIdsAccess(Collection<Long> messageIds) {
        if (Assert.isEmpty(messageIds)) {
            return RespResult.paramEmpty("message");
        }
        Set<Long> uniqueMessageIds = new HashSet<>(messageIds);
        if (uniqueMessageIds.contains(null)) {
            return RespResult.paramEmpty("message");
        }
        Collection<Message> messages = messageService.queryByIds(uniqueMessageIds);
        if (messages == null || messages.size() != uniqueMessageIds.size()) {
            return RespResult.notFound("message");
        }
        for (Message message : messages) {
            RespResult access = checkMessageAccess(message);
            if (access != null) {
                return access;
            }
        }
        return null;
    }

    protected boolean canAccessAgentConfig(AgentConfig agentConfig) {
        return Assert.notEmpty(agentConfig) && (isAdmin() || (loginUserId != null && loginUserId.equals(agentConfig.getUserId())));
    }

    protected RespResult checkAgentConfigAccess(AgentConfig agentConfig) {
        if (Assert.isEmpty(agentConfig)) {
            return RespResult.notFound("agentConfig");
        }
        if (!canAccessAgentConfig(agentConfig)) {
            return RespResult.fail("FORBIDDEN");
        }
        return null;
    }

    protected void addAuthCookie(String token, boolean remember, HttpServletRequest request) {
        StringBuilder cookie = new StringBuilder("kuocai_cdn_token=").append(token)
                .append("; Path=/; HttpOnly; SameSite=Strict");
        if (remember) {
            cookie.append("; Max-Age=").append(7 * 24 * 60 * 60);
        }
        if (request.isSecure()) {
            cookie.append("; Secure");
        }
        response.addHeader("Set-Cookie", cookie.toString());
    }

    protected void deleteAuthCookie(HttpServletRequest request) {
        StringBuilder cookie = new StringBuilder("kuocai_cdn_token=; Path=/; Max-Age=0; HttpOnly; SameSite=Strict");
        if (request.isSecure()) {
            cookie.append("; Secure");
        }
        response.addHeader("Set-Cookie", cookie.toString());
    }

    protected void revokeAuthToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (Assert.notEmpty(cookies)) {
            for (Cookie cookie : cookies) {
                if ("kuocai_cdn_token".equals(cookie.getName())) {
                    JedisUtil.delKey("token:" + cookie.getValue());
                }
            }
        }
        deleteAuthCookie(request);
    }

    private String resolveWebsiteFooterHtml(WebsiteFooterCodeConfigVo footerConfig, WebsiteBaseConfigVo baseConfig) {
        if (Assert.isEmpty(footerConfig) || Assert.isEmpty(footerConfig.getHtmlCode())) {
            return "";
        }
        String html = footerConfig.getHtmlCode();
        if (Assert.notEmpty(baseConfig)) {
            html = replaceIfNotEmpty(html, "/common/images/59872277_1707142593.jpeg", baseConfig.getWechatQrCodeImg());
            html = replaceIfNotEmpty(html, "common/images/59872277_1707142593.jpeg", baseConfig.getWechatQrCodeImg());
            html = replaceIfNotEmpty(html, "/common/images/4583752_1709117981.jpeg", baseConfig.getQqGroupQrCodeImg());
            html = replaceIfNotEmpty(html, "common/images/4583752_1709117981.jpeg", baseConfig.getQqGroupQrCodeImg());
        }
        return html;
    }

    private String replaceIfNotEmpty(String source, String target, String replacement) {
        if (Assert.isEmpty(source) || Assert.isEmpty(target) || Assert.isEmpty(replacement)) {
            return source;
        }
        return source.replace(target, replacement.trim());
    }

}
