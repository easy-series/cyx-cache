package com.easy.cache.annotation;

import java.lang.annotation.*;

/**
 * 缓存失效注解
 * <p>
 * 标注了此注解的方法执行后，会使指定的缓存失效
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheInvalidate {

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
     * @CacheInvalidate(key = "#user.id")
     * public void updateUser(User user) {...}
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
     * 条件表达式，满足条件时才使缓存失效，支持Spring EL表达式
     * <p>
     * 示例：
     * 
     * <pre>
     * @CacheInvalidate(condition = "#user.id > 0")
     * public void updateUser(User user) {...}
     * </pre>
     */
    String condition() default "";

    /**
     * 是否使所有缓存失效
     * <p>
     * 如果为true，则会使指定缓存区域下的所有缓存失效，此时key属性无效
     */
    boolean allEntries() default false;

    /**
     * 是否在方法执行前使缓存失效
     */
    boolean beforeInvocation() default false;

    /**
     * 缓存类型
     */
    CacheType cacheType() default CacheType.MULTILEVEL;

    /**
     * 是否同步删除
     */
    boolean sync() default false;

    /**
     * 缓存键的前缀
     */
    String keyPrefix() default "";
}