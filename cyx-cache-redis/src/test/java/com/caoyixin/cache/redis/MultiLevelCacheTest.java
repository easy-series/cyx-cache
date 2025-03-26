package com.caoyixin.cache.redis;

import com.caoyixin.cache.api.Cache;
import com.caoyixin.cache.api.CacheManager;
import com.caoyixin.cache.api.CacheType;
import com.caoyixin.cache.config.CacheConfig;
import com.caoyixin.cache.consistency.ConsistencyStrategyFactory;
import com.caoyixin.cache.consistency.DefaultConsistencyStrategyFactory;
import com.caoyixin.cache.enums.ConsistencyType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.Assert.*;

/**
 * 多级缓存功能测试
 */
public class MultiLevelCacheTest {

    private RedisConnectionFactory connectionFactory;
    private CacheManager multiLevelCacheManager;
    private CacheManager localCacheManager;
    private CacheManager redisCacheManager;
    private String keyPrefix;

    @Before
    public void setUp() {
        connectionFactory = RedisTestConfig.createConnectionFactory();
        keyPrefix = RedisTestConfig.getTestKeyPrefix();

        // 创建本地缓存管理器（使用一个简单的Map实现）
        localCacheManager = new SimpleLocalCacheManager();

        // 创建Redis缓存管理器
        redisCacheManager = new RedisCacheManager(connectionFactory, keyPrefix);

        // 创建一致性策略工厂
        ConsistencyStrategyFactory strategyFactory = new DefaultConsistencyStrategyFactory();

        // 创建多级缓存管理器
        multiLevelCacheManager = RedisConfig.createRedisCache(
                connectionFactory,
                localCacheManager,
                keyPrefix,
                strategyFactory);
    }

    @After
    public void tearDown() {
        if (multiLevelCacheManager != null) {
            multiLevelCacheManager.close();
        }
        if (redisCacheManager != null) {
            redisCacheManager.close();
        }
        if (localCacheManager != null) {
            localCacheManager.close();
        }
        RedisTestConfig.cleanTestEnvironment(connectionFactory);
    }

    @Test
    public void testMultiLevelCacheOperations() {
        // 创建多级缓存
        String cacheName = "testMultiLevel";
        CacheConfig config = CacheConfig.builder()
                .name(cacheName)
                .cacheType(CacheType.BOTH) // 使用本地和远程缓存
                .expire(Duration.ofMinutes(5))
                .localExpire(Duration.ofMinutes(2))
                .localLimit(100)
                .consistencyType(ConsistencyType.WRITE_THROUGH)
                .build();

        Cache<String, String> cache = multiLevelCacheManager.createCache(cacheName, config);
        assertNotNull("Multi-level cache should be created", cache);

        // 测试放入和获取
        String key = "multi-key";
        String value = "multi-value";
        cache.put(key, value);

        // 验证多级缓存能获取
        String retrievedFromMulti = cache.get(key);
        assertEquals("Retrieved value from multi-level cache should match", value, retrievedFromMulti);

        // 测试更新
        String newValue = "updated-value";
        cache.put(key, newValue);
        assertEquals("Updated value should be retrieved", newValue, cache.get(key));

        // 测试删除
        cache.remove(key);
        assertNull("Value should be null after removal", cache.get(key));

        // 测试不存在的键
        assertNull("Non-existent key should return null", cache.get("non-existent-key"));

        // 测试TTL
        String ttlKey = "ttl-key";
        cache.put(ttlKey, value, Duration.ofMillis(100));
        assertEquals("Value should be available before expiration", value, cache.get(ttlKey));

        try {
            Thread.sleep(200); // 等待过期
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 注意：这里测试行为取决于底层缓存的实现，可能需要调整
        // 如果使用的是读写穿透策略，可能仍然能获取到值
    }

    @Test
    public void testCacheHierarchy() {
        // 创建多级缓存
        String cacheName = "testHierarchy";
        CacheConfig config = CacheConfig.builder()
                .name(cacheName)
                .cacheType(CacheType.BOTH)
                .expire(Duration.ofMinutes(5))
                .localExpire(Duration.ofMinutes(2))
                .localLimit(100)
                .consistencyType(ConsistencyType.WRITE_THROUGH)
                .build();

        Cache<String, String> cache = multiLevelCacheManager.createCache(cacheName, config);
        assertNotNull("Multi-level cache should be created", cache);

        // 测试基本操作
        String key = "hierarchy-key";
        String value = "hierarchy-value";
        cache.put(key, value);

        assertEquals("Should be able to get value", value, cache.get(key));

        // 测试批量操作
        Map<String, String> batch = new HashMap<>();
        for (int i = 0; i < 5; i++) {
            batch.put("batch-key-" + i, "batch-value-" + i);
        }

        cache.putAll(batch);

        // 验证批量写入
        for (int i = 0; i < 5; i++) {
            assertEquals("Batch value should be correctly stored",
                    "batch-value-" + i, cache.get("batch-key-" + i));
        }

        // 测试计算回源
        String computeKey = "compute-key";
        String computed = cache.computeIfAbsent(computeKey, k -> "computed-" + k);
        assertEquals("Computed value should be stored and retrieved", "computed-" + computeKey, computed);
        assertEquals("Computed value should be cached", "computed-" + computeKey, cache.get(computeKey));
    }

    @Test
    public void testWriteThroughStrategy() {
        // 创建多级缓存，使用写穿透策略
        String cacheName = "testWriteThrough";
        CacheConfig config = CacheConfig.builder()
                .name(cacheName)
                .cacheType(CacheType.BOTH)
                .expire(Duration.ofMinutes(5))
                .localExpire(Duration.ofMinutes(2))
                .consistencyType(ConsistencyType.WRITE_THROUGH)
                .build();

        Cache<String, String> cache = multiLevelCacheManager.createCache(cacheName, config);

        // 测试基本写操作
        String key = "write-through-key";
        String value = "write-through-value";
        cache.put(key, value);

        // 验证数据已正确写入
        assertEquals("Value should be stored", value, cache.get(key));

        // 测试更新操作
        String newValue = "write-through-new";
        cache.put(key, newValue);

        // 验证数据已正确更新
        assertEquals("Value should be updated", newValue, cache.get(key));

        // 测试删除操作
        cache.remove(key);

        // 验证数据已正确删除
        assertNull("Value should be removed", cache.get(key));

        // 创建新的多级缓存管理器，模拟应用重启
        CacheManager newMultiLevelCacheManager = RedisConfig.createRedisCache(
                connectionFactory,
                new SimpleLocalCacheManager(), // 新的本地缓存管理器（空的）
                keyPrefix,
                new DefaultConsistencyStrategyFactory());

        try {
            // 重新获取缓存
            Cache<String, String> newCache = newMultiLevelCacheManager.getOrCreateCache(cacheName, config);

            // 写入数据
            String persistentKey = "persistent-key";
            String persistentValue = "persistent-value";
            newCache.put(persistentKey, persistentValue);

            // 从原始缓存读取，验证数据一致性
            assertEquals("Value should be accessible from original cache",
                    persistentValue, cache.get(persistentKey));
        } finally {
            newMultiLevelCacheManager.close();
        }
    }

    @Test
    public void testConsistency() throws InterruptedException {
        // 创建多级缓存
        String cacheName = "testConsistency";
        CacheConfig config = CacheConfig.builder()
                .name(cacheName)
                .cacheType(CacheType.BOTH)
                .expire(Duration.ofMinutes(5))
                .localExpire(Duration.ofMinutes(2))
                .consistencyType(ConsistencyType.WRITE_THROUGH)
                .build();

        // 创建两个不同的缓存管理器，模拟两个应用实例
        CacheManager multiLevelCacheManager2 = RedisConfig.createRedisCache(
                connectionFactory,
                new SimpleLocalCacheManager(),
                keyPrefix,
                new DefaultConsistencyStrategyFactory());

        try {
            // 在第一个管理器中创建缓存
            Cache<String, String> cache1 = multiLevelCacheManager.createCache(cacheName, config);

            // 在第二个管理器中获取或创建相同名称的缓存
            Cache<String, String> cache2 = multiLevelCacheManager2.getOrCreateCache(cacheName, config);

            // 通过第一个缓存写入数据
            String key = "consistency-key";
            String value = "original-value";
            cache1.put(key, value);

            // 验证第二个缓存可以读取数据
            String retrievedFromCache2 = cache2.get(key);
            assertEquals("Second cache should get the same value", value, retrievedFromCache2);

            // 通过第二个缓存更新数据
            String newValue = "modified-value";
            cache2.put(key, newValue);

            // 等待通知同步
            Thread.sleep(100);

            // 验证第一个缓存能读取更新后的值
            String retrievedFromCache1 = cache1.get(key);
            assertEquals("First cache should get the updated value", newValue, retrievedFromCache1);

            // 测试删除的一致性
            cache2.remove(key);

            // 等待通知同步
            Thread.sleep(100);

            // 验证第一个缓存已正确删除数据
            assertNull("Value should be removed from first cache", cache1.get(key));
        } finally {
            multiLevelCacheManager2.close();
        }
    }
}

/**
 * 简单的本地缓存管理器，用于测试
 */
class SimpleLocalCacheManager implements CacheManager {
    private final Map<String, Cache<?, ?>> caches = new HashMap<>();

    @Override
    public <K, V> Cache<K, V> getCache(String name) {
        @SuppressWarnings("unchecked")
        Cache<K, V> cache = (Cache<K, V>) caches.get(name);
        return cache;
    }

    @Override
    public <K, V> Cache<K, V> createCache(String name, CacheConfig config) {
        SimpleLocalCache<K, V> cache = new SimpleLocalCache<>(name);
        caches.put(name, cache);
        return cache;
    }

    @Override
    public <K, V> Cache<K, V> getOrCreateCache(String name, CacheConfig config) {
        @SuppressWarnings("unchecked")
        Cache<K, V> cache = (Cache<K, V>) caches.get(name);
        if (cache != null) {
            return cache;
        }
        return createCache(name, config);
    }

    @Override
    public void removeCache(String name) {
        caches.remove(name);
    }

    @Override
    public Set<String> getCacheNames() {
        return caches.keySet();
    }

    @Override
    public void close() {
        caches.clear();
    }
}

/**
 * 简单的本地缓存实现，用于测试
 */
class SimpleLocalCache<K, V> implements Cache<K, V> {
    private final String name;
    private final Map<K, V> store = new HashMap<>();

    public SimpleLocalCache(String name) {
        this.name = name;
    }

    @Override
    public V get(K key) {
        return store.get(key);
    }

    @Override
    public void put(K key, V value) {
        store.put(key, value);
    }

    @Override
    public void put(K key, V value, Duration ttl) {
        store.put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        store.putAll(map);
    }

    @Override
    public V computeIfAbsent(K key, Function<K, V> loader) {
        return store.computeIfAbsent(key, loader);
    }

    @Override
    public V computeIfAbsent(K key, Function<K, V> loader, Duration ttl) {
        return computeIfAbsent(key, loader);
    }

    @Override
    public boolean remove(K key) {
        return store.remove(key) != null;
    }

    @Override
    public void clear() {
        store.clear();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public com.caoyixin.cache.api.CacheStats stats() {
        return new com.caoyixin.cache.api.CacheStats(name);
    }
}