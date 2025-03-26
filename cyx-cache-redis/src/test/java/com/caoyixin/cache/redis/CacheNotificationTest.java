package com.caoyixin.cache.redis;

import com.caoyixin.cache.api.Cache;
import com.caoyixin.cache.api.CacheManager;
import com.caoyixin.cache.api.CacheType;
import com.caoyixin.cache.api.DistributedLock;
import com.caoyixin.cache.config.CacheConfig;
import com.caoyixin.cache.consistency.ConsistencyStrategyFactory;
import com.caoyixin.cache.consistency.DefaultConsistencyStrategyFactory;
import com.caoyixin.cache.enums.ConsistencyType;
import com.caoyixin.cache.notification.CacheEvent;
import com.caoyixin.cache.notification.CacheEventListener;
import com.caoyixin.cache.notification.CacheNotifier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * 缓存通知机制测试
 */
public class CacheNotificationTest {

    private RedisConnectionFactory connectionFactory;
    private CacheManager multiLevelCacheManager1;
    private CacheManager multiLevelCacheManager2;
    private SimpleLocalCacheManager localCacheManager1;
    private SimpleLocalCacheManager localCacheManager2;
    private RedisCacheNotifier notifier1;
    private RedisCacheNotifier notifier2;
    private String keyPrefix;

    @Before
    public void setUp() {
        connectionFactory = RedisTestConfig.createConnectionFactory();
        keyPrefix = RedisTestConfig.getTestKeyPrefix();

        // 创建两个本地缓存管理器，模拟两个不同的应用实例
        localCacheManager1 = new SimpleLocalCacheManager();
        localCacheManager2 = new SimpleLocalCacheManager();

        // 创建一致性策略工厂
        ConsistencyStrategyFactory strategyFactory = new DefaultConsistencyStrategyFactory();

        // 创建Redis消息组件
        RedisMessageListenerContainer listenerContainer1 = createListenerContainer();
        RedisMessageListenerContainer listenerContainer2 = createListenerContainer();
        RedisTemplate<String, String> stringRedisTemplate1 = createStringRedisTemplate();
        RedisTemplate<String, String> stringRedisTemplate2 = createStringRedisTemplate();

        // 创建消息监听器和通知器
        RedisMessageListener messageListener1 = new RedisMessageListener(
                listenerContainer1,
                stringRedisTemplate1,
                keyPrefix);

        RedisMessageListener messageListener2 = new RedisMessageListener(
                listenerContainer2,
                stringRedisTemplate2,
                keyPrefix);

        notifier1 = new RedisCacheNotifier(
                stringRedisTemplate1,
                messageListener1,
                keyPrefix);

        notifier2 = new RedisCacheNotifier(
                stringRedisTemplate2,
                messageListener2,
                keyPrefix);

        // 设置相互引用
        messageListener1.setCacheNotifier(notifier1);
        messageListener2.setCacheNotifier(notifier2);

        // 创建Redis缓存管理器
        RedisCacheManager redisCacheManager = new RedisCacheManager(connectionFactory, keyPrefix);

        // 创建两个多级缓存管理器，模拟两个不同的应用实例
        multiLevelCacheManager1 = RedisConfig.createRedisCache(
                connectionFactory,
                localCacheManager1,
                keyPrefix,
                strategyFactory);

        multiLevelCacheManager2 = createMultiLevelCacheManager(
                localCacheManager2,
                redisCacheManager,
                notifier2);
    }

    private CacheManager createMultiLevelCacheManager(
            CacheManager localCacheManager,
            CacheManager remoteCacheManager,
            CacheNotifier notifier) {
        // 使用构建器创建多级缓存管理器
        return new com.caoyixin.cache.builder.CacheManagerBuilder()
                .localCacheManager(localCacheManager)
                .remoteCacheManager(remoteCacheManager)
                .notifier(notifier)
                .strategyFactory(new DefaultConsistencyStrategyFactory())
                .withDistributedLock((DistributedLock<Object>) remoteCacheManager)
                .build();
    }

    private RedisMessageListenerContainer createListenerContainer() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.afterPropertiesSet();
        container.start();
        return container;
    }

    private RedisTemplate<String, String> createStringRedisTemplate() {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @After
    public void tearDown() {
        if (multiLevelCacheManager1 != null) {
            multiLevelCacheManager1.close();
        }
        if (multiLevelCacheManager2 != null) {
            multiLevelCacheManager2.close();
        }
        RedisTestConfig.cleanTestEnvironment(connectionFactory);
    }

    @Test
    public void testCacheEventNotification() throws InterruptedException {
        // 创建一个测试事件监听器
        final AtomicBoolean eventReceived = new AtomicBoolean(false);
        final AtomicReference<String> receivedKey = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);

        CacheEventListener listener = new CacheEventListener() {
            @Override
            public void onEvent(CacheEvent event) {
                receivedKey.set((String) event.getKey());
                eventReceived.set(true);
                latch.countDown();
            }
        };

        // 订阅一个测试缓存的事件
        String cacheName = "testNotification";
        notifier2.subscribe(cacheName, listener);

        // 通过第一个通知器发布事件
        String testKey = "notification-key";
        notifier1.notifyUpdate(cacheName, testKey);

        // 等待事件接收
        boolean received = latch.await(3, TimeUnit.SECONDS);

        assertTrue("Event should be received", received);
        assertTrue("Event received flag should be set", eventReceived.get());
        assertEquals("Received key should match", testKey, receivedKey.get());
    }

    @Test
    public void testCacheConsistencyAcrossInstances() throws InterruptedException {
        // 创建两个多级缓存
        String cacheName = "testAcrossInstances";
        CacheConfig config = CacheConfig.builder()
                .name(cacheName)
                .cacheType(CacheType.BOTH)
                .expire(Duration.ofMinutes(5))
                .localExpire(Duration.ofMinutes(2))
                .consistencyType(ConsistencyType.WRITE_THROUGH)
                .build();

        // 在两个实例上创建缓存
        Cache<String, String> cache1 = multiLevelCacheManager1.createCache(cacheName, config);
        Cache<String, String> cache2 = multiLevelCacheManager2.createCache(cacheName, config);

        // 获取各自的本地缓存
        Cache<String, String> localCache1 = localCacheManager1.getCache(cacheName + ":local");
        Cache<String, String> localCache2 = localCacheManager2.getCache(cacheName + ":local");

        // 在实例1上写入数据
        String key = "cross-key";
        String value = "cross-value";
        cache1.put(key, value);

        // 验证实例1上的本地缓存已更新
        assertEquals("Local cache 1 should be updated", value, localCache1.get(key));

        // 等待通知同步
        Thread.sleep(200);

        // 从实例2的本地缓存读取，应该获取到数据
        assertEquals("Local cache 2 should be updated through notification", value, localCache2.get(key));

        // 在实例2上修改数据
        String newValue = "new-cross-value";
        cache2.put(key, newValue);

        // 等待通知同步
        Thread.sleep(200);

        // 检查实例1上的本地缓存是否更新
        assertEquals("Local cache 1 should be updated with new value", newValue, localCache1.get(key));
    }

    @Test
    public void testCacheRemoveNotification() throws InterruptedException {
        // 创建两个多级缓存
        String cacheName = "testRemoveNotification";
        CacheConfig config = CacheConfig.builder()
                .name(cacheName)
                .cacheType(CacheType.BOTH)
                .expire(Duration.ofMinutes(5))
                .localExpire(Duration.ofMinutes(2))
                .consistencyType(ConsistencyType.WRITE_THROUGH)
                .build();

        // 在两个实例上创建缓存
        Cache<String, String> cache1 = multiLevelCacheManager1.createCache(cacheName, config);
        Cache<String, String> cache2 = multiLevelCacheManager2.createCache(cacheName, config);

        // 获取各自的本地缓存
        Cache<String, String> localCache1 = localCacheManager1.getCache(cacheName + ":local");
        Cache<String, String> localCache2 = localCacheManager2.getCache(cacheName + ":local");

        // 在实例1上写入数据
        String key = "remove-key";
        String value = "remove-value";
        cache1.put(key, value);

        // 等待通知同步
        Thread.sleep(200);

        // 验证实例2上的本地缓存已更新
        assertEquals("Local cache 2 should be updated", value, localCache2.get(key));

        // 在实例1上删除数据
        cache1.remove(key);

        // 等待通知同步
        Thread.sleep(200);

        // 检查实例2上的本地缓存是否已删除
        assertNull("Value should be removed from local cache 2", localCache2.get(key));
    }

    @Test
    public void testCacheClearNotification() throws InterruptedException {
        // 创建两个多级缓存
        String cacheName = "testClearNotification";
        CacheConfig config = CacheConfig.builder()
                .name(cacheName)
                .cacheType(CacheType.BOTH)
                .expire(Duration.ofMinutes(5))
                .localExpire(Duration.ofMinutes(2))
                .consistencyType(ConsistencyType.WRITE_THROUGH)
                .build();

        // 在两个实例上创建缓存
        Cache<String, String> cache1 = multiLevelCacheManager1.createCache(cacheName, config);
        Cache<String, String> cache2 = multiLevelCacheManager2.createCache(cacheName, config);

        // 获取各自的本地缓存
        Cache<String, String> localCache1 = localCacheManager1.getCache(cacheName + ":local");
        Cache<String, String> localCache2 = localCacheManager2.getCache(cacheName + ":local");

        // 在实例1上批量写入数据
        for (int i = 0; i < 5; i++) {
            cache1.put("clear-key-" + i, "clear-value-" + i);
        }

        // 等待通知同步
        Thread.sleep(200);

        // 验证实例2上的本地缓存已更新
        for (int i = 0; i < 5; i++) {
            assertEquals("Local cache 2 should contain all values",
                    "clear-value-" + i, localCache2.get("clear-key-" + i));
        }

        // 在实例1上清空缓存
        cache1.clear();

        // 等待通知同步
        Thread.sleep(200);

        // 检查实例2上的本地缓存是否已清空
        for (int i = 0; i < 5; i++) {
            assertNull("Values should be removed from local cache 2",
                    localCache2.get("clear-key-" + i));
        }
    }
}