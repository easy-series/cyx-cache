package com.easy.cache.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import com.easy.cache.annotation.Cached;
import com.easy.cache.aop.CacheAspect;
import com.easy.cache.core.Cache;
import com.easy.cache.core.CacheManager;
import com.easy.cache.core.CacheType;
import com.easy.cache.core.LocalCache;
import com.easy.cache.spring.CacheAutoConfiguration;

/**
 * 缓存集成测试类
 */
@SpringBootTest
@Import(CacheAutoConfiguration.class)
public class CacheIntegrationTest {
    
    @MockBean
    private CacheAspect cacheAspect;
    
    private CacheManager cacheManager;
    private TestService testService;
    
    @BeforeEach
    public void setUp() {
        cacheManager = CacheManager.getInstance();
        testService = new TestService();
    }
    
    @Test
    public void testCacheChain() {
        // 创建基础缓存
        Cache<String, String> baseCache = new LocalCache<>("base-cache");
        
        // 添加布隆过滤器
        Cache<String, String> bloomCache = cacheManager.wrapWithBloomFilter(baseCache, 1000, 0.01);
        
        // 添加熔断器
        Cache<String, String> circuitCache = cacheManager.wrapWithCircuitBreaker(bloomCache, 3, 5, TimeUnit.SECONDS);
        
        // 测试缓存链
        circuitCache.put("key1", "value1");
        String value = circuitCache.get("key1");
        assertEquals("value1", value);
        
        // 测试布隆过滤器
        String nonExistentValue = circuitCache.get("non-existent-key");
        assertNull(nonExistentValue);
    }
    
    @Test
    public void testCacheDecorators() {
        // 创建本地缓存
        Cache<String, String> localCache = cacheManager.getOrCreateLocalCache("decorator-test-cache");
        
        // 添加统计装饰器
        Cache<String, String> statsCache = cacheManager.wrapWithStats(localCache);
        
        // 测试缓存操作
        statsCache.put("key1", "value1");
        String value = statsCache.get("key1");
        assertEquals("value1", value);
        
        // 获取统计信息
        CacheStats stats = cacheManager.getStats("decorator-test-cache");
        assertNotNull(stats);
        assertEquals(1, stats.getHitCount());
        assertEquals(0, stats.getMissCount());
        assertEquals(1.0, stats.getHitRate());
    }
    
    /**
     * 测试服务类
     */
    static class TestService {
        
        private int callCount = 0;
        
        @Cached(key = "#param", expire = 60, cacheType = CacheType.LOCAL)
        public String getData(String param) {
            callCount++;
            return "data-" + param + "-" + callCount;
        }
        
        public int getCallCount() {
            return callCount;
        }
    }
} 