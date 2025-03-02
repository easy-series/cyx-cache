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

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Redis缓存测试类
 */
public class RedisCacheTest {
    
    private RedisCache<String, String> cache;
    
    @Mock
    private JedisPool jedisPool;
    
    @Mock
    private Jedis jedis;
    
    private Serializer serializer = new JdkSerializer();
    
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 配置模拟对象
        when(jedisPool.getResource()).thenReturn(jedis);
        
        // 创建Redis缓存
        cache = new RedisCache<>("test-redis-cache", jedisPool, serializer);
    }
    
    @Test
    public void testPutAndGet() {
        // 准备数据
        String key = "key1";
        String value = "value1";
        byte[] keyBytes = serializer.serialize(key);
        byte[] valueBytes = serializer.serialize(value);
        
        // 配置模拟行为
        when(jedis.get(keyBytes)).thenReturn(valueBytes);
        
        // 放入缓存
        cache.put(key, value);
        
        // 验证put调用
        verify(jedis).set(any(byte[].class), any(byte[].class));
        
        // 获取缓存
        String cachedValue = cache.get(key);
        
        // 验证
        assertEquals(value, cachedValue);
        verify(jedis).get(any(byte[].class));
    }
    
    @Test
    public void testExpire() {
        // 准备数据
        String key = "key2";
        String value = "value2";
        byte[] keyBytes = serializer.serialize(key);
        byte[] valueBytes = serializer.serialize(value);
        
        // 配置模拟行为
        when(jedis.get(keyBytes)).thenReturn(valueBytes);
        
        // 放入缓存，10秒后过期
        cache.put(key, value, 10, TimeUnit.SECONDS);
        
        // 验证put调用
        verify(jedis).setex(any(byte[].class), eq(10), any(byte[].class));
        
        // 获取缓存
        String cachedValue = cache.get(key);
        
        // 验证
        assertEquals(value, cachedValue);
    }
    
    @Test
    public void testRemove() {
        // 准备数据
        String key = "key3";
        
        // 配置模拟行为
        when(jedis.del(any(byte[].class))).thenReturn(1L);
        
        // 移除缓存
        boolean removed = cache.remove(key);
        
        // 验证
        assertTrue(removed);
        verify(jedis).del(any(byte[].class));
    }
    
    @Test
    public void testGetWithLoader() {
        // 准备数据
        String key = "key4";
        String value = "loaded-key4";
        byte[] keyBytes = serializer.serialize(key);
        
        // 配置模拟行为 - 键不存在
        when(jedis.get(keyBytes)).thenReturn(null);
        
        // 使用加载器获取不存在的键
        String cachedValue = cache.get(key, k -> "loaded-" + k);
        
        // 验证加载器被调用并且值被缓存
        assertEquals(value, cachedValue);
        verify(jedis).get(any(byte[].class));
        verify(jedis).set(any(byte[].class), any(byte[].class));
    }
} 