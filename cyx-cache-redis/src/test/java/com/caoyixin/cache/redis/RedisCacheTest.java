package com.caoyixin.cache.redis;

import com.caoyixin.cache.serialization.Jackson2ValueDecoder;
import com.caoyixin.cache.serialization.Jackson2ValueEncoder;
import com.caoyixin.cache.serialization.StringKeyConvertor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.Serializable;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class RedisCacheTest {

    private RedisCache<String, TestUser> cache;
    private LettuceConnectionFactory connectionFactory;

    @BeforeEach
    public void setup() {
        // 配置Redis连接
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName("localhost"); // 使用本地Redis
        config.setPort(6379); // 默认端口
        config.setPassword("123456"); // 设置Redis密码
        // config.setPassword("your_password"); // 如果有密码，取消注释并设置

        connectionFactory = new LettuceConnectionFactory(config);
        connectionFactory.afterPropertiesSet();

        RedisTemplate<String, byte[]> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setKeySerializer(new org.springframework.data.redis.serializer.StringRedisSerializer());
        redisTemplate.setValueSerializer(RedisValueSerializer.INSTANCE);
        redisTemplate.afterPropertiesSet();

        // 创建RedisCache
        cache = new RedisCache<>(
                "testCache",
                redisTemplate,
                connectionFactory,
                new StringKeyConvertor<String>(),
                new Jackson2ValueEncoder<TestUser>(),
                new Jackson2ValueDecoder<>(TestUser.class),
                Duration.ofMinutes(5),
                "test:");

        // 清空测试使用的key，确保测试环境干净
        cache.clear();
    }

    @Test
    public void testPutAndGet() {
        TestUser user = new TestUser("张三", 30);
        cache.put("user1", user);

        TestUser cachedUser = cache.get("user1");
        assertNotNull(cachedUser);
        assertEquals("张三", cachedUser.getName());
        assertEquals(30, cachedUser.getAge());
    }

    @Test
    public void testExpiration() throws InterruptedException {
        TestUser user = new TestUser("临时用户", 25);
        cache.put("tempUser", user, Duration.ofSeconds(1));

        assertNotNull(cache.get("tempUser"));

        // 等待过期
        TimeUnit.SECONDS.sleep(2);

        assertNull(cache.get("tempUser"), "缓存应该已过期");
    }

    @Test
    public void testPutAll() {
        Map<String, TestUser> users = new HashMap<>();
        users.put("user1", new TestUser("张三", 30));
        users.put("user2", new TestUser("李四", 25));

        cache.putAll(users);

        assertEquals("张三", cache.get("user1").getName());
        assertEquals("李四", cache.get("user2").getName());
    }

    @Test
    public void testRemove() {
        TestUser user = new TestUser("将被删除", 40);
        cache.put("toRemove", user);

        assertNotNull(cache.get("toRemove"));
        assertTrue(cache.remove("toRemove"));
        assertNull(cache.get("toRemove"));
    }

    @Test
    public void testComputeIfAbsent() {
        TestUser user = cache.computeIfAbsent("computed", key -> new TestUser("计算生成", 35));

        assertNotNull(user);
        assertEquals("计算生成", user.getName());
        assertEquals(35, user.getAge());

        // 第二次调用应该直接返回缓存值
        TestUser cachedUser = cache.computeIfAbsent("computed", key -> new TestUser("不会被使用", 99));
        assertEquals("计算生成", cachedUser.getName());
        assertEquals(35, cachedUser.getAge());
    }

    @Test
    public void testClear() {
        cache.put("user1", new TestUser("张三", 30));
        cache.put("user2", new TestUser("李四", 25));

        assertNotNull(cache.get("user1"));
        assertNotNull(cache.get("user2"));

        cache.clear();

        assertNull(cache.get("user1"));
        assertNull(cache.get("user2"));
    }

    @Test
    public void testLock() throws InterruptedException {
        assertTrue(cache.tryLock("lockKey", Duration.ofSeconds(5)));

        // 尝试在另一个线程获取同一个锁应该失败
        Thread thread = new Thread(() -> {
            assertFalse(cache.tryLock("lockKey", Duration.ofMillis(500)));
        });
        thread.start();
        thread.join();

        // 释放锁
        cache.unlock("lockKey");

        // 现在应该可以获取锁了
        assertTrue(cache.tryLock("lockKey", Duration.ofSeconds(5)));
        cache.unlock("lockKey");
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestUser implements Serializable {
        private String name;
        private int age;
    }
}