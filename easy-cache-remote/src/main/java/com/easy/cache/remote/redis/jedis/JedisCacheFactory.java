package com.easy.cache.remote.redis.jedis;

import com.easy.cache.core.Cache;
import com.easy.cache.core.CacheConfig;
import com.easy.cache.core.CacheLoader;
import com.easy.cache.remote.redis.RedisConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * Jedis缓存工厂
 * <p>
 * 用于创建基于Jedis的Redis缓存实例
 */
@Slf4j
public class JedisCacheFactory {

    /**
     * 创建Jedis缓存实例
     *
     * @param name        缓存名称
     * @param config      缓存配置
     * @param valueType   值类型
     * @param redisConfig Redis配置
     * @param <K>         键类型
     * @param <V>         值类型
     * @return Jedis缓存实例
     */
    public static <K, V> Cache<K, V> createJedisCache(String name, CacheConfig config, 
                                                     Class<V> valueType, RedisConfig redisConfig) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("缓存名称不能为空");
        }
        if (config == null) {
            config = CacheConfig.remoteConfig();
        }
        if (valueType == null) {
            throw new IllegalArgumentException("值类型不能为空");
        }
        if (redisConfig == null) {
            throw new IllegalArgumentException("Redis配置不能为空");
        }
        
        log.info("创建Jedis缓存: name={}, expireSeconds={}, host={}, port={}", 
                name, config.getRemoteExpireSeconds(), redisConfig.getHost(), redisConfig.getPort());
        
        return new JedisCache<>(name, config, valueType, redisConfig);
    }
    
    /**
     * 创建带加载器的Jedis缓存实例
     *
     * @param name        缓存名称
     * @param config      缓存配置
     * @param valueType   值类型
     * @param redisConfig Redis配置
     * @param cacheLoader 缓存加载器
     * @param <K>         键类型
     * @param <V>         值类型
     * @return Jedis缓存实例
     */
    public static <K, V> Cache<K, V> createJedisCache(String name, CacheConfig config, 
                                                     Class<V> valueType, RedisConfig redisConfig,
                                                     CacheLoader<K, V> cacheLoader) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("缓存名称不能为空");
        }
        if (config == null) {
            config = CacheConfig.remoteConfig();
        }
        if (valueType == null) {
            throw new IllegalArgumentException("值类型不能为空");
        }
        if (redisConfig == null) {
            throw new IllegalArgumentException("Redis配置不能为空");
        }
        if (cacheLoader == null) {
            throw new IllegalArgumentException("缓存加载器不能为空");
        }
        
        log.info("创建带加载器的Jedis缓存: name={}, expireSeconds={}, host={}, port={}", 
                name, config.getRemoteExpireSeconds(), redisConfig.getHost(), redisConfig.getPort());
        
        return new JedisCache<>(name, config, valueType, redisConfig, cacheLoader);
    }
} 