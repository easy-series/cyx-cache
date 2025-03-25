package com.easy.cache.remote.redis.jedis;

import com.easy.cache.core.CacheConfig;
import com.easy.cache.core.CacheLoader;
import com.easy.cache.core.serializer.ValueSerializer;
import com.easy.cache.remote.redis.AbstractRedisCache;
import com.easy.cache.remote.redis.RedisConfig;
import com.easy.cache.remote.serializer.SerializerFactory;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.util.Pool;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 基于Jedis的Redis缓存实现
 *
 * @param <K> 缓存键类型
 * @param <V> 缓存值类型
 */
@Slf4j
public class JedisCache<K, V> extends AbstractRedisCache<K, V> {

    private final Class<V> valueType;
    private final JedisPool jedisPool;
    private final RedisConfig redisConfig;

    /**
     * 创建JedisCache实例
     *
     * @param name        缓存名称
     * @param config      缓存配置
     * @param valueType   值类型
     * @param redisConfig Redis配置
     */
    @SuppressWarnings("unchecked")
    public JedisCache(String name, CacheConfig config, Class<V> valueType, RedisConfig redisConfig) {
        super(name, config);
        this.valueType = valueType;
        this.redisConfig = redisConfig;

        // 创建值序列化器
        String valueSerializerType = config.getRemoteValueSerializer();
        SerializerFactory.SerializerType serializerType;

        if ("kryo".equalsIgnoreCase(valueSerializerType)) {
            serializerType = SerializerFactory.SerializerType.KRYO;
        } else if ("jackson".equalsIgnoreCase(valueSerializerType)) {
            serializerType = SerializerFactory.SerializerType.JACKSON;
        } else {
            serializerType = SerializerFactory.SerializerType.JAVA;
        }

        // 使用反射机制设置valueSerializer字段的值
        try {
            java.lang.reflect.Field field = AbstractRedisCache.class.getDeclaredField("valueSerializer");
            field.setAccessible(true);
            field.set(this, SerializerFactory.createValueSerializer(valueType, serializerType));
        } catch (Exception e) {
            throw new RuntimeException("设置值序列化器失败", e);
        }

        // 创建Jedis连接池
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(redisConfig.getMaxTotal());
        poolConfig.setMaxIdle(redisConfig.getMaxIdle());
        poolConfig.setMinIdle(redisConfig.getMinIdle());
        poolConfig.setMaxWait(redisConfig.getMaxWait());

        // 创建连接池
        this.jedisPool = new JedisPool(
                poolConfig,
                redisConfig.getHost(),
                redisConfig.getPort(),
                redisConfig.getConnectionTimeout(),
                redisConfig.getReadTimeout(),
                redisConfig.getPassword(),
                redisConfig.getDatabase(),
                null);
    }

    /**
     * 创建JedisCache实例
     *
     * @param name        缓存名称
     * @param config      缓存配置
     * @param valueType   值类型
     * @param redisConfig Redis配置
     * @param cacheLoader 缓存加载器
     */
    public JedisCache(String name, CacheConfig config, Class<V> valueType, RedisConfig redisConfig,
            CacheLoader<K, V> cacheLoader) {
        super(name, config, cacheLoader);
        this.valueType = valueType;
        this.redisConfig = redisConfig;

        // 创建值序列化器
        String valueSerializerType = config.getRemoteValueSerializer();
        SerializerFactory.SerializerType serializerType;

        if ("kryo".equalsIgnoreCase(valueSerializerType)) {
            serializerType = SerializerFactory.SerializerType.KRYO;
        } else if ("jackson".equalsIgnoreCase(valueSerializerType)) {
            serializerType = SerializerFactory.SerializerType.JACKSON;
        } else {
            serializerType = SerializerFactory.SerializerType.JAVA;
        }

        // 使用反射机制设置valueSerializer字段的值
        try {
            java.lang.reflect.Field field = AbstractRedisCache.class.getDeclaredField("valueSerializer");
            field.setAccessible(true);
            field.set(this, SerializerFactory.createValueSerializer(valueType, serializerType));
        } catch (Exception e) {
            throw new RuntimeException("设置值序列化器失败", e);
        }

        // 创建Jedis连接池
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(redisConfig.getMaxTotal());
        poolConfig.setMaxIdle(redisConfig.getMaxIdle());
        poolConfig.setMinIdle(redisConfig.getMinIdle());
        poolConfig.setMaxWait(redisConfig.getMaxWait());

        // 创建连接池
        this.jedisPool = new JedisPool(
                poolConfig,
                redisConfig.getHost(),
                redisConfig.getPort(),
                redisConfig.getConnectionTimeout(),
                redisConfig.getReadTimeout(),
                redisConfig.getPassword(),
                redisConfig.getDatabase(),
                null);
    }

    /**
     * 执行Jedis操作
     *
     * @param function Jedis操作函数
     * @param <T>      返回值类型
     * @return 操作结果
     */
    private <T> T execute(Function<Jedis, T> function) {
        try (Jedis jedis = jedisPool.getResource()) {
            return function.apply(jedis);
        } catch (JedisException e) {
            log.error("Redis操作异常: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public V get(K key) {
        if (key == null) {
            return null;
        }

        stats.recordRequest();

        String fullKey = getFullKey(key);
        byte[] keyBytes = fullKey.getBytes(StandardCharsets.UTF_8);

        byte[] valueBytes = execute(jedis -> jedis.get(keyBytes));

        if (valueBytes != null) {
            stats.recordHit();
            return valueSerializer.deserialize(valueBytes);
        } else {
            stats.recordMiss();
            return null;
        }
    }

    @Override
    public V get(K key, Callable<V> valueLoader) {
        V value = get(key);
        if (value != null) {
            return value;
        }

        try {
            V newValue = valueLoader.call();
            if (newValue != null || config.isAllowNullValues()) {
                put(key, newValue);
            }
            return newValue;
        } catch (Exception e) {
            log.error("加载缓存值失败: key={}, error={}", key, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<K, V> getAll(Collection<K> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }

        stats.recordRequest();

        List<String> fullKeys = new ArrayList<>(keys.size());
        Map<String, K> keyMapping = new HashMap<>(keys.size());

        for (K key : keys) {
            String fullKey = getFullKey(key);
            fullKeys.add(fullKey);
            keyMapping.put(fullKey, key);
        }

        byte[][] keyArrays = fullKeys.stream()
                .map(k -> k.getBytes(StandardCharsets.UTF_8))
                .toArray(byte[][]::new);

        List<byte[]> valuesBytesList = execute(jedis -> jedis.mget(keyArrays));

        Map<K, V> result = new HashMap<>(keys.size());
        int hitCount = 0;

        for (int i = 0; i < fullKeys.size(); i++) {
            byte[] valueBytes = valuesBytesList.get(i);
            if (valueBytes != null) {
                K originalKey = keyMapping.get(fullKeys.get(i));
                V value = valueSerializer.deserialize(valueBytes);
                result.put(originalKey, value);
                hitCount++;
            }
        }

        if (hitCount > 0) {
            stats.recordHits(hitCount);
        }

        if (hitCount < keys.size()) {
            stats.recordMisses(keys.size() - hitCount);
        }

        return result;
    }

    @Override
    public void put(K key, V value) {
        if (key == null) {
            return;
        }

        String fullKey = getFullKey(key);
        byte[] keyBytes = fullKey.getBytes(StandardCharsets.UTF_8);
        byte[] valueBytes = valueSerializer.serialize(value);

        if (defaultExpireSeconds > 0) {
            execute(jedis -> jedis.setex(keyBytes, (int) defaultExpireSeconds, valueBytes));
        } else {
            execute(jedis -> jedis.set(keyBytes, valueBytes));
        }

        stats.recordWrite();
    }

    @Override
    public void put(K key, V value, long expireSeconds) {
        if (key == null) {
            return;
        }

        String fullKey = getFullKey(key);
        byte[] keyBytes = fullKey.getBytes(StandardCharsets.UTF_8);
        byte[] valueBytes = valueSerializer.serialize(value);

        if (expireSeconds > 0) {
            execute(jedis -> jedis.setex(keyBytes, (int) expireSeconds, valueBytes));
        } else {
            execute(jedis -> jedis.set(keyBytes, valueBytes));
        }

        stats.recordWrite();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        if (map == null || map.isEmpty()) {
            return;
        }

        Map<byte[], byte[]> byteMap = new HashMap<>(map.size());

        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            String fullKey = getFullKey(entry.getKey());
            byte[] keyBytes = fullKey.getBytes(StandardCharsets.UTF_8);
            byte[] valueBytes = valueSerializer.serialize(entry.getValue());
            byteMap.put(keyBytes, valueBytes);
        }

        execute(jedis -> jedis.mset(byteMap));

        // 如果需要设置过期时间，需要单独设置
        if (defaultExpireSeconds > 0) {
            for (byte[] keyBytes : byteMap.keySet()) {
                execute(jedis -> jedis.expire(keyBytes, (int) defaultExpireSeconds));
            }
        }

        stats.recordWrites(map.size());
    }

    @Override
    public void putAll(Map<K, V> map, long expireSeconds) {
        if (map == null || map.isEmpty()) {
            return;
        }

        Map<byte[], byte[]> byteMap = new HashMap<>(map.size());

        for (Map.Entry<K, V> entry : map.entrySet()) {
            String fullKey = getFullKey(entry.getKey());
            byte[] keyBytes = fullKey.getBytes(StandardCharsets.UTF_8);
            byte[] valueBytes = valueSerializer.serialize(entry.getValue());
            byteMap.put(keyBytes, valueBytes);
        }

        execute(jedis -> {
            Pipeline pipeline = jedis.pipelined();
            for (Map.Entry<byte[], byte[]> entry : byteMap.entrySet()) {
                if (expireSeconds > 0) {
                    pipeline.setex(entry.getKey(), (int) expireSeconds, entry.getValue());
                } else {
                    pipeline.set(entry.getKey(), entry.getValue());
                }
            }
            pipeline.sync();
            return null;
        });

        stats.recordWrites(map.size());
    }

    @Override
    public boolean putIfAbsent(K key, V value) {
        if (key == null) {
            return false;
        }

        String fullKey = getFullKey(key);
        byte[] keyBytes = fullKey.getBytes(StandardCharsets.UTF_8);
        byte[] valueBytes = valueSerializer.serialize(value);

        SetParams params = new SetParams().nx();
        if (defaultExpireSeconds > 0) {
            params.ex((int) defaultExpireSeconds);
        }

        String result = execute(jedis -> jedis.set(keyBytes, valueBytes, params));

        boolean success = "OK".equals(result);
        if (success) {
            stats.recordWrite();
        }

        return success;
    }

    @Override
    public void remove(K key) {
        if (key == null) {
            return;
        }

        String fullKey = getFullKey(key);
        byte[] keyBytes = fullKey.getBytes(StandardCharsets.UTF_8);

        long count = execute(jedis -> jedis.del(keyBytes));

        if (count > 0) {
            stats.recordDelete();
        }
    }

    @Override
    public void removeAll(Collection<? extends K> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }

        byte[][] keyArrays = keys.stream()
                .map(this::getFullKey)
                .map(k -> k.getBytes(StandardCharsets.UTF_8))
                .toArray(byte[][]::new);

        long count = execute(jedis -> jedis.del(keyArrays));

        if (count > 0) {
            stats.recordDeletes(count);
        }
    }

    @Override
    public void clear() {
        // 查找所有匹配的键并删除
        String pattern = keyPrefix + "*";
        Set<byte[]> keys = execute(jedis -> {
            Set<byte[]> result = new HashSet<>();
            ScanParams params = new ScanParams().match(pattern).count(100);
            String cursor = "0";
            do {
                ScanResult<byte[]> scanResult = jedis.scan(cursor.getBytes(StandardCharsets.UTF_8), params);
                result.addAll(scanResult.getResult());
                cursor = scanResult.getCursor();
            } while (!"0".equals(cursor));
            return result;
        });

        if (!keys.isEmpty()) {
            long count = execute(jedis -> jedis.del(keys.toArray(new byte[0][0])));
            stats.recordDeletes(count);
        }
    }

    @Override
    public boolean containsKey(K key) {
        if (key == null) {
            return false;
        }

        String fullKey = getFullKey(key);
        byte[] keyBytes = fullKey.getBytes(StandardCharsets.UTF_8);

        return execute(jedis -> jedis.exists(keyBytes));
    }

    @Override
    public void expire(K key, long expireSeconds) {
        if (key == null) {
            return;
        }

        String fullKey = getFullKey(key);
        byte[] keyBytes = fullKey.getBytes(StandardCharsets.UTF_8);

        if (expireSeconds > 0) {
            execute(jedis -> jedis.expire(keyBytes, (int) expireSeconds));
        }
    }

    @Override
    public long ttl(K key) {
        if (key == null) {
            return -2;
        }

        String fullKey = getFullKey(key);
        byte[] keyBytes = fullKey.getBytes(StandardCharsets.UTF_8);

        return execute(jedis -> jedis.ttl(keyBytes));
    }

    @Override
    public long increment(K key, long delta) {
        if (key == null) {
            throw new IllegalArgumentException("键不能为空");
        }

        String fullKey = getFullKey(key);

        long result = execute(jedis -> jedis.incrBy(fullKey, delta));

        // 设置过期时间
        if (defaultExpireSeconds > 0) {
            expire(key, defaultExpireSeconds);
        }

        return result;
    }

    /**
     * 关闭缓存，释放资源
     */
    public void close() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }
}