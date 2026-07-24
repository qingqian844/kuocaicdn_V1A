package com.kuocai.cdn.component;

import com.kuocai.cdn.dto.resp.RespResult;
import com.kuocai.cdn.service.InstallationStateService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.JwtUtil;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@Component
public class InstallationGuardInterceptor implements HandlerInterceptor {

    private final InstallationStateService installationStateService;

    public InstallationGuardInterceptor(InstallationStateService installationStateService) {
        this.installationStateService = installationStateService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!installationStateService.isPending()) {
            return true;
        }
        String uri = request.getRequestURI();
        Map<String, String> claims = JwtUtil.claimsFormRequest(request);
        boolean admin = Assert.notEmpty(claims) && "admin".equals(claims.get("roleCode"));

        if (isStatic(uri) || "/health".equals(uri) || "/api/setup/status".equals(uri)
                || "/kuocaiadmin".equals(uri) || "/login/loginAdmin".equals(uri)
                || "/logout".equals(uri)) {
            return true;
        }
        if ("/setup".equals(uri) || uri.startsWith("/api/setup/")) {
            if (admin) {
                return true;
            }
            reject(request, response, HttpServletResponse.SC_UNAUTHORIZED,
                    "请先使用临时管理员账号登录", "/kuocaiadmin");
            return false;
        }
        reject(request, response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                "系统正在等待管理员完成首次初始化", admin ? "/setup" : "/kuocaiadmin");
        return false;
    }

    private boolean isStatic(String uri) {
        return uri.startsWith("/front/") || uri.startsWith("/dashboard/")
                || uri.startsWith("/common/") || uri.startsWith("/image/")
                || "/favicon.ico".equals(uri) || "/robots.txt".equals(uri);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, int status,
                        String message, String redirect) throws Exception {
        String accept = request.getHeader("Accept");
        boolean json = uriLooksLikeApi(request.getRequestURI())
                || "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))
                || (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE));
        if (json) {
            response.setStatus(status);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
            response.getWriter().write(RespResult.fail(message).toString());
        } else {
            response.sendRedirect(request.getContextPath() + redirect);
        }
    }

    private boolean uriLooksLikeApi(String uri) {
        return uri.startsWith("/api/") || uri.startsWith("/Sys") || uri.startsWith("/Cdn")
                || uri.startsWith("/Vendor") || uri.startsWith("/login/");
    }
}
