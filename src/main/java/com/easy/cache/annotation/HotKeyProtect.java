package com.easy.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 热点数据保护注解，用于防止缓存击穿
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HotKeyProtect {
    
    /**
     * 访问阈值，超过该阈值会触发保护机制
     */
    int threshold() default 1000;
    
    /**
     * 时间窗口（秒），在该时间窗口内统计访问次数
     */
    long timeWindow() default 60;
    
    /**
     * 本地缓存过期时间（秒），热点数据会在本地缓存中保存更长时间
     */
    long localExpire() default 300;
} 