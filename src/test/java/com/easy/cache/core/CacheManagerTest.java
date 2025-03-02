package com.easy.cache.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.easy.cache.core.RedisCache.Serializer;
import com.easy.cache.support.JdkSerializer;

import redis.clients.jedis.JedisPool;

/**
 * 缓存管理器测试类
 */
public class CacheManagerTest {
    
    private CacheManager cacheManager;
    
    @Mock
    private JedisPool jedisPool;
    
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 创建缓存管理器
        cacheManager = CacheManager.getInstance();
        cacheManager.setJedisPool(jedisPool);
        cacheManager.setSerializer(new JdkSerializer());
    }
    
    @Test
    public void testGetOrCreateLocalCache() {
        // 创建本地缓存
        Cache<String, String> cache = cacheManager.getOrCreateLocalCache("test-local-cache");
        
        // 验证
        assertNotNull(cache);
        assertTrue(cache instanceof LocalCache);
        assertEquals("test-local-cache", cache.getName());
        
        // 再次获取同名缓存，应该返回相同实例
        Cache<String, String> cache2 = cacheManager.getOrCreateLocalCache("test-local-cache");
        assertSame(cache, cache2);
    }
    
    @Test
    public void testGetOrCreateRedisCache() {
        // 创建Redis缓存
        Cache<String, String> cache = cacheManager.getOrCreateRedisCache("test-redis-cache");
        
        // 验证
        assertNotNull(cache);
        assertTrue(cache instanceof RedisCache);
        assertEquals("test-redis-cache", cache.getName());
        
        // 再次获取同名缓存，应该返回相同实例
        Cache<String, String> cache2 = cacheManager.getOrCreateRedisCache("test-redis-cache");
        assertSame(cache, cache2);
    }
    
    @Test
    public void testGetOrCreateMultiLevelCache() {
        // 创建多级缓存
        Cache<String, String> cache = cacheManager.getOrCreateMultiLevelCache("test-multi-level-cache");
        
        // 验证
        assertNotNull(cache);
        assertTrue(cache instanceof MultiLevelCache);
        assertEquals("test-multi-level-cache", cache.getName());
        
        // 再次获取同名缓存，应该返回相同实例
        Cache<String, String> cache2 = cacheManager.getOrCreateMultiLevelCache("test-multi-level-cache");
        assertSame(cache, cache2);
    }
    
    @Test
    public void testGetOrCreateCacheWithLocalType() {
        // 创建配置
        QuickConfig config = QuickConfig.builder()
                .name("test-local-config-cache")
                .cacheType(CacheType.LOCAL)
                .expire(100, TimeUnit.SECONDS)
                .build();
        
        // 创建缓存
        Cache<String, Object> cache = cacheManager.getOrCreateCache(config);
        
        // 验证
        assertNotNull(cache);
        assertTrue(cache instanceof LocalCache);
        assertEquals("test-local-config-cache", cache.getName());
    }
    
    @Test
    public void testGetOrCreateCacheWithRemoteType() {
        // 创建配置
        QuickConfig config = QuickConfig.builder()
                .name("test-remote-config-cache")
                .cacheType(CacheType.REMOTE)
                .expire(100, TimeUnit.SECONDS)
                .build();
        
        // 创建缓存
        Cache<String, Object> cache = cacheManager.getOrCreateCache(config);
        
        // 验证
        assertNotNull(cache);
        assertTrue(cache instanceof RedisCache);
        assertEquals("test-remote-config-cache", cache.getName());
    }
    
    @Test
    public void testGetOrCreateCacheWithBothType() {
        // 创建配置
        QuickConfig config = QuickConfig.builder()
                .name("test-both-config-cache")
                .cacheType(CacheType.BOTH)
                .expire(100, TimeUnit.SECONDS)
                .build();
        
        // 创建缓存
        Cache<String, Object> cache = cacheManager.getOrCreateCache(config);
        
        // 验证
        assertNotNull(cache);
        assertTrue(cache instanceof MultiLevelCache);
        assertEquals("test-both-config-cache", cache.getName());
    }
} 