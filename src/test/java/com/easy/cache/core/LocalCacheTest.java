package com.easy.cache.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 本地缓存测试类
 */
public class LocalCacheTest {
    
    private Cache<String, String> cache;
    
    @BeforeEach
    public void setUp() {
        cache = new LocalCache<>("test-local-cache");
    }
    
    @Test
    public void testPutAndGet() {
        // 放入缓存
        cache.put("key1", "value1");
        
        // 获取缓存
        String value = cache.get("key1");
        
        // 验证
        assertEquals("value1", value);
    }
    
    @Test
    public void testExpire() throws InterruptedException {
        // 放入缓存，1秒后过期
        cache.put("key2", "value2", 1, TimeUnit.SECONDS);
        
        // 立即获取，应该存在
        String value = cache.get("key2");
        assertEquals("value2", value);
        
        // 等待2秒
        Thread.sleep(2000);
        
        // 再次获取，应该过期
        value = cache.get("key2");
        assertNull(value);
    }
    
    @Test
    public void testRemove() {
        // 放入缓存
        cache.put("key3", "value3");
        
        // 验证存在
        String value = cache.get("key3");
        assertEquals("value3", value);
        
        // 移除缓存
        boolean removed = cache.remove("key3");
        assertTrue(removed);
        
        // 验证不存在
        value = cache.get("key3");
        assertNull(value);
    }
    
    @Test
    public void testClear() {
        // 放入多个缓存
        cache.put("key4", "value4");
        cache.put("key5", "value5");
        
        // 验证存在
        assertEquals("value4", cache.get("key4"));
        assertEquals("value5", cache.get("key5"));
        
        // 清空缓存
        cache.clear();
        
        // 验证不存在
        assertNull(cache.get("key4"));
        assertNull(cache.get("key5"));
    }
    
    @Test
    public void testGetWithLoader() {
        // 使用加载器获取不存在的键
        String value = cache.get("key6", k -> "loaded-" + k);
        
        // 验证加载器被调用
        assertEquals("loaded-key6", value);
        
        // 验证值已被缓存
        assertEquals("loaded-key6", cache.get("key6"));
    }
} 