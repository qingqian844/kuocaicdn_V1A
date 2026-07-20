package com.kuocai.cdn.config;

import com.kuocai.cdn.component.DomainPermissionHandlerInterceptor;
import com.kuocai.cdn.component.LoginHandlerInterceptor;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.ErrorPageRegistrar;
import org.springframework.boot.web.server.ErrorPageRegistry;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC 控制器
 *
 * @author XUEW
 * @date 下午8:59 2023/2/12
 */
@Configuration
public class MvcConfig implements WebMvcConfigurer, ErrorPageRegistrar {

    private final DomainPermissionHandlerInterceptor domainPermissionHandlerInterceptor;

    private final LoginHandlerInterceptor loginHandlerInterceptor;

    public MvcConfig(DomainPermissionHandlerInterceptor domainPermissionHandlerInterceptor,
                     LoginHandlerInterceptor loginHandlerInterceptor) {
        this.domainPermissionHandlerInterceptor = domainPermissionHandlerInterceptor;
        this.loginHandlerInterceptor = loginHandlerInterceptor;
    }

    /**
     * 注册视图控制器
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 注册错误页面
        registry.addViewController("/400").setViewName("error/400");
        registry.addViewController("/401").setViewName("error/401");
        registry.addViewController("/403").setViewName("error/403");
        registry.addViewController("/404").setViewName("error/404");
        registry.addViewController("/500").setViewName("error/500");
        registry.addViewController("/success").setViewName("error/success");
        registry.addViewController("/fail").setViewName("error/fail");
    }

    /**
     * 配置错误页面
     */
    @Override
    public void registerErrorPages(ErrorPageRegistry registry) {
        ErrorPage error400Page = new ErrorPage(HttpStatus.BAD_REQUEST, "/400");
        ErrorPage error401Page = new ErrorPage(HttpStatus.UNAUTHORIZED, "/401");
        ErrorPage error403Page = new ErrorPage(HttpStatus.FORBIDDEN, "/403");
        ErrorPage error404Page = new ErrorPage(HttpStatus.NOT_FOUND, "/404");
        ErrorPage error500Page = new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/500");
        registry.addErrorPages(error400Page, error401Page, error403Page, error404Page, error500Page);
    }

    /**
     * 注册登录拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginHandlerInterceptor)
                // 拦截的请求
                .addPathPatterns("/**")
                // 不拦截的请求（放行）
                .excludePathPatterns(
                        "/", "/index", "/product_price", "/contact", "/alipay-authentication-redirect",
                        "/login/**", "/admin-login", "/user-login", "/register", "/register-email", "/forget", "/SysUser/registerUser", "/SysUser/registerUserByEmail", "/sign", "/MP_verify_uTqpCgnxTUMc708G.txt", "/robots.txt", "/getWechatQrCode", "/wechatBinding", "/wechatOpenIdLogin", "/kuocaiadmin",
                        "/FaceCertifyVerify/i/**", "/api/**", "/internal/scdn/**",
                        "/400", "/401", "/403", "/404", "/500", "/banned",
                        "/**/front/**", "/**/dashboard/assets/**", "/**/common/**")
                .order(1);
        // 注册支付拦截器
        // 权限拦截器
        registry.addInterceptor(domainPermissionHandlerInterceptor)
                .addPathPatterns("/domain-setting-basic", "/domain-setting-origin", "/domain-setting-https", "/domain-setting-cache", "/domain-setting-access", "/domain-setting-higher")
                .order(3);
    }
}
