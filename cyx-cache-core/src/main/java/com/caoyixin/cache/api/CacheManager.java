package com.caoyixin.cache.api;

import com.caoyixin.cache.config.CacheConfig;

import java.util.Collection;

/**
 * 缓存管理器接口
 */
public interface CacheManager {
    /**
     * 获取指定名称的缓存
     *
     * @param name 缓存名称
     * @param <K>  键类型
     * @param <V>  值类型
     * @return 缓存对象，如果不存在则返回null
     */
    <K, V> Cache<K, V> getCache(String name);

    /**
     * 创建缓存
     *
     * @param config 缓存配置
     * @param <K>    键类型
     * @param <V>    值类型
     * @return 创建的缓存
     */
    <K, V> Cache<K, V> createCache(String name, CacheConfig config);

    /**
     * 获取或创建缓存
     *
     * @param config 缓存配置
     * @param <K>    键类型
     * @param <V>    值类型
     * @return 已存在或新创建的缓存
     */
    <K, V> Cache<K, V> getOrCreateCache(String name, CacheConfig config);

    /**
     * 移除缓存
     *
     * @param name 缓存名称
     */
    void removeCache(String name);

    /**
     * 获取所有缓存名称
     *
     * @return 缓存名称集合
     */
    Collection<String> getCacheNames();

    /**
     * 关闭缓存管理器
     */
    default void close() {
        // 默认空实现
    }
}