package com.easy.cache.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * 热点数据缓存测试类
 */
public class HotKeyCacheTest {
    
    private HotKeyCache<String, String> cache;
    
    @Mock
    private MultiLevelCache<String, String> delegate;
    
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 创建热点数据缓存
        cache = new HotKeyCache<>(
            delegate,
            3,                  // 访问阈值
            60, TimeUnit.SECONDS, // 时间窗口
            300, TimeUnit.SECONDS  // 本地缓存过期时间
        );
    }
    
    @Test
    public void testHotKeyDetection() {
        // 准备数据
        String key = "hot-key";
        String value = "hot-value";
        
        // 配置模拟行为
        when(delegate.get(key)).thenReturn(value);
        
        // 多次访问同一个键
        for (int i = 0; i < 5; i++) {
            String cachedValue = cache.get(key);
            assertEquals(value, cachedValue);
        }
        
        // 验证键被识别为热点数据
        assertTrue(cache.isHotKey(key));
        
        // 验证热点数据被特殊处理
        verify(delegate, times(2)).putLocal(eq(key), eq(value), eq(300L), eq(TimeUnit.SECONDS));
    }
    
    @Test
    public void testNonHotKey() {
        // 准备数据
        String key = "non-hot-key";
        String value = "non-hot-value";
        
        // 配置模拟行为
        when(delegate.get(key)).thenReturn(value);
        
        // 访问键一次
        String cachedValue = cache.get(key);
        assertEquals(value, cachedValue);
        
        // 验证键未被识别为热点数据
        assertFalse(cache.isHotKey(key));
        
        // 验证非热点数据未被特殊处理
        verify(delegate, never()).putLocal(eq(key), eq(value), eq(300L), eq(TimeUnit.SECONDS));
    }
    
    @Test
    public void testPutHotKey() {
        // 准备数据
        String key = "hot-key-2";
        String value = "hot-value-2";
        
        // 配置模拟行为
        when(delegate.get(key)).thenReturn(value);
        
        // 多次访问同一个键使其成为热点数据
        for (int i = 0; i < 5; i++) {
            cache.get(key);
        }
        
        // 放入热点数据
        cache.put(key, value);
        
        // 验证热点数据被特殊处理
        verify(delegate).putLocal(eq(key), eq(value), eq(300L), eq(TimeUnit.SECONDS));
        verify(delegate).putRemote(eq(key), eq(value), anyLong(), any());
    }
} 