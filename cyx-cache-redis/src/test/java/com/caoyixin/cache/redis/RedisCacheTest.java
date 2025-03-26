package com.caoyixin.cache.redis;

import com.caoyixin.cache.api.Cache;
import com.caoyixin.cache.config.CacheConfig;
import com.caoyixin.cache.serialization.FastjsonKeyConvertor;
import com.caoyixin.cache.serialization.JavaValueDecoder;
import com.caoyixin.cache.serialization.JavaValueEncoder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.Assert.*;

/**
 * Redis缓存核心功能测试
 */
public class RedisCacheTest {

    private RedisConnectionFactory connectionFactory;
    private RedisCacheManager cacheManager;
    private String keyPrefix;

    @Before
    public void setUp() {
        connectionFactory = RedisTestConfig.createConnectionFactory();
        keyPrefix = RedisTestConfig.getTestKeyPrefix();
        cacheManager = new RedisCacheManager(connectionFactory, keyPrefix);
    }

    @After
    public void tearDown() {
        if (cacheManager != null) {
            cacheManager.close();
        }
        RedisTestConfig.cleanTestEnvironment(connectionFactory);
    }

    @Test
    public void testBasicOperations() {
        // 创建缓存
        String cacheName = "testBasicOps";
        CacheConfig config = CacheConfig.builder()
                .name(cacheName)
                .cacheType(com.caoyixin.cache.api.CacheType.REMOTE)
                .expire(Duration.ofMinutes(5))
                .build();

        Cache<String, String> cache = cacheManager.createCache(cacheName, config);
        assertNotNull("Cache should be created", cache);

        // 测试放入和获取
        String key = "test-key";
        String value = "test-value";
        cache.put(key, value);

        String retrieved = cache.get(key);
        assertEquals("Retrieved value should match", value, retrieved);

        // 测试删除
        assertTrue("Remove should return true for existing key", cache.remove(key));
        assertNull("Value should be null after removal", cache.get(key));

        // 测试TTL
        String ttlKey = "ttl-key";
        cache.put(ttlKey, value, Duration.ofMillis(100));

        assertEquals("Value should be available before expiration", value, cache.get(ttlKey));

        try {
            Thread.sleep(200); // 等待过期
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertNull("Value should be null after expiration", cache.get(ttlKey));
    }

    @Test
    public void testBatchOperations() {
        // 创建缓存
        String cacheName = "testBatchOps";
        CacheConfig config = CacheConfig.builder()
                .name(cacheName)
                .cacheType(com.caoyixin.cache.api.CacheType.REMOTE)
                .expire(Duration.ofMinutes(5))
                .build();

        Cache<String, String> cache = cacheManager.createCache(cacheName, config);

        // 批量放入
        Map<String, String> batch = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            batch.put("batch-key-" + i, "batch-value-" + i);
        }

        cache.putAll(batch);

        // 验证批量操作
        for (int i = 0; i < 10; i++) {
            String key = "batch-key-" + i;
            String expectedValue = "batch-value-" + i;
            assertEquals("Batch value should be correctly stored", expectedValue, cache.get(key));
        }

        // 测试清空
        cache.clear();

        for (int i = 0; i < 10; i++) {
            assertNull("Value should be null after clear", cache.get("batch-key-" + i));
        }
    }

    @Test
    public void testComputeIfAbsent() {
        // 创建缓存
        String cacheName = "testCompute";
        CacheConfig config = CacheConfig.builder()
                .name(cacheName)
                .cacheType(com.caoyixin.cache.api.CacheType.REMOTE)
                .expire(Duration.ofMinutes(5))
                .build();

        Cache<String, String> cache = cacheManager.createCache(cacheName, config);

        // 定义计算函数，记录调用次数
        final AtomicInteger computeCount = new AtomicInteger(0);
        Function<String, String> computeFunction = key -> {
            computeCount.incrementAndGet();
            return "computed-" + key;
        };

        // 第一次调用应该执行计算函数
        String key = "compute-key";
        String value = cache.computeIfAbsent(key, computeFunction);

        assertEquals("Computed value should be correct", "computed-" + key, value);
        assertEquals("Compute function should be called once", 1, computeCount.get());

        // 第二次调用应该直接返回缓存值，不再执行计算函数
        value = cache.computeIfAbsent(key, computeFunction);

        assertEquals("Computed value should be correct", "computed-" + key, value);
        assertEquals("Compute function should still be called once", 1, computeCount.get());

        // 测试带TTL的计算
        String ttlKey = "compute-ttl-key";
        value = cache.computeIfAbsent(ttlKey, computeFunction, Duration.ofMillis(100));

        assertEquals("Computed value with TTL should be correct", "computed-" + ttlKey, value);
        assertEquals("Compute function should be called again", 2, computeCount.get());

        try {
            Thread.sleep(200); // 等待过期
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 再次计算过期键，应该再次调用计算函数
        value = cache.computeIfAbsent(ttlKey, computeFunction);

        assertEquals("Recomputed value should be correct", "computed-" + ttlKey, value);
        assertEquals("Compute function should be called one more time", 3, computeCount.get());
    }

    @Test
    public void testConcurrentComputeIfAbsent() throws InterruptedException {
        // 创建缓存
        String cacheName = "testConcurrentCompute";
        CacheConfig config = CacheConfig.builder()
                .name(cacheName)
                .cacheType(com.caoyixin.cache.api.CacheType.REMOTE)
                .expire(Duration.ofMinutes(5))
                .build();

        Cache<String, Integer> cache = cacheManager.createCache(cacheName, config);

        // 定义计算函数，模拟耗时操作
        final AtomicInteger computeCount = new AtomicInteger(0);
        Function<String, Integer> computeFunction = key -> {
            try {
                Thread.sleep(100); // 模拟耗时操作
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return computeCount.incrementAndGet();
        };

        // 多线程并发调用
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        String concurrentKey = "concurrent-key";

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    cache.computeIfAbsent(concurrentKey, computeFunction);
                } finally {
                    latch.countDown();
                }
            });
        }

        // 等待所有线程完成
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue("All threads should complete in time", completed);

        // 计算函数应该只被调用一次
        assertEquals("Only one thread should compute the value", 1, computeCount.get());
        assertEquals("All threads should get the same value", Integer.valueOf(1), cache.get(concurrentKey));

        executor.shutdown();
    }
}