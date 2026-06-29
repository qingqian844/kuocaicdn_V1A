package com.kuocai.cdn.component;

import com.kuocai.cdn.config.SystemConfig;
import com.kuocai.cdn.dto.resp.RespError;
import com.kuocai.cdn.exception.AuthorityException;
import com.kuocai.cdn.exception.ServerBusyException;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.TraceIdUtil;
import io.sentry.Sentry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    @ModelAttribute
    public void addErrorPageBranding(Map<String, Object> model) {
        if (!model.containsKey("websiteBaseConfig")) {
            model.put("websiteBaseConfig", SystemConfig.websiteBaseConfig);
        }
        if (!model.containsKey("dashboardLogo")
                && Assert.notEmpty(SystemConfig.websiteBaseConfig)
                && Assert.notEmpty(SystemConfig.websiteBaseConfig.getWebsiteLogoImg())) {
            model.put("dashboardLogo", SystemConfig.websiteBaseConfig.getWebsiteLogoImg());
        }
        if (!model.containsKey("dashboardIcon")
                && Assert.notEmpty(SystemConfig.websiteBaseConfig)
                && Assert.notEmpty(SystemConfig.websiteBaseConfig.getWebsiteIconImg())) {
            model.put("dashboardIcon", SystemConfig.websiteBaseConfig.getWebsiteIconImg());
        }
    }

    private static boolean isAjax(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE)) {
            return true;
        }
        String xRequestedWith = request.getHeader("X-Requested-With");
        if (null == xRequestedWith) {
            return false;
        }
        return xRequestedWith.contains("XMLHttpRequest");
        // return request.getHeader("Accept").contains(MediaType.APPLICATION_JSON_VALUE) || (null != request.getHeader("X-Requested-With") && request.getHeader("X-Requested-With").contains("XMLHttpRequest"));
    }

    @ExceptionHandler(value = Exception.class)
    public Object handleException(Exception e, HttpServletRequest request, HttpServletResponse response, Model model) {
        try {
            if (isAjax(request)) {
                RespError error = new RespError();
                String message;
                if (e instanceof ServerBusyException) {
                    error.setCode("SERVER_BUSY");
                    message = "服务器繁忙，请稍后再试";
                    response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                    log.error("服务器繁忙，错误信息：{}", e.getMessage());
                } else if (e instanceof AuthorityException) {
                    error.setCode("NO_AUTHORITY");
                    message = "当前用户无权限访问";
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    log.error("操作权限不足，错误信息：{}", e.getMessage());
                } else {
                    Sentry.captureException(e);
                    error.setCode("INTERNAL_SERVER_ERROR");
                    message = "请求发生错误，请稍后再试";
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    log.error("发生错误：", e);
                }
                Object traceId = request.getAttribute(TraceIdUtil.KEY);
                message += " (" + (traceId != null ? traceId.toString() : "none") + ")";
                error.setMessage(message);

                String requestUrl = "unknown";
                try {
                    requestUrl = request.getRequestURL() != null ? request.getRequestURL().toString() : "unknown";
                } catch (Exception ex) {
                    log.warn("获取 requestURL 异常", ex);
                }
                error.setRequestUrl(requestUrl);

                error.setException("");
                return error;
            } else {
                Sentry.captureException(e);
                log.error("发生错误：", e);
                ModelAndView modelAndView = new ModelAndView();
                modelAndView.setViewName("error/500");
                modelAndView.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
                return modelAndView;
            }
        } catch (Exception handlerEx) {
            Sentry.captureException(handlerEx);
            log.error("全局异常处理器自身抛出异常", handlerEx);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }
}
