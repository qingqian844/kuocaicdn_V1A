package com.kuocai.cdn.component;

import com.kuocai.cdn.util.JedisUtil;
import com.kuocai.cdn.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
public class AuthTokenHandlerFilter implements Filter {

    private String getTokenFormCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        // cookies 有可能为空
        if (null == cookies) {
            return null;
        }

        String token = null;

        for (Cookie cookie : cookies) {
            if ("kuocai_cdn_token".equals(cookie.getName())) {
                token = cookie.getValue();
                break;
            }
        }

        return token;
    }

    private void saveState(String token, HttpServletRequest request) {
        Map<String, String> userMap = null;

        if (JedisUtil.exists(String.format("token:%s", token))) {
            try {
                userMap = JwtUtil.getValidationsObjects(token);
            } catch (Exception e) {
                log.error("Auth Token 获取信息失败，错误：{}", e.getMessage());
            }
        }

        // save claims
        if (userMap != null) {
            request.setAttribute(JwtUtil.CLAIMS_KEY, userMap);
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        // 从 cookies 中获取 token
        String token = getTokenFormCookies(request);

        if (token != null) {
            saveState(token, request);
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }
}
