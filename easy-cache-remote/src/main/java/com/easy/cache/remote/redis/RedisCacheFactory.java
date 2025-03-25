package com.easy.cache.remote.redis;

import com.easy.cache.core.Cache;
import com.easy.cache.core.CacheConfig;
import com.easy.cache.core.CacheLoader;
import com.easy.cache.remote.redis.jedis.JedisCacheFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis缓存工厂
 * <p>
 * 用于创建Redis缓存实例，根据配置选择不同的Redis客户端实现
 */
@Slf4j
public class RedisCacheFactory {

    /**
     * 创建Redis缓存实例
     *
     * @param name        缓存名称
     * @param config      缓存配置
     * @param valueType   值类型
     * @param redisConfig Redis配置
     * @param <K>         键类型
     * @param <V>         值类型
     * @return Redis缓存实例
     */
    public static <K, V> Cache<K, V> createRedisCache(String name, CacheConfig config,
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
            redisConfig = RedisConfig.builder().host("localhost").build();
            log.warn("Redis配置为空，使用默认配置: host=localhost, port=6379");
        }

        // 根据配置选择不同的Redis客户端实现
        RedisConfig.ClientType clientType = redisConfig.getClientType();
        switch (clientType) {
            case JEDIS:
                log.info("使用Jedis客户端");
                return JedisCacheFactory.createJedisCache(name, config, valueType, redisConfig);
            case LETTUCE:
                log.info("Lettuce客户端尚未实现，使用Jedis客户端代替");
                return JedisCacheFactory.createJedisCache(name, config, valueType, redisConfig);
            case REDISSON:
                log.info("Redisson客户端尚未实现，使用Jedis客户端代替");
                return JedisCacheFactory.createJedisCache(name, config, valueType, redisConfig);
            default:
                log.warn("未知的Redis客户端类型：{}，使用Jedis客户端", clientType);
                return JedisCacheFactory.createJedisCache(name, config, valueType, redisConfig);
        }
    }

    /**
     * 创建带加载器的Redis缓存实例
     *
     * @param name        缓存名称
     * @param config      缓存配置
     * @param valueType   值类型
     * @param redisConfig Redis配置
     * @param cacheLoader 缓存加载器
     * @param <K>         键类型
     * @param <V>         值类型
     * @return Redis缓存实例
     */
    public static <K, V> Cache<K, V> createRedisCache(String name, CacheConfig config,
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
            redisConfig = RedisConfig.builder().host("localhost").build();
            log.warn("Redis配置为空，使用默认配置: host=localhost, port=6379");
        }
        if (cacheLoader == null) {
            throw new IllegalArgumentException("缓存加载器不能为空");
        }

        // 根据配置选择不同的Redis客户端实现
        RedisConfig.ClientType clientType = redisConfig.getClientType();
        switch (clientType) {
            case JEDIS:
                log.info("使用Jedis客户端");
                return JedisCacheFactory.createJedisCache(name, config, valueType, redisConfig, cacheLoader);
            case LETTUCE:
                log.info("Lettuce客户端尚未实现，使用Jedis客户端代替");
                return JedisCacheFactory.createJedisCache(name, config, valueType, redisConfig, cacheLoader);
            case REDISSON:
                log.info("Redisson客户端尚未实现，使用Jedis客户端代替");
                return JedisCacheFactory.createJedisCache(name, config, valueType, redisConfig, cacheLoader);
            default:
                log.warn("未知的Redis客户端类型：{}，使用Jedis客户端", clientType);
                return JedisCacheFactory.createJedisCache(name, config, valueType, redisConfig, cacheLoader);
        }
    }
}