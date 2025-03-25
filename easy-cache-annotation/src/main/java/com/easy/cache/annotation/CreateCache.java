package com.easy.cache.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 创建缓存注解
 * <p>
 * 标注了此注解的字段会被注入为Cache实例
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CreateCache {

    /**
     * 缓存区域名称，如不指定则使用字段名作为缓存区域
     */
    String area() default "";

    /**
     * 缓存名称，如不指定则使用字段名作为缓存名称
     */
    String name() default "";

    /**
     * 缓存过期时间
     */
    long expire() default 300;

    /**
     * 缓存过期时间单位，默认为秒
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 是否缓存空值
     */
    boolean cacheNull() default true;

    /**
     * 本地缓存过期时间
     */
    long localExpire() default 0;

    /**
     * 本地缓存最大容量
     */
    long localLimit() default 0;

    /**
     * 远程缓存过期时间
     */
    long remoteExpire() default 0;

    /**
     * 是否启用自动刷新
     */
    boolean refresh() default false;

    /**
     * 自动刷新间隔时间
     */
    long refreshInterval() default 0;

    /**
     * 自动刷新时间单位，默认为秒
     */
    TimeUnit refreshTimeUnit() default TimeUnit.SECONDS;

    /**
     * 缓存类型
     */
    CacheType cacheType() default CacheType.MULTILEVEL;

    /**
     * 远程缓存序列化策略
     */
    String serialPolicy() default "";

    /**
     * 是否启用统计
     */
    boolean enableStats() default true;

    /**
     * 缓存键的前缀
     */
    String keyPrefix() default "";

    /**
     * 缓存穿透保护：加载器的Bean名称
     */
    String loader() default "";

    /**
     * 是否同步加载
     */
    boolean syncLoad() default false;

    /**
     * 等待获取锁的最大时间（毫秒）
     */
    long waitLockMillis() default 500;

    /**
     * 是否使用弱引用键
     */
    boolean weakKeys() default false;

    /**
     * 是否使用软引用值
     */
    boolean softValues() default false;
} 