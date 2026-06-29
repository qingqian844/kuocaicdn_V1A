package com.kuocai.cdn.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimiter {
    /**
     * 限流key
     */
    String key() default "ServiceBusy:";

    /**
     * 限流时间,单位秒
     */
    int time() default 1;

    /**
     * 限流次数
     */
    int count() default 500;

    /**
     * 限流后返回的文字
     */
    String limitMsg() default "访问过于频繁，请稍候再试";
}