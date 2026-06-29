package com.kuocai.cdn.aspect;


import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.annotation.SysLog;
import com.kuocai.cdn.dao.OperationLogDao;
import com.kuocai.cdn.entity.OperationLog;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.BrowserUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;


/**
 * @author xiaobo
 * @date 2022/12/5
 */
@Slf4j
@Aspect
@Component
@SuppressWarnings("all")
public class LogAspect {

    @Resource
    private OperationLogDao operationLogDao;

    /**
     * 设置操作日志切入点 记录操作日志 在注解的位置切入代码
     */
    @Pointcut("@annotation(com.kuocai.cdn.annotation.SysLog)")
    public void operLogPointCut() {

    }

    /**
     * 正常返回通知，拦截用户操作日志，连接点正常执行完成后执行， 如果连接点抛出异常，则不会执行
     *
     * @param joinPoint 切入点
     * @param keys      返回结果
     */
    @AfterReturning(value = "operLogPointCut()", returning = "keys")
    public void saveOperLog(JoinPoint joinPoint, Object keys) {
        // 获取RequestAttributes
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        // 从获取RequestAttributes中获取HttpServletRequest的信息
        HttpServletRequest request = (HttpServletRequest) requestAttributes
                .resolveReference(RequestAttributes.REFERENCE_REQUEST);
        //获取用户信息
        SysUser loginUser = (SysUser) request.getSession().getAttribute("loginUser");
        OperationLog sysOperationLog = new OperationLog();
        try {
            // 从切面织入点处通过反射机制获取织入点处的方法
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            // 获取切入点所在的方法
            Method method = signature.getMethod();
            // 获取操作
            SysLog opLog = method.getAnnotation(SysLog.class);
            if (opLog != null) {
                sysOperationLog.setOpDescribe(opLog.describe());
                sysOperationLog.setModule(opLog.module());
            }
            // 获取请求的类名
            String className = joinPoint.getTarget().getClass().getName();
            // 获取请求的方法名
            String methodName = method.getName();
            methodName = className + "." + methodName;
            sysOperationLog.setId(IdUtil.getSnowflake().nextId());
            sysOperationLog.setMethod(methodName);
            sysOperationLog.setRequest(JSONObject.toJSONString(joinPoint.getArgs()));
            sysOperationLog.setUrl(request.getRequestURI());
            sysOperationLog.setIp(BrowserUtils.getIp(request));
            if (Assert.isEmpty(loginUser)) {
                log.error("记录操作日志失败，{}", sysOperationLog);
                return;
            }
            sysOperationLog.setUserId(loginUser.getId());
            sysOperationLog.setUserName(loginUser.getUserName());
            operationLogDao.insert(sysOperationLog);
        } catch (Exception e) {
            log.error("存储操作日志异常：{}", e.getMessage());
        }
    }
}
