package com.easy.cache.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * 布隆过滤器缓存测试类
 */
public class BloomFilterCacheTest {
    
    private BloomFilterCache<String, String> cache;
    
    @Mock
    private Cache<String, String> delegate;
    
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 创建布隆过滤器缓存
        cache = new BloomFilterCache<>(delegate, 1000, 0.01);
    }
    
    @Test
    public void testPutAndGet() {
        // 准备数据
        String key = "key1";
        String value = "value1";
        
        // 配置模拟行为
        when(delegate.get(key)).thenReturn(value);
        
        // 放入缓存
        cache.put(key, value);
        
        // 验证put调用
        verify(delegate).put(eq(key), eq(value), anyLong(), any());
        
        // 获取缓存
        String cachedValue = cache.get(key);
        
        // 验证
        assertEquals(value, cachedValue);
        verify(delegate).get(key);
    }
    
    @Test
    public void testGetNonExistentKey() {
        // 准备数据
        String key = "non-existent-key";
        
        // 获取不存在的键
        String cachedValue = cache.get(key);
        
        // 验证
        assertNull(cachedValue);
        // 布隆过滤器应该阻止查询委托缓存
        verify(delegate, never()).get(key);
    }
    
    @Test
    public void testGetWithLoader() {
        // 准备数据
        String key = "key2";
        String value = "loaded-key2";
        
        // 使用加载器获取不存在的键
        String cachedValue = cache.get(key, k -> "loaded-" + k);
        
        // 验证加载器被调用
        assertEquals(value, cachedValue);
        // 布隆过滤器应该阻止查询委托缓存，但允许加载器执行
        verify(delegate, never()).get(key);
        // 加载的值应该被放入缓存
        verify(delegate).put(eq(key), eq(value), anyLong(), any());
    }
} 