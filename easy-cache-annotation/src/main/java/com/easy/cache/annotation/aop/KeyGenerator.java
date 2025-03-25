package com.easy.cache.annotation.aop;

import java.lang.reflect.Method;

/**
 * 缓存键生成器接口
 */
@FunctionalInterface
public interface KeyGenerator {

    /**
     * 根据方法和参数生成缓存键
     *
     * @param method    方法
     * @param arguments 方法参数
     * @return 缓存键
     */
    Object generate(Method method, Object[] arguments);
} 