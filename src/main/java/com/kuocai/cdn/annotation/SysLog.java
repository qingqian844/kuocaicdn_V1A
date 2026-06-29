package com.kuocai.cdn.annotation;

import java.lang.annotation.*;

/**
 * 自定义操作日志注解
 * 注解放置的目标位置,METHOD是可注解在方法级别上
 *
 * @author bo
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SysLog {

    /**
     * 功能模块
     */
    String module() default "";

    /**
     * 功能描述
     */
    String describe() default "";

}
