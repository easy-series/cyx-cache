package com.easy.cache.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.easy.cache.annotation.Cached;
import com.easy.cache.support.KeyGenerator;

/**
 * 缓存处理器测试类
 */
public class CacheProcessorTest {
    
    private CacheProcessor processor;
    
    @Mock
    private CacheManager cacheManager;
    
    @Mock
    private KeyGenerator keyGenerator;
    
    @Mock
    private Cache<String, Object> cache;
    
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 创建缓存处理器
        processor = new CacheProcessor(keyGenerator, cacheManager);
        
        // 配置模拟行为
        when(keyGenerator.generate(any(), any(), any())).thenReturn("test-key");
        when(cacheManager.getOrCreateCache(any(QuickConfig.class))).thenReturn(cache);
    }
    
    @Test
    public void testProcessWithCacheHit() throws Exception {
        // 准备数据
        TestService service = new TestService();
        Method method = TestService.class.getMethod("getData", String.class);
        Object[] args = new Object[] { "test" };
        String expectedResult = "cached-result";
        
        // 配置模拟行为 - 缓存命中
        when(cache.get("test-key")).thenReturn(expectedResult);
        
        // 执行处理
        Object result = processor.process(service, method, args, () -> service.getData("test"));
        
        // 验证
        assertEquals(expectedResult, result);
        verify(cache).get("test-key");
        // 缓存命中，不应该调用原方法
        verify(cache, never()).put(anyString(), any(), anyLong(), any());
    }
    
    @Test
    public void testProcessWithCacheMiss() throws Exception {
        // 准备数据
        TestService service = new TestService();
        Method method = TestService.class.getMethod("getData", String.class);
        Object[] args = new Object[] { "test" };
        String expectedResult = "original-result";
        
        // 配置模拟行为 - 缓存未命中
        when(cache.get("test-key")).thenReturn(null);
        
        // 执行处理
        Object result = processor.process(service, method, args, () -> expectedResult);
        
        // 验证
        assertEquals(expectedResult, result);
        verify(cache).get("test-key");
        // 缓存未命中，应该调用原方法并缓存结果
        verify(cache).put(eq("test-key"), eq(expectedResult), anyLong(), any());
    }
    
    @Test
    public void testProcessWithNullResult() throws Exception {
        // 准备数据
        TestService service = new TestService();
        Method method = TestService.class.getMethod("getData", String.class);
        Object[] args = new Object[] { "test" };
        
        // 配置模拟行为 - 缓存未命中，原方法返回null
        when(cache.get("test-key")).thenReturn(null);
        
        // 执行处理
        Object result = processor.process(service, method, args, () -> null);
        
        // 验证
        assertNull(result);
        verify(cache).get("test-key");
        // 默认不缓存null值
        verify(cache, never()).put(eq("test-key"), eq(null), anyLong(), any());
    }
    
    @Test
    public void testProcessWithCacheNull() throws Exception {
        // 准备数据
        TestService service = new TestService();
        Method method = TestService.class.getMethod("getDataCacheNull", String.class);
        Object[] args = new Object[] { "test" };
        
        // 配置模拟行为 - 缓存未命中，原方法返回null
        when(cache.get("test-key")).thenReturn(null);
        
        // 执行处理
        Object result = processor.process(service, method, args, () -> null);
        
        // 验证
        assertNull(result);
        verify(cache).get("test-key");
        // 配置了缓存null值，应该缓存null
        verify(cache).put(eq("test-key"), eq(null), anyLong(), any());
    }
    
    /**
     * 测试服务类
     */
    static class TestService {
        
        @Cached(key = "#param", expire = 3600)
        public String getData(String param) {
            return "original-result";
        }
        
        @Cached(key = "#param", expire = 3600, cacheNull = true)
        public String getDataCacheNull(String param) {
            return null;
        }
    }
} 