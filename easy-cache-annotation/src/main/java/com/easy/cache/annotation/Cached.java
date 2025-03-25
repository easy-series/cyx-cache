package com.easy.cache.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 缓存注解
 * <p>
 * 标注了此注解的方法，其返回结果会被缓存
 * 相同参数的调用将直接返回缓存结果而不执行方法体
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Cached {

    /**
     * 缓存区域名称，如不指定则使用 [类名].[方法名] 作为缓存区域
     */
    String area() default "";

    /**
     * 缓存名称，如不指定则使用 [类名].[方法名] 作为缓存名称
     */
    String name() default "";

    /**
     * 缓存键表达式，支持Spring EL表达式
     * <p>
     * 示例：
     * <pre>
     * @Cached(key = "#user.id")
     * public User getUser(User user) {...}
     * </pre>
     */
    String key() default "";

    /**
     * 缓存键生成器的Bean名称
     * <p>
     * 缓存键生成器用于生成缓存键，优先级高于key属性
     */
    String keyGenerator() default "";

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
     * 远程缓存过期时间
     */
    long remoteExpire() default 0;

    /**
     * 条件表达式，满足条件时才缓存结果，支持Spring EL表达式
     * <p>
     * 示例：
     * <pre>
     * @Cached(condition = "#user.id > 0")
     * public User getUser(User user) {...}
     * </pre>
     */
    String condition() default "";

    /**
     * 除非表达式，满足条件时不缓存结果，支持Spring EL表达式
     * <p>
     * 示例：
     * <pre>
     * @Cached(unless = "#result == null")
     * public User getUser(User user) {...}
     * </pre>
     */
    String unless() default "";

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
     * 同步加载
     * <p>
     * 如果为true，则在多线程环境下只有一个线程会执行方法体，其它线程等待结果
     */
    boolean syncLoad() default false;

    /**
     * 等待获取锁的最大时间（毫秒）
     * <p>
     * 仅在syncLoad=true时有效
     */
    long waitLockMillis() default 500;

    /**
     * 是否启用统计
     */
    boolean enableStats() default true;

    /**
     * 缓存键的前缀
     */
    String keyPrefix() default "";
} 