package com.kuocai.cdn.aspect;

import com.kuocai.cdn.annotation.RateLimiter;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.exception.ServerBusyException;
import com.kuocai.cdn.util.JedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
public class RateLimiterAspect {

    @Before("@annotation(rateLimiter)")
    public void doBefore(JoinPoint joinPoint, RateLimiter rateLimiter) throws Throwable {
        int time = rateLimiter.time();
        int count = rateLimiter.count();
        long total = 1L;
        String combineKey = getCombineKey(rateLimiter, joinPoint);
        try {
            if (JedisUtil.exists(combineKey)) {
                total = JedisUtil.incr(combineKey);  //请求进来，对应的key加1
                if (total > count) {
                    log.error(combineKey + "接口触发限流");
                    throw new ServerBusyException(rateLimiter.limitMsg());
                }
            } else {
                JedisUtil.setIncr(combineKey, time);  //初始化key
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("网络繁忙，请稍候再试");
        }
    }

    /**
     * 获取限流key
     *
     * @param rateLimiter
     * @param point
     * @return
     */
    public String getCombineKey(RateLimiter rateLimiter, JoinPoint point) {
        StringBuffer stringBuffer = new StringBuffer(rateLimiter.key());
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = method.getDeclaringClass();
        stringBuffer.append(targetClass.getName()).append(":").append(method.getName());
        return stringBuffer.toString().replace(".", ":");
    }
}