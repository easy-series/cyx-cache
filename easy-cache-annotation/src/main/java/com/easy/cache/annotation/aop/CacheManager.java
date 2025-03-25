package com.easy.cache.annotation.aop;

import com.easy.cache.annotation.CacheType;
import com.easy.cache.core.Cache;

/**
 * 缓存管理器接口
 */
public interface CacheManager {

    /**
     * 获取缓存
     *
     * @param area      缓存区域
     * @param name      缓存名称
     * @param cacheType 缓存类型
     * @param <K>       键类型
     * @param <V>       值类型
     * @return 缓存实例
     */
    <K, V> Cache<K, V> getCache(String area, String name, CacheType cacheType);

    /**
     * 获取缓存
     *
     * @param name 缓存名称
     * @param <K>  键类型
     * @param <V>  值类型
     * @return 缓存实例
     */
    default <K, V> Cache<K, V> getCache(String name) {
        return getCache(null, name, CacheType.MULTILEVEL);
    }

    /**
     * 获取缓存
     *
     * @param name      缓存名称
     * @param cacheType 缓存类型
     * @param <K>       键类型
     * @param <V>       值类型
     * @return 缓存实例
     */
    default <K, V> Cache<K, V> getCache(String name, CacheType cacheType) {
        return getCache(null, name, cacheType);
    }

    /**
     * 获取缓存
     *
     * @param area 缓存区域
     * @param name 缓存名称
     * @param <K>  键类型
     * @param <V>  值类型
     * @return 缓存实例
     */
    default <K, V> Cache<K, V> getCache(String area, String name) {
        return getCache(area, name, CacheType.MULTILEVEL);
    }

    /**
     * 移除缓存
     *
     * @param area      缓存区域
     * @param name      缓存名称
     * @param cacheType 缓存类型
     */
    void removeCache(String area, String name, CacheType cacheType);

    /**
     * 清空所有缓存
     */
    void clear();
} 