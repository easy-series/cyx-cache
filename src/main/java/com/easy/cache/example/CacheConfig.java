package com.easy.cache.example;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.easy.cache.annotation.EnableCaching;
import com.easy.cache.aop.CacheAspect;
import com.easy.cache.core.CacheManager;
import com.easy.cache.core.CacheInterceptor;

/**
 * 缓存配置类
 */
@Configuration
@EnableCaching(enableRemoteCache = true, enableCacheSync = true)
public class CacheConfig {

    /**
     * 这个Bean会由框架自动创建，我们不需要手动创建
     */
} 