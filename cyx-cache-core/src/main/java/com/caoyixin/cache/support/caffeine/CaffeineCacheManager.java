package com.caoyixin.cache.support.caffeine;

import com.caoyixin.cache.api.Cache;
import com.caoyixin.cache.api.CacheManager;
import com.caoyixin.cache.api.CacheType;
import com.caoyixin.cache.config.CacheConfig;
import com.caoyixin.cache.exception.CacheException;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于Caffeine的缓存管理器
 */
@Slf4j
public class CaffeineCacheManager implements CacheManager {

    private final Map<String, Cache<?, ?>> caches = new ConcurrentHashMap<>();

    /**
     * 创建Caffeine缓存管理器
     */
    public CaffeineCacheManager() {
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getCache(String name) {
        return (Cache<K, V>) caches.get(name);
    }

    @Override
    public <K, V> Cache<K, V> createCache(String name, CacheConfig config) {
        if (caches.containsKey(name)) {
            throw new CacheException("缓存已存在: " + name);
        }

        Cache<K, V> cache = doCreateCache(config);
        caches.put(name, cache);
        return cache;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getOrCreateCache(String name, CacheConfig config) {
        return (Cache<K, V>) caches.computeIfAbsent(name, k -> doCreateCache(config));
    }

    @Override
    public void removeCache(String name) {
        caches.remove(name);
    }

    @Override
    public Collection<String> getCacheNames() {
        return caches.keySet();
    }

    @Override
    public void close() {
        caches.clear();
    }

    /**
     * 创建缓存实例
     *
     * @param config 缓存配置
     * @param <K>    键类型
     * @param <V>    值类型
     * @return 缓存实例
     */
    private <K, V> Cache<K, V> doCreateCache(CacheConfig config) {
        if (config.getCacheType() != CacheType.LOCAL) {
            throw new CacheException("CaffeineCacheManager只支持LOCAL类型的缓存");
        }

        // 使用Caffeine构建器
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .maximumSize(config.getLocalLimit());

        // 设置过期时间
        if (config.getExpire() != null) {
            builder.expireAfterWrite(config.getExpire());
        }

        // 刷新策略
        if (config.getRefreshPolicy() != null && config.getRefreshPolicy().isEnabled()) {
            if (config.getRefreshPolicy().getRefreshInterval() != null) {
                builder.refreshAfterWrite(config.getRefreshPolicy().getRefreshInterval());
            }
        }

        return new CaffeineCache<>(config.getName(), builder);
    }
}