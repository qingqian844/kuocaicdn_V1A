package com.kuocai.cdn.component;

import com.kuocai.cdn.dto.resp.RespResult;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.AdminPathUtils;
import com.kuocai.cdn.util.JedisUtil;
import com.kuocai.cdn.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Nonnull;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 登录拦截器
 *
 * @author XUEW
 * @date 下午8:58 2023/2/12
 */
@Slf4j
@Component
public class LoginHandlerInterceptor implements HandlerInterceptor {

    /**
     * 在目标方式执行之前执行
     */
    @Override
    public boolean preHandle(HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull Object handler) throws Exception {
        if (AdminPathUtils.isConfiguredRequestPath(request.getRequestURI())) {
            return true;
        }
        String requestURI = request.getRequestURI() + "?";
        Map<String, String[]> parameterMap = request.getParameterMap();
        String params = parameterMap.keySet().stream().map(k -> k + "=" + parameterMap.get(k)[0]).collect(Collectors.joining("&"));
        requestURI += params;
        String redirectUrl = "/user-login?callback=" + URLEncoder.encode(requestURI, "UTF-8");
        // 获取 token cookie
        /** Cookie[] cookies = request.getCookies();
         String token = null;
         for (Cookie cookie : cookies) {
         if ("kuocai_cdn_token".equals(cookie.getName())) {
         token = cookie.getValue();
         break;
         }
         }
         if (Assert.isEmpty(token)) {
         log.info("请求已被拦截，原因：尚未登录的用户，Cookie为空");
         response.sendRedirect(redirectUrl);
         return false;
         }
         // 双重验证
         if (!JedisUtil.exists("token:" + token)) {
         log.error("请求已被拦截，原因：Token已过期");
         response.sendRedirect(redirectUrl);
         return false;
         } **/
        // token map
        // Map<String, String> userMap = null;
        /** try {
         userMap = JwtUtil.getValidationsObjects(token);
         } catch (Exception e) {
         // Token 验证不通过
         log.error("请求已被拦截，原因：Token验证未通过，{}", e.getMessage());
         response.sendRedirect(redirectUrl);
         return false;
         }
         Object claims = request.getAttribute(JwtUtil.CLAIMS_KEY);
         if (null == claims) {
         // Auth Token 没有通过
         response.sendRedirect(redirectUrl);
         return false;
         } **/
        Map<String, String> userMap = JwtUtil.claimsFormRequest(request);
        if (Assert.isEmpty(userMap)) {
            // Auth Token To userMap 转换失败
            if (isAjaxRequest(request)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                writeJson(response, RespResult.fail("登录已过期，请重新登录"));
                return false;
            }
            response.sendRedirect(redirectUrl);
            return false;
        }
        // 封禁
        if (JedisUtil.exists(String.format("banned:%s", userMap.get("userId")))) {
            String[] bannedPassUris = {"/logout"};
            if (!Arrays.asList(bannedPassUris).contains(request.getRequestURI())) {
                response.sendRedirect("/banned");
                return false;
            }
        }
        // END 封禁
        // request.setAttribute(JwtUtil.CLAIMS_KEY, userMap);
        // Token 验证通过 传递 userId
        request.setAttribute("userId", Long.valueOf(userMap.get("userId")));
        return true;
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        return "XMLHttpRequest".equalsIgnoreCase(requestedWith)
                || (accept != null && accept.contains("application/json"));
    }

    private void writeJson(HttpServletResponse response, RespResult result) throws Exception {
        response.setContentType("application/json;charset=utf-8");
        response.getWriter().write(result.toString());
    }
}
