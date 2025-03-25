package com.easy.cache.local;

import com.easy.cache.core.Cache;
import com.easy.cache.core.CacheConfig;
import com.easy.cache.core.CacheLoader;
import lombok.extern.slf4j.Slf4j;

/**
 * 本地缓存工厂
 * <p>
 * 用于创建本地缓存实例
 */
@Slf4j
public class LocalCacheFactory {

    /**
     * 创建本地缓存实例
     *
     * @param name   缓存名称
     * @param config 缓存配置
     * @param <K>    键类型
     * @param <V>    值类型
     * @return 本地缓存实例
     */
    public static <K, V> Cache<K, V> createLocalCache(String name, CacheConfig config) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("缓存名称不能为空");
        }
        if (config == null) {
            config = CacheConfig.localConfig();
        }
        
        log.info("创建本地缓存: name={}, expireSeconds={}, maxSize={}", 
                name, config.getLocalExpireSeconds(), config.getLocalMaxSize());
        
        return new CaffeineLocalCache<>(name, config);
    }

    /**
     * 创建带加载器的本地缓存实例
     *
     * @param name        缓存名称
     * @param config      缓存配置
     * @param cacheLoader 缓存加载器
     * @param <K>         键类型
     * @param <V>         值类型
     * @return 本地缓存实例
     */
    public static <K, V> Cache<K, V> createLocalCache(String name, CacheConfig config, CacheLoader<K, V> cacheLoader) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("缓存名称不能为空");
        }
        if (config == null) {
            config = CacheConfig.localConfig();
        }
        if (cacheLoader == null) {
            throw new IllegalArgumentException("缓存加载器不能为空");
        }
        
        log.info("创建带加载器的本地缓存: name={}, expireSeconds={}, maxSize={}", 
                name, config.getLocalExpireSeconds(), config.getLocalMaxSize());
        
        return new CaffeineLocalCache<>(name, config, cacheLoader);
    }
} 