package com.easy.cache.spring;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import com.easy.cache.aop.CacheAspect;
import com.easy.cache.core.CacheManager;

/**
 * 缓存自动配置测试类
 */
@SpringBootTest(classes = CacheAutoConfiguration.class)
@TestPropertySource(properties = {
    "easy.cache.local.enabled=true",
    "easy.cache.local.maximum-size=10000",
    "easy.cache.local.expire-after-write=3600",
    "easy.cache.redis.enabled=true",
    "easy.cache.redis.host=localhost",
    "easy.cache.redis.port=6379",
    "easy.cache.redis.serializer=JSON"
})
public class CacheAutoConfigurationTest {
    
    @Autowired
    private ApplicationContext context;
    
    @Test
    public void testAutoConfiguration() {
        // 验证Bean是否存在
        assertTrue(context.containsBean("cacheManager"));
        assertTrue(context.containsBean("cacheAspect"));
        
        // 获取Bean
        CacheManager cacheManager = context.getBean(CacheManager.class);
        CacheAspect cacheAspect = context.getBean(CacheAspect.class);
        
        // 验证Bean不为空
        assertNotNull(cacheManager);
        assertNotNull(cacheAspect);
    }
    
    @Test
    public void testCacheManagerConfiguration() {
        // 获取缓存管理器
        CacheManager cacheManager = context.getBean(CacheManager.class);
        
        // 验证配置是否生效
        assertEquals(10000, cacheManager.getLocalCacheMaximumSize());
        assertEquals(3600, cacheManager.getDefaultLocalExpiration());
    }
} 