package com.easy.cache.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * 熔断器缓存测试类
 */
public class CircuitBreakerCacheTest {
    
    private CircuitBreakerCache<String, String> cache;
    
    @Mock
    private Cache<String, String> delegate;
    
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 创建熔断器缓存
        cache = new CircuitBreakerCache<>(
            delegate,
            3,                  // 失败阈值
            5, TimeUnit.SECONDS  // 重置超时时间
        );
    }
    
    @Test
    public void testNormalOperation() {
        // 准备数据
        String key = "key1";
        String value = "value1";
        
        // 配置模拟行为
        when(delegate.get(key)).thenReturn(value);
        
        // 获取缓存
        String cachedValue = cache.get(key);
        
        // 验证
        assertEquals(value, cachedValue);
        verify(delegate).get(key);
        assertFalse(cache.isCircuitOpen());
    }
    
    @Test
    public void testCircuitBreaker() {
        // 准备数据
        String key = "key2";
        
        // 配置模拟行为 - 抛出异常
        when(delegate.get(key)).thenThrow(new RuntimeException("模拟异常"));
        
        // 多次调用触发熔断
        for (int i = 0; i < 5; i++) {
            try {
                cache.get(key);
                fail("应该抛出异常");
            } catch (RuntimeException e) {
                // 预期异常
            }
        }
        
        // 验证熔断器已打开
        assertTrue(cache.isCircuitOpen());
        assertEquals(3, cache.getFailureCount());
        
        // 熔断器打开后，不应该调用委托缓存
        reset(delegate);
        String cachedValue = cache.get(key);
        assertNull(cachedValue);
        verify(delegate, never()).get(key);
    }
    
    @Test
    public void testCircuitReset() throws InterruptedException {
        // 准备数据
        String key = "key3";
        String value = "value3";
        
        // 配置模拟行为 - 先抛出异常，然后正常返回
        when(delegate.get(key))
            .thenThrow(new RuntimeException("模拟异常"))
            .thenThrow(new RuntimeException("模拟异常"))
            .thenThrow(new RuntimeException("模拟异常"))
            .thenReturn(value);
        
        // 多次调用触发熔断
        for (int i = 0; i < 3; i++) {
            try {
                cache.get(key);
                fail("应该抛出异常");
            } catch (RuntimeException e) {
                // 预期异常
            }
        }
        
        // 验证熔断器已打开
        assertTrue(cache.isCircuitOpen());
        
        // 等待熔断器重置
        Thread.sleep(6000);
        
        // 熔断器应该已重置
        assertFalse(cache.isCircuitOpen());
        
        // 再次调用应该成功
        String cachedValue = cache.get(key);
        assertEquals(value, cachedValue);
    }
} 