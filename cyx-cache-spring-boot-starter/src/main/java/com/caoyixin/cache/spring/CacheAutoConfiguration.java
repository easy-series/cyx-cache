package com.caoyixin.cache.spring;

import com.caoyixin.cache.api.CacheManager;
import com.caoyixin.cache.builder.CacheManagerBuilder;
import com.caoyixin.cache.multilevel.MultiLevelCacheManager;
import com.caoyixin.cache.redis.RedisMessageListener;
import com.caoyixin.cache.redis.RedisMessagePublisher;
import com.caoyixin.cache.redis.RedisCacheManager;
import com.caoyixin.cache.support.caffeine.CaffeineCacheManager;
import com.caoyixin.cache.support.simple.SimpleCacheManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.UUID;

/**
 * 缓存自动配置类
 */
@Configuration
@EnableConfigurationProperties(CacheProperties.class)
@AutoConfigureAfter(RedisAutoConfiguration.class)
@Slf4j
public class CacheAutoConfiguration {

    /**
     * 创建简单缓存管理器（基于LinkedHashMap）
     */
    @Bean
    @ConditionalOnMissingBean(name = "simpleCacheManager")
    @ConditionalOnProperty(name = "cyx.cache.local.type", havingValue = "simple", matchIfMissing = true)
    public SimpleCacheManager simpleCacheManager() {
        log.info("创建SimpleCacheManager");
        return new SimpleCacheManager();
    }

    /**
     * 创建Caffeine缓存管理器
     */
    @Bean
    @ConditionalOnMissingBean(name = "caffeineCacheManager")
    @ConditionalOnClass(name = "com.github.benmanes.caffeine.cache.Caffeine")
    @ConditionalOnProperty(name = "cyx.cache.local.type", havingValue = "caffeine")
    public CaffeineCacheManager caffeineCacheManager() {
        log.info("创建CaffeineCacheManager");
        return new CaffeineCacheManager();
    }

    /**
     * 创建Redis消息监听容器
     */
    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    @ConditionalOnMissingBean(RedisMessageListenerContainer.class)
    @ConditionalOnProperty(name = "cyx.cache.redis.enabled", havingValue = "true")
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {
        log.info("创建RedisMessageListenerContainer");
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }

    /**
     * 创建Redis缓存管理器
     */
    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    @ConditionalOnMissingBean(name = "redisCacheManager")
    @ConditionalOnProperty(name = "cyx.cache.redis.enabled", havingValue = "true")
    public RedisCacheManager redisCacheManager(
            RedisConnectionFactory connectionFactory,
            CacheProperties cacheProperties) {
        log.info("创建RedisCacheManager");
        return new RedisCacheManager(connectionFactory, cacheProperties.getRedis().getKeyPrefix());
    }

    /**
     * 创建Redis消息发布器
     */
    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    @ConditionalOnMissingBean(RedisMessagePublisher.class)
    @ConditionalOnProperty(name = "cyx.cache.redis.enabled", havingValue = "true")
    public RedisMessagePublisher redisMessagePublisher(
            StringRedisTemplate stringRedisTemplate,
            CacheProperties cacheProperties) {
        String instanceId = cacheProperties.getInstanceId() != null ? cacheProperties.getInstanceId()
                : UUID.randomUUID().toString();
        log.info("创建RedisMessagePublisher, instanceId={}", instanceId);
        return new RedisMessagePublisher(
                stringRedisTemplate,
                cacheProperties.getRedis().getTopicPrefix(),
                instanceId);
    }

    /**
     * 创建Redis消息监听器
     */
    @Bean
    @ConditionalOnBean({ RedisMessageListenerContainer.class, MultiLevelCacheManager.class })
    @ConditionalOnMissingBean(RedisMessageListener.class)
    @ConditionalOnProperty(name = "cyx.cache.redis.enabled", havingValue = "true")
    public RedisMessageListener redisMessageListener(
            RedisMessageListenerContainer listenerContainer,
            StringRedisTemplate stringRedisTemplate,
            MultiLevelCacheManager cacheManager,
            CacheProperties cacheProperties) {
        String instanceId = cacheProperties.getInstanceId() != null ? cacheProperties.getInstanceId()
                : cacheManager.getInstanceId();
        log.info("创建RedisMessageListener, instanceId={}", instanceId);
        return new RedisMessageListener(
                listenerContainer,
                stringRedisTemplate,
                cacheManager,
                cacheProperties.getRedis().getTopicPrefix(),
                instanceId);
    }

    /**
     * 创建复合缓存管理器
     */
    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager cacheManager(CacheProperties cacheProperties) {
        log.info("创建缓存管理器");
        CacheManagerBuilder builder = new CacheManagerBuilder();

        // 配置本地缓存管理器
        if (cacheProperties.getLocal().isEnabled()) {
            if ("caffeine".equals(cacheProperties.getLocal().getType()) &&
                    isPresent("com.github.benmanes.caffeine.cache.Caffeine")) {
                builder.localCacheManager(caffeineCacheManager());
            } else {
                builder.localCacheManager(simpleCacheManager());
            }
        }

        // 配置远程缓存管理器（如果Redis可用）
        if (cacheProperties.getRedis().isEnabled() &&
                isPresent("org.springframework.data.redis.core.RedisTemplate")) {
            try {
                // 获取RedisConnectionFactory，如果不存在就不配置Redis
                Class<?> factoryClass = Class
                        .forName("org.springframework.data.redis.connection.RedisConnectionFactory");
                Object factory = SpringBeanUtils.getBean(factoryClass);
                if (factory != null) {
                    RedisCacheManager redisCacheManager = new RedisCacheManager(
                            (RedisConnectionFactory) factory,
                            cacheProperties.getRedis().getKeyPrefix());
                    builder.remoteCacheManager(redisCacheManager);
                }
            } catch (Exception e) {
                log.warn("Redis组件不可用，不配置远程缓存管理器", e);
            }
        }

        return builder.build();
    }

    /**
     * 检查类是否存在
     *
     * @param className 类名
     * @return 是否存在
     */
    private boolean isPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}