package com.kuocai.cdn.component;

import com.kuocai.cdn.util.TraceIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;

@Slf4j
@Component
public class TraceIdHandlerFilter implements Filter {

    private String getRequestId(HttpServletRequest request) {

        /* Enumeration<String> headers = request.getHeaders(TraceIdUtil.HEADER_NAME);
        while (headers.hasMoreElements()) {
            String header = headers.nextElement();
            log.info(header);
        } */

        String requestId = request.getHeader(TraceIdUtil.HEADER_NAME);

        if (null == requestId) {
            requestId = TraceIdUtil.generateTraceId();
        }

        return requestId;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        HttpServletRequest request = (HttpServletRequest) servletRequest;

        String requestId = getRequestId(request);
        MDC.put(TraceIdUtil.KEY, requestId);
        response.setHeader(TraceIdUtil.HEADER_NAME, requestId);
        request.setAttribute(TraceIdUtil.KEY, requestId);

        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            MDC.clear();
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }
}
