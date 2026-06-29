package com.kuocai.cdn.annotation;

import java.lang.annotation.*;

/**
 * @author xiaobo
 * @date 2023/4/18
 */

/**
 * description: 权限限制注解声明
 *
 * @author bo
 * @version 1.0
 * @date 2023/4/18 10:55
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuthorLimiter {
}
