package com.kuocai.cdn.component;

import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.config.MyRabbitConfig;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.util.TraceIdUtil;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

//@Component
public class AccessTrack {
    private final RabbitTemplate rabbitTemplate;

    AccessTrack(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    private void send(String message) {
        rabbitTemplate.convertAndSend(MyRabbitConfig.EXCHANGE_NAME, MyRabbitConfig.ACCESS_QUEUE_NAME, message);
    }

    /**
     * 添加访问记录
     *
     * @param request 请求
     * @param sysUser 用户
     */
    public void add(HttpServletRequest request, SysUser sysUser) {
        Long userId = 0L;
        if (sysUser != null) {
            userId = sysUser.getId();
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("userId", userId);
        jsonObject.put("trackId", request.getAttribute(TraceIdUtil.KEY));
        jsonObject.put("ip", getIpAddr(request));
        jsonObject.put("method", request.getMethod());
        jsonObject.put("url", request.getRequestURI());
        jsonObject.put("params", request.getQueryString());
        jsonObject.put("ua", request.getHeader("User-Agent"));
        jsonObject.put("referer", request.getHeader("Referer"));
        jsonObject.put("time", System.currentTimeMillis());
        send(jsonObject.toJSONString());
    }

    /**
     * 获取IP地址
     *
     * @param request 请求
     * @return IP地址
     */
    public String getIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("x-forwarded-for");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
