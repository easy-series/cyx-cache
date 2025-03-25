package com.caoyixin.cache.redis;

import com.caoyixin.cache.api.Cache;
import com.caoyixin.cache.api.CacheStats;
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
 * 基于Redis的缓存实现
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
@Slf4j
public class RedisCache<K, V> implements Cache<K, V> {

    private final String name;
    private final RedisTemplate<String, byte[]> redisTemplate;
    private final RedisConnectionFactory connectionFactory;
    private final KeyConvertor<K> keyConvertor;
    private final ValueEncoder<V> valueEncoder;
    private final ValueDecoder<V> valueDecoder;
    private final Duration defaultExpiration;
    private final String keyPrefix;
    private final CacheStats stats;

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
    public RedisCache(String name, RedisTemplate<String, byte[]> redisTemplate,
                      RedisConnectionFactory connectionFactory,
                      KeyConvertor<K> keyConvertor,
                      ValueEncoder<V> valueEncoder,
                      ValueDecoder<V> valueDecoder,
                      Duration defaultExpiration,
                      String keyPrefix) {
        this.name = name;
        this.redisTemplate = redisTemplate;
        this.connectionFactory = connectionFactory;
        this.keyConvertor = keyConvertor;
        this.valueEncoder = valueEncoder;
        this.valueDecoder = valueDecoder;
        this.defaultExpiration = defaultExpiration;
        this.keyPrefix = keyPrefix;
        this.stats = new CacheStats(name);
    }

    @Override
    public V get(K key) {
        if (key == null) {
            return null;
        }

        try {
            String redisKey = buildRedisKey(key);
            byte[] value = redisTemplate.opsForValue().get(redisKey);
            if (value == null || value.length == 0) {
                stats.recordMiss();
                return null;
            }

            stats.recordHit();
            return valueDecoder.decode(value);
        } catch (Exception e) {
            log.error("从Redis获取缓存值异常, cacheName={}, key={}", name, key, e);
            stats.recordMiss();
            throw new CacheException("从Redis获取缓存值异常: " + e.getMessage(), e);
        }
    }

    @Override
    public void put(K key, V value) {
        put(key, value, defaultExpiration);
    }

    @Override
    public void put(K key, V value, Duration ttl) {
        if (key == null) {
            return;
        }

        try {
            String redisKey = buildRedisKey(key);
            byte[] encodedValue = valueEncoder.encode(value);

            if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
                redisTemplate.opsForValue().set(redisKey, encodedValue, ttl);
            } else if (defaultExpiration != null && !defaultExpiration.isZero()) {
                redisTemplate.opsForValue().set(redisKey, encodedValue, defaultExpiration);
            } else {
                redisTemplate.opsForValue().set(redisKey, encodedValue);
            }
        } catch (Exception e) {
            log.error("向Redis存储缓存值异常, cacheName={}, key={}", name, key, e);
            throw new CacheException("向Redis存储缓存值异常: " + e.getMessage(), e);
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        if (map == null || map.isEmpty()) {
            return;
        }

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
        } catch (Exception e) {
            log.error("向Redis批量存储缓存值异常, cacheName={}", name, e);
            throw new CacheException("向Redis批量存储缓存值异常: " + e.getMessage(), e);
        }
    }

    @Override
    public V computeIfAbsent(K key, Function<K, V> loader) {
        return computeIfAbsent(key, loader, defaultExpiration);
    }

    @Override
    public V computeIfAbsent(K key, Function<K, V> loader, Duration ttl) {
        if (key == null) {
            return null;
        }

        V value = get(key);
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
                value = get(key);
                if (value != null) {
                    return value;
                }

                // 加载数据
                stats.recordLoadStart();
                long startTime = System.currentTimeMillis();
                try {
                    value = loader.apply(key);
                    if (value != null) {
                        put(key, value, ttl);
                        stats.recordLoadSuccess(System.currentTimeMillis() - startTime);
                    } else {
                        stats.recordLoadFailure();
                    }
                    return value;
                } catch (Exception e) {
                    stats.recordLoadFailure();
                    log.error("加载缓存值异常, cacheName={}, key={}", name, key, e);
                    throw e;
                }
            } else {
                // 等待一段时间后再次尝试获取数据
                Thread.sleep(100);
                return get(key);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式锁被中断, cacheName={}, key={}", name, key, e);
            throw new CacheException("获取分布式锁被中断", e);
        } finally {
            if (locked) {
                redisTemplate.delete(lockKey);
            }
        }
    }

    @Override
    public boolean remove(K key) {
        if (key == null) {
            return false;
        }

        try {
            String redisKey = buildRedisKey(key);
            Boolean result = redisTemplate.delete(redisKey);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("从Redis删除缓存值异常, cacheName={}, key={}", name, key, e);
            throw new CacheException("从Redis删除缓存值异常: " + e.getMessage(), e);
        }
    }

    @Override
    public void clear() {
        try {
            String pattern = keyPrefix + name + ":*";
            redisTemplate.delete(redisTemplate.keys(pattern));
        } catch (Exception e) {
            log.error("清空Redis缓存异常, cacheName={}", name, e);
            throw new CacheException("清空Redis缓存异常: " + e.getMessage(), e);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public CacheStats stats() {
        return stats;
    }

    @Override
    public boolean tryLock(K key, Duration timeout) {
        if (key == null) {
            return false;
        }

        String lockKey = buildRedisKey(key) + ":lock";
        try {
            if (timeout == null || timeout.isZero() || timeout.isNegative()) {
                Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, new byte[0], Duration.ofMinutes(5));
                return Boolean.TRUE.equals(success);
            } else {
                Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, new byte[0], timeout);
                return Boolean.TRUE.equals(success);
            }
        } catch (Exception e) {
            log.error("获取Redis分布式锁异常, cacheName={}, key={}", name, key, e);
            return false;
        }
    }

    @Override
    public void unlock(K key) {
        if (key == null) {
            return;
        }

        String lockKey = buildRedisKey(key) + ":lock";
        try {
            redisTemplate.delete(lockKey);
        } catch (Exception e) {
            log.error("释放Redis分布式锁异常, cacheName={}, key={}", name, key, e);
        }
    }

    @Override
    public boolean tryLockAndRun(K key, Duration timeout, Runnable action) {
        if (tryLock(key, timeout)) {
            try {
                action.run();
                return true;
            } finally {
                unlock(key);
            }
        }
        return false;
    }

    /**
     * 构建Redis键
     *
     * @param key 原始键
     * @return Redis键
     */
    private String buildRedisKey(K key) {
        String convertedKey = keyConvertor.convert(key);
        return keyPrefix + name + ":" + convertedKey;
    }
}