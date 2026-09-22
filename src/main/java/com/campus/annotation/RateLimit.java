package com.campus.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    // 令牌桶容量（最大令牌数）
    long capacity() default 10;

    // 每秒补充多少个令牌
    long refillRate() default 5;

    // 补充间隔时间单位（通常用秒）
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    // 限流提示消息
    String message() default "请求过于频繁，请稍后再试";

    // 兼容旧版本（如果用了value参数）
    @Deprecated
    int value() default -1;

    // 兼容旧版本
    @Deprecated
    int timeout() default 60;
}