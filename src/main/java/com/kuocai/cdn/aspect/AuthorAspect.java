package com.kuocai.cdn.aspect;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.kuocai.cdn.annotation.AuthorLimiter;
import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.exception.AuthorityException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author xiaobo
 * @date 2023/4/18
 */
@Slf4j
@Aspect
@Component
public class AuthorAspect {

    @Resource
    private HttpServletResponse response;

    public static final String ADMIN = "admin";

    public static final String REST = "rest";

    /**
     * description: 权限验证
     *
     * @param joinPoint     切入点
     * @param authorLimiter 注解
     * @author bo
     */
    @Before("@annotation(authorLimiter)")
    public void checkAdminPermission(JoinPoint joinPoint, AuthorLimiter authorLimiter) throws AuthorityException {
        // 获取请求的类名
        String className = joinPoint.getTarget().getClass().getName();
        // 获取目标方法所在的类
        Object target = joinPoint.getTarget();
        // 获取类中的字段
        String loginUserRoleCode = (String) BeanUtil.getFieldValue(target, "loginUserRoleCode");
        if (!ADMIN.equals(loginUserRoleCode)) {
            SysUser loginUser = null;
            try {
                BaseController baseController = (BaseController) joinPoint.getTarget();
                loginUser = baseController.getLoginUser();
            } catch (Exception e) {
                log.error("解析当前无权限用户失败，此用户视图访问资源：{}", joinPoint.getSignature());
            }
            if (StrUtil.contains(className, REST)) {
                throw new AuthorityException("当前用户[{}]无权限访问[{}]", loginUser, joinPoint.getSignature()).log();
            } else {
                try {
                    response.sendRedirect("401");
                } catch (IOException e) {
                    // 这里是不会执行的，除非没有401
                    throw new AuthorityException("当前用户[{}]无权限访问[{}]", loginUser, joinPoint.getSignature()).log();
                }
            }
        }
    }
}
