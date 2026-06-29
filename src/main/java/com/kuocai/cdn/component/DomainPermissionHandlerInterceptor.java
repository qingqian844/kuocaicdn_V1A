package com.kuocai.cdn.component;

import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.service.CdnDomainService;
import com.kuocai.cdn.service.SysUserService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class DomainPermissionHandlerInterceptor implements HandlerInterceptor {


    private final CdnDomainService cdnDomainService;
    private final SysUserService sysUserService;

    DomainPermissionHandlerInterceptor(SysUserService sysUserService, CdnDomainService cdnDomainService) {
        this.sysUserService = sysUserService;
        this.cdnDomainService = cdnDomainService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Long userId = (Long) request.getAttribute("userId");
        SysUser user = sysUserService.queryCacheUserById(userId);
        if (1 == user.getRoleId()) {
            return true;
        }
        String id = request.getParameter("id");
        if (cdnDomainService.checkUserDomain(userId, Long.valueOf(id))) {
            return true;
        }
        response.sendRedirect("/403");
        return false;
    }
}
