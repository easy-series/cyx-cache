package com.caoyixin.cache.redis;

import com.caoyixin.cache.api.Cache;
import com.caoyixin.cache.api.CacheManager;
import com.caoyixin.cache.api.CacheType;
import com.caoyixin.cache.config.CacheConfig;
import com.caoyixin.cache.exception.CacheException;
import com.caoyixin.cache.serialization.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis缓存管理器
 */
@Slf4j
public class RedisCacheManager implements CacheManager {

    private final RedisConnectionFactory connectionFactory;
    private final RedisTemplate<String, byte[]> redisTemplate;
    private final String keyPrefix;
    private final Map<String, Cache<?, ?>> caches = new ConcurrentHashMap<>();
    private final Map<String, KeyConvertor<?>> keyConvertors = new ConcurrentHashMap<>();
    private final Map<String, ValueEncoder<?>> valueEncoders = new ConcurrentHashMap<>();
    private final Map<String, ValueDecoder<?>> valueDecoders = new ConcurrentHashMap<>();

    /**
     * 创建Redis缓存管理器
     *
     * @param connectionFactory Redis连接工厂
     * @param keyPrefix         键前缀，用于区分不同应用的缓存
     */
    public RedisCacheManager(RedisConnectionFactory connectionFactory, String keyPrefix) {
        this.connectionFactory = connectionFactory;
        this.keyPrefix = keyPrefix == null ? "" : keyPrefix;
        this.redisTemplate = createRedisTemplate();

        // 注册默认的键转换器
        registerKeyConvertor("string", new StringKeyConvertor<Object>());
        registerKeyConvertor("fastjson", new FastjsonKeyConvertor<Object>());
        registerKeyConvertor("jackson", new JacksonKeyConvertor<Object>());

        // 注册默认的值编码器和解码器
        registerValueCodec("java", new JavaValueEncoder(), new JavaValueDecoder());
        registerValueCodec("jackson", new Jackson2ValueEncoder<Object>(), new Jackson2ValueDecoder<>(Object.class));

        log.info("初始化RedisCacheManager, keyPrefix={}", this.keyPrefix);
    }

    /**
     * 创建Redis模板
     *
     * @return Redis模板
     */
    private RedisTemplate<String, byte[]> createRedisTemplate() {
        RedisTemplate<String, byte[]> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(RedisValueSerializer.INSTANCE);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(RedisValueSerializer.INSTANCE);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 注册键转换器
     *
     * @param name         转换器名称
     * @param keyConvertor 键转换器
     */
    public void registerKeyConvertor(String name, KeyConvertor<?> keyConvertor) {
        keyConvertors.put(name, keyConvertor);
        log.info("注册键转换器: {}", name);
    }

    /**
     * 注册值编码器和解码器
     *
     * @param name    编解码器名称
     * @param encoder 值编码器
     * @param decoder 值解码器
     */
    public void registerValueCodec(String name, ValueEncoder<?> encoder, ValueDecoder<?> decoder) {
        valueEncoders.put(name, encoder);
        valueDecoders.put(name, decoder);
        log.info("注册值编解码器: {}", name);
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

        if (caches.containsKey(name)) {
            throw new CacheException("缓存已存在: " + name);
        }

        Cache<K, V> cache = doCreateCache(name, config);
        caches.put(name, cache);

        log.info("创建Redis缓存: {} with config: {}", name, config);
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
            log.info("移除Redis缓存: {}", name);
        }
    }

    @Override
    public Set<String> getCacheNames() {
        return caches.keySet();
    }

    @Override
    public void close() {
        caches.clear();
        log.info("关闭RedisCacheManager");
        // 连接工厂由Spring管理，不需要关闭
    }

    /**
     * 验证缓存类型
     *
     * @param config 缓存配置
     */
    private void validateCacheType(CacheConfig config) {
        if (config.getCacheType() != CacheType.REMOTE) {
            throw new IllegalArgumentException("RedisCacheManager仅支持REMOTE类型的缓存，当前类型: " + config.getCacheType());
        }
    }

    /**
     * 创建Redis缓存
     *
     * @param name   缓存名称
     * @param config 缓存配置
     * @param <K>    键类型
     * @param <V>    值类型
     * @return Redis缓存
     */
    @SuppressWarnings("unchecked")
    private <K, V> Cache<K, V> doCreateCache(String name, CacheConfig config) {
        // 获取键转换器
        String keyConvertorName = config.getKeyConvertor() != null ? config.getKeyConvertor() : "fastjson";
        KeyConvertor<K> keyConvertor = (KeyConvertor<K>) keyConvertors.get(keyConvertorName);
        if (keyConvertor == null) {
            throw new CacheException("未知的键转换器: " + keyConvertorName);
        }

        // 获取值编码器和解码器
        String valueEncoderName = config.getValueEncoder() != null ? config.getValueEncoder() : "java";
        ValueEncoder<V> valueEncoder = (ValueEncoder<V>) valueEncoders.get(valueEncoderName);
        if (valueEncoder == null) {
            throw new CacheException("未知的值编码器: " + valueEncoderName);
        }

        String valueDecoderName = config.getValueDecoder() != null ? config.getValueDecoder() : "java";
        ValueDecoder<V> valueDecoder = (ValueDecoder<V>) valueDecoders.get(valueDecoderName);
        if (valueDecoder == null) {
            throw new CacheException("未知的值解码器: " + valueDecoderName);
        }

        // 创建Redis缓存
        return new RedisCache<>(
                name,
                redisTemplate,
                connectionFactory,
                keyConvertor,
                valueEncoder,
                valueDecoder,
                config.getExpire(),
                keyPrefix);
    }
}