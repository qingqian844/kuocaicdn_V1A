package com.kuocai.cdn.component;

import com.kuocai.cdn.dto.resp.RespError;
import com.kuocai.cdn.dto.resp.RespResult;
import com.kuocai.cdn.exception.AuthorityException;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.exception.ServerBusyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestControllerAdvice
public class GlobalRestExceptionHandler {

    /**
     * 接口限流异常处理
     */
    @ExceptionHandler(value = ServerBusyException.class)
    public RespResult handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.error("服务器繁忙，错误信息：{}", e.getMessage());
        RespError error = new RespError();
        error.setCode("SERVER_BUSY");
        error.setMessage("服务器繁忙，请稍后再试");
        error.setRequestUrl(request.getRequestURL().toString());
        error.setException(e.getClass().getName());
        return error;
    }

    /**
     * 权限异常处理
     */
    @ExceptionHandler(value = AuthorityException.class)
    public RespResult handleAuthorityException(AuthorityException e, HttpServletRequest request) {
        log.error("操作权限不足，错误信息：{}", e.getMessage());
        RespError error = new RespError();
        error.setCode("NO_AUTHORITY");
        error.setMessage("当前用户无权限访问");
        error.setRequestUrl(request.getRequestURL().toString());
        error.setException(e.getClass().getName());
        return error;
    }
}
