package com.easy.cache.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 缓存更新注解
 * <p>
 * 标注了此注解的方法执行后，会用返回值更新缓存
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheUpdate {

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
     * 
     * <pre>
     * @CacheUpdate(key = "#user.id", value = "#result")
     * public User updateUser(User user) {...}
     * </pre>
     */
    String key() default "";

    /**
     * 缓存值表达式，支持Spring EL表达式
     * <p>
     * 示例：
     * 
     * <pre>
     * @CacheUpdate(key = "#user.id", value = "#result")
     * public User updateUser(User user) {...}
     * </pre>
     */
    String value() default "#result";

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
     * 条件表达式，满足条件时才更新缓存，支持Spring EL表达式
     * <p>
     * 示例：
     * 
     * <pre>
     * @CacheUpdate(condition = "#user.id > 0")
     * public User updateUser(User user) {...}
     * </pre>
     */
    String condition() default "";

    /**
     * 除非表达式，满足条件时不更新缓存，支持Spring EL表达式
     * <p>
     * 示例：
     * 
     * <pre>
     * @CacheUpdate(unless = "#result == null")
     * public User updateUser(User user) {...}
     * </pre>
     */
    String unless() default "";

    /**
     * 缓存类型
     */
    CacheType cacheType() default CacheType.MULTILEVEL;

    /**
     * 是否同步更新
     */
    boolean sync() default false;

    /**
     * 缓存键的前缀
     */
    String keyPrefix() default "";
}