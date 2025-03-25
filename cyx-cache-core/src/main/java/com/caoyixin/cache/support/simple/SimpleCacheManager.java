package com.caoyixin.cache.support.simple;

import com.caoyixin.cache.api.Cache;
import com.caoyixin.cache.api.CacheManager;
import com.caoyixin.cache.api.CacheType;
import com.caoyixin.cache.config.CacheConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单缓存管理器，基于LinkedHashMap实现的轻量级本地缓存
 */
@Slf4j
public class SimpleCacheManager implements CacheManager {

    private final Map<String, Cache<?, ?>> caches = new ConcurrentHashMap<>();

    /**
     * 创建简单缓存管理器
     */
    public SimpleCacheManager() {
        log.info("初始化SimpleCacheManager");
    }

    @Override
    public <K, V> Cache<K, V> getCache(String name) {
        @SuppressWarnings("unchecked")
        Cache<K, V> cache = (Cache<K, V>) caches.get(name);
        return cache;
    }

    @Override
    public <K, V> Cache<K, V> createCache(String name, CacheConfig config) {
        validateCacheType(config);

        Cache<K, V> cache = doCreateCache(name, config);
        caches.put(name, cache);
        log.info("创建缓存: {} with config: {}", name, config);
        return cache;
    }

    @Override
    public <K, V> Cache<K, V> getOrCreateCache(String name, CacheConfig config) {
        @SuppressWarnings("unchecked")
        Cache<K, V> cache = (Cache<K, V>) caches.get(name);
        if (cache != null) {
            return cache;
        }

        synchronized (this) {
            @SuppressWarnings("unchecked")
            Cache<K, V> existingCache = (Cache<K, V>) caches.get(name);
            if (existingCache != null) {
                return existingCache;
            }

            return createCache(name, config);
        }
    }

    @Override
    public void removeCache(String name) {
        Cache<?, ?> cache = caches.remove(name);
        if (cache != null) {
            log.info("移除缓存: {}", name);
        }
    }

    @Override
    public Set<String> getCacheNames() {
        return caches.keySet();
    }

    @Override
    public void close() {
        caches.clear();
        log.info("关闭SimpleCacheManager");
    }

    /**
     * 创建缓存实例
     *
     * @param name   缓存名称
     * @param config 缓存配置
     * @param <K>    键类型
     * @param <V>    值类型
     * @return 缓存实例
     */
    @SuppressWarnings("unchecked")
    private <K, V> Cache<K, V> doCreateCache(String name, CacheConfig config) {
        int maxSize = config.getMaxSize() > 0 ? config.getMaxSize() : 100;
        return (Cache<K, V>) new SimpleCache<>(name, maxSize);
    }

    /**
     * 验证缓存类型
     *
     * @param config 缓存配置
     */
    private void validateCacheType(CacheConfig config) {
        if (config.getCacheType() != CacheType.LOCAL) {
            throw new IllegalArgumentException("SimpleCacheManager仅支持LOCAL类型的缓存，不支持: " + config.getCacheType());
        }
    }
}