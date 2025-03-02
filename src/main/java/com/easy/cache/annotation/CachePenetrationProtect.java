package com.easy.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 缓存穿透保护注解，用于防止缓存穿透
 * 当多个线程同时请求同一个不存在的key时，只有一个线程会去查询数据源，其他线程会等待结果
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CachePenetrationProtect {

    /**
     * 超时时间，单位毫秒，默认为3000ms
     */
    long timeout() default 3000;
} 