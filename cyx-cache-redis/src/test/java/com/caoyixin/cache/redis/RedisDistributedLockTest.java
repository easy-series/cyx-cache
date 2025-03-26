package com.caoyixin.cache.redis;

import com.caoyixin.cache.api.DistributedLock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Redis分布式锁功能测试
 */
public class RedisDistributedLockTest {

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
    public void testBasicLockOperations() {
        DistributedLock<Object> lock = cacheManager;
        String key = "lock-key";

        // 测试加锁和解锁
        assertTrue("Should be able to acquire lock", lock.tryLock(key, Duration.ofSeconds(10)));
        lock.unlock(key);

        // 锁已经释放，应该能再次获取
        assertTrue("Should be able to acquire lock again", lock.tryLock(key, Duration.ofSeconds(10)));
        lock.unlock(key);
    }

    @Test
    public void testLockTimeout() {
        DistributedLock<Object> lock = cacheManager;
        String key = "timeout-lock-key";

        // 获取锁
        assertTrue("Should be able to acquire lock", lock.tryLock(key, Duration.ofMillis(200)));

        // 在锁过期前，另一个尝试应该失败
        assertFalse("Should not be able to acquire lock while held", lock.tryLock(key, Duration.ofSeconds(10)));

        try {
            Thread.sleep(300); // 等待锁过期
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 锁过期后，应该能再次获取
        assertTrue("Should be able to acquire lock after timeout", lock.tryLock(key, Duration.ofSeconds(10)));
        lock.unlock(key);
    }

    @Test
    public void testConcurrentLockAcquisition() throws InterruptedException {
        DistributedLock<Object> lock = cacheManager;
        String key = "concurrent-lock-key";

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(threadCount);

        // 统计成功获取锁的线程数量
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await(); // 等待所有线程就绪后一起开始

                    // 尝试获取锁
                    if (lock.tryLock(key, Duration.ofSeconds(1))) {
                        try {
                            successCount.incrementAndGet();
                            Thread.sleep(100); // 模拟持有锁的操作
                        } finally {
                            lock.unlock(key);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        // 等待所有线程就绪
        boolean allReady = readyLatch.await(5, TimeUnit.SECONDS);
        assertTrue("All threads should be ready", allReady);

        // 开始测试
        startLatch.countDown();

        // 等待所有线程完成
        boolean allCompleted = completeLatch.await(5, TimeUnit.SECONDS);
        assertTrue("All threads should complete in time", allCompleted);

        // 验证只有一个线程成功获取锁
        assertEquals("Only one thread should acquire the lock", 1, successCount.get());

        executor.shutdown();
    }

    @Test
    public void testLockReentry() {
        DistributedLock<Object> lock = cacheManager;
        String key = "reentry-lock-key";

        // Redis分布式锁不支持可重入，第二次获取应该失败
        assertTrue("First lock acquisition should succeed", lock.tryLock(key, Duration.ofSeconds(10)));
        assertFalse("Second lock acquisition should fail (not reentrant)", lock.tryLock(key, Duration.ofSeconds(10)));

        // 释放锁
        lock.unlock(key);

        // 锁释放后应该可以再次获取
        assertTrue("Lock acquisition after unlock should succeed", lock.tryLock(key, Duration.ofSeconds(10)));
        lock.unlock(key);
    }
}