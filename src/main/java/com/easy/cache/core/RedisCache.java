package com.easy.cache.core;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Redis缓存实现
 */
public class RedisCache<K, V> implements Cache<K, V> {

    /**
     * 序列化器接口
     */
    public interface Serializer {
        /**
         * 序列化对象
         */
        byte[] serialize(Object obj);

        /**
         * 反序列化对象
         */
        <T> T deserialize(byte[] bytes, Class<T> clazz);
    }

    private final String name;
    private final JedisPool jedisPool;
    private final Serializer serializer;
    private long expireTime = 0;
    private TimeUnit timeUnit = TimeUnit.SECONDS;

    /**
     * 构造函数
     */
    public RedisCache(String name, JedisPool jedisPool, Serializer serializer) {
        this.name = name;
        this.jedisPool = jedisPool;
        this.serializer = serializer;
    }

    /**
     * 设置过期时间
     */
    public void setExpire(long expireTime, TimeUnit timeUnit) {
        this.expireTime = expireTime;
        this.timeUnit = timeUnit;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public V get(K key) {
        if (key == null) {
            return null;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            byte[] keyBytes = serializer.serialize(key.toString());
            byte[] valueBytes = jedis.get(keyBytes);
            if (valueBytes == null) {
                return null;
            }
            @SuppressWarnings("unchecked")
            V value = (V) serializer.deserialize(valueBytes, Object.class);
            return value;
        }
    }

    @Override
    public V get(K key, Function<K, V> loader) {
        V value = get(key);
        if (value == null && loader != null) {
            value = loader.apply(key);
            if (value != null) {
                put(key, value);
            }
        }
        return value;
    }

    @Override
    public void put(K key, V value) {
        if (key == null) {
            return;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            byte[] keyBytes = serializer.serialize(key.toString());
            byte[] valueBytes = serializer.serialize(value);

            if (expireTime > 0) {
                long seconds = timeUnit.toSeconds(expireTime);
                jedis.setex(keyBytes, (int) seconds, valueBytes);
            } else {
                jedis.set(keyBytes, valueBytes);
            }
        }
    }

    @Override
    public void put(K key, V value, long expireTime, TimeUnit timeUnit) {
        if (key == null) {
            return;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            byte[] keyBytes = serializer.serialize(key.toString());
            byte[] valueBytes = serializer.serialize(value);

            long seconds = timeUnit.toSeconds(expireTime);
            jedis.setex(keyBytes, (int) seconds, valueBytes);
        }
    }

    @Override
    public boolean remove(K key) {
        if (key == null) {
            return false;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            byte[] keyBytes = serializer.serialize(key.toString());
            return jedis.del(keyBytes) > 0;
        }
    }

    @Override
    public void clear() {
        // 清空缓存需要谨慎，这里只清除与当前缓存名称相关的键
        try (Jedis jedis = jedisPool.getResource()) {
            String pattern = name + ":*";
            byte[] patternBytes = serializer.serialize(pattern);
            for (byte[] keyBytes : jedis.keys(patternBytes)) {
                jedis.del(keyBytes);
            }
        }
    }
}