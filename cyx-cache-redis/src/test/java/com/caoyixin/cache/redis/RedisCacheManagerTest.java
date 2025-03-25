package com.caoyixin.cache.redis;

import com.caoyixin.cache.api.Cache;
import com.caoyixin.cache.api.CacheType;
import com.caoyixin.cache.config.CacheConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class RedisCacheManagerTest {

    private RedisCacheManager cacheManager;
    private LettuceConnectionFactory connectionFactory;

    @BeforeEach
    public void setup() {
        // 配置Redis连接
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName("localhost"); // 使用本地Redis
        config.setPort(6379);            // 默认端口
        config.setPassword("123456"); // 设置Redis密码
        // config.setPassword("your_password"); // 如果有密码，取消注释并设置

        connectionFactory = new LettuceConnectionFactory(config);
        connectionFactory.afterPropertiesSet();

        // 创建RedisCacheManager
        cacheManager = new RedisCacheManager(connectionFactory, "test:");
    }

    @Test
    public void testCreateCache() {
        CacheConfig config = new CacheConfig();
        config.setCacheType(CacheType.REMOTE);
        config.setKeyConvertor("string");
        config.setValueEncoder("java");
        config.setValueDecoder("java");
        config.setExpire(Duration.ofMinutes(5));

        Cache<String, String> cache = cacheManager.createCache("testCache", config);

        assertNotNull(cache);
        assertEquals("testCache", cache.getName());

        // 清理之前的数据
        cache.clear();

        // 测试基本操作
        cache.put("key1", "value1");
        assertEquals("value1", cache.get("key1"));

        // 清理测试数据
        cache.clear();
    }

    @Test
    public void testGetOrCreateCache() {
        CacheConfig config = new CacheConfig();
        config.setCacheType(CacheType.REMOTE);

        // 首次调用应该创建缓存
        Cache<String, String> cache1 = cacheManager.getOrCreateCache("testCache", config);
        assertNotNull(cache1);

        // 清理之前的数据
        cache1.clear();

        // 再次调用应该返回同一个缓存
        Cache<String, String> cache2 = cacheManager.getOrCreateCache("testCache", config);
        assertSame(cache1, cache2);

        // 清理测试数据
        cache1.clear();
    }

    @Test
    public void testRemoveCache() {
        CacheConfig config = new CacheConfig();
        config.setCacheType(CacheType.REMOTE);

        cacheManager.createCache("cacheToRemove", config);
        assertNotNull(cacheManager.getCache("cacheToRemove"));

        cacheManager.removeCache("cacheToRemove");
        assertNull(cacheManager.getCache("cacheToRemove"));
    }

    @Test
    public void testGetCacheNames() {
        CacheConfig config = new CacheConfig();
        config.setCacheType(CacheType.REMOTE);

        // 清理可能存在的测试缓存
        cacheManager.removeCache("cache1");
        cacheManager.removeCache("cache2");

        cacheManager.createCache("cache1", config);
        cacheManager.createCache("cache2", config);

        Set<String> cacheNames = cacheManager.getCacheNames();
        assertTrue(cacheNames.contains("cache1"));
        assertTrue(cacheNames.contains("cache2"));

        // 清理测试数据
        cacheManager.removeCache("cache1");
        cacheManager.removeCache("cache2");
    }

    @Test
    public void testInvalidCacheType() {
        CacheConfig config = new CacheConfig();
        config.setCacheType(CacheType.LOCAL); // Redis管理器不支持本地缓存类型

        assertThrows(IllegalArgumentException.class, () -> {
            cacheManager.createCache("invalidCache", config);
        });
    }
}