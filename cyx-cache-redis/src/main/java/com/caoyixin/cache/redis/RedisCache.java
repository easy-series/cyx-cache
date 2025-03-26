package com.caoyixin.cache.redis;

import com.caoyixin.cache.api.AbstractCache;
import com.caoyixin.cache.exception.CacheException;
import com.caoyixin.cache.serialization.KeyConvertor;
import com.caoyixin.cache.serialization.ValueDecoder;
import com.caoyixin.cache.serialization.ValueEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.types.Expiration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 基于Redis的缓存实现，继承AbstractCache以复用通用逻辑
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
@Slf4j
public class RedisCache<K, V> extends AbstractCache<K, V> {

    private final RedisTemplate<String, byte[]> redisTemplate;
    private final RedisConnectionFactory connectionFactory;
    private final KeyConvertor<K> keyConvertor;
    private final ValueEncoder<V> valueEncoder;
    private final ValueDecoder<V> valueDecoder;
    private final Duration defaultExpiration;
    private final String keyPrefix;

    /**
     * 创建Redis缓存
     *
     * @param name              缓存名称
     * @param redisTemplate     Redis模板
     * @param connectionFactory Redis连接工厂
     * @param keyConvertor      键转换器
     * @param valueEncoder      值编码器
     * @param valueDecoder      值解码器
     * @param defaultExpiration 默认过期时间
     * @param keyPrefix         键前缀
     */
    public RedisCache(String name,
            RedisTemplate<String, byte[]> redisTemplate,
            RedisConnectionFactory connectionFactory,
            KeyConvertor<K> keyConvertor,
            ValueEncoder<V> valueEncoder,
            ValueDecoder<V> valueDecoder,
            Duration defaultExpiration,
            String keyPrefix) {
        super(name);
        this.redisTemplate = redisTemplate;
        this.connectionFactory = connectionFactory;
        this.keyConvertor = keyConvertor;
        this.valueEncoder = valueEncoder;
        this.valueDecoder = valueDecoder;
        this.defaultExpiration = defaultExpiration;
        this.keyPrefix = keyPrefix;
    }

    @Override
    protected V doGet(K key) {
        String redisKey = buildRedisKey(key);
        byte[] value = redisTemplate.opsForValue().get(redisKey);
        if (value == null || value.length == 0) {
            return null;
        }

        return valueDecoder.decode(value);
    }

    @Override
    protected void doPut(K key, V value, Duration ttl) {
        String redisKey = buildRedisKey(key);
        byte[] encodedValue = valueEncoder.encode(value);

        Duration expiration = ttl;
        if ((expiration == null || expiration.isZero() || expiration.isNegative())
                && defaultExpiration != null && !defaultExpiration.isZero()) {
            expiration = defaultExpiration;
        }

        if (expiration != null && !expiration.isZero() && !expiration.isNegative()) {
            redisTemplate.opsForValue().set(redisKey, encodedValue, expiration);
        } else {
            redisTemplate.opsForValue().set(redisKey, encodedValue);
        }
    }

    @Override
    protected void doPutAll(Map<? extends K, ? extends V> map) {
        try (RedisConnection connection = RedisConnectionUtils.getConnection(connectionFactory)) {
            for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
                String redisKey = buildRedisKey(entry.getKey());
                byte[] encodedValue = valueEncoder.encode(entry.getValue());
                byte[] keyBytes = redisKey.getBytes();

                if (defaultExpiration != null && !defaultExpiration.isZero()) {
                    connection.set(keyBytes, encodedValue,
                            Expiration.from(defaultExpiration.toMillis(), TimeUnit.MILLISECONDS),
                            RedisStringCommands.SetOption.UPSERT);
                } else {
                    connection.set(keyBytes, encodedValue);
                }
            }
        }
    }

    @Override
    protected V doComputeIfAbsent(K key, Function<K, V> loader, Duration ttl) {
        V value = doGet(key);
        if (value != null) {
            return value;
        }

        // 使用分布式锁保证并发安全
        String lockKey = buildRedisKey(key) + ":lock";
        boolean locked = false;
        try {
            // 尝试获取锁
            Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, new byte[0], Duration.ofSeconds(30));
            locked = Boolean.TRUE.equals(success);

            if (locked) {
                // 二次检查
                value = doGet(key);
                if (value != null) {
                    return value;
                }

                // 加载数据
                value = loader.apply(key);
                if (value != null) {
                    doPut(key, value, ttl);
                }
                return value;
            } else {
                // 等待一段时间后再次尝试获取数据
                Thread.sleep(100);
                return doGet(key);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CacheException("获取分布式锁被中断", e);
        } finally {
            if (locked) {
                redisTemplate.delete(lockKey);
            }
        }
    }

    @Override
    protected boolean doRemove(K key) {
        String redisKey = buildRedisKey(key);
        Boolean result = redisTemplate.delete(redisKey);
        return Boolean.TRUE.equals(result);
    }

    @Override
    protected void doClear() {
        String pattern = keyPrefix + name + ":*";
        redisTemplate.delete(redisTemplate.keys(pattern));
    }

    /**
     * 构建Redis键
     *
     * @param key 缓存键
     * @return Redis键
     */
    private String buildRedisKey(K key) {
        String keyStr = keyConvertor.convert(key);
        return keyPrefix + name + ":" + keyStr;
    }
}