package com.easy.cache.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * 多级缓存测试类
 */
public class MultiLevelCacheTest {
    
    private MultiLevelCache<String, String> cache;
    
    @Mock
    private Cache<String, String> localCache;
    
    @Mock
    private Cache<String, String> remoteCache;
    
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 创建多级缓存
        cache = new MultiLevelCache<>("test-multi-level-cache", localCache, remoteCache);
    }
    
    @Test
    public void testGetFromLocalCache() {
        // 准备数据
        String key = "key1";
        String value = "value1";
        
        // 配置模拟行为 - 本地缓存命中
        when(localCache.get(key)).thenReturn(value);
        
        // 获取缓存
        String cachedValue = cache.get(key);
        
        // 验证
        assertEquals(value, cachedValue);
        verify(localCache).get(key);
        // 本地缓存命中，不应该查询远程缓存
        verify(remoteCache, never()).get(key);
    }
    
    @Test
    public void testGetFromRemoteCache() {
        // 准备数据
        String key = "key2";
        String value = "value2";
        
        // 配置模拟行为 - 本地缓存未命中，远程缓存命中
        when(localCache.get(key)).thenReturn(null);
        when(remoteCache.get(key)).thenReturn(value);
        
        // 获取缓存
        String cachedValue = cache.get(key);
        
        // 验证
        assertEquals(value, cachedValue);
        verify(localCache).get(key);
        verify(remoteCache).get(key);
        // 应该将远程缓存的值同步到本地缓存
        verify(localCache).put(eq(key), eq(value), anyLong(), any());
    }
    
    @Test
    public void testPutWithWriteThrough() {
        // 准备数据
        String key = "key3";
        String value = "value3";
        
        // 配置写透模式
        cache.setWriteThrough(true);
        
        // 放入缓存
        cache.put(key, value);
        
        // 验证
        verify(localCache).put(eq(key), eq(value), anyLong(), any());
        verify(remoteCache).put(eq(key), eq(value), anyLong(), any());
    }
    
    @Test
    public void testPutWithoutWriteThrough() {
        // 准备数据
        String key = "key4";
        String value = "value4";
        
        // 配置非写透模式
        cache.setWriteThrough(false);
        
        // 放入缓存
        cache.put(key, value);
        
        // 验证
        verify(localCache).put(eq(key), eq(value), anyLong(), any());
        // 非写透模式，不应该写入远程缓存
        verify(remoteCache, never()).put(eq(key), eq(value), anyLong(), any());
    }
    
    @Test
    public void testRemove() {
        // 准备数据
        String key = "key5";
        
        // 配置模拟行为
        when(localCache.remove(key)).thenReturn(true);
        when(remoteCache.remove(key)).thenReturn(true);
        
        // 移除缓存
        boolean removed = cache.remove(key);
        
        // 验证
        assertTrue(removed);
        verify(localCache).remove(key);
        verify(remoteCache).remove(key);
    }
} 