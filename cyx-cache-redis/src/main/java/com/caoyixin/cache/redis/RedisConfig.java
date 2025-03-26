package com.caoyixin.cache.redis;

import com.caoyixin.cache.api.CacheManager;
import com.caoyixin.cache.api.DistributedLock;
import com.caoyixin.cache.builder.CacheManagerBuilder;
import com.caoyixin.cache.consistency.ConsistencyStrategyFactory;
import com.caoyixin.cache.notification.CacheNotifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis缓存配置类，用于创建并组装Redis缓存组件
 */
@Slf4j
public class RedisConfig {

    /**
     * 创建Redis缓存系统，包括本地缓存和Redis缓存
     *
     * @param connectionFactory Redis连接工厂
     * @param localCacheManager 本地缓存管理器
     * @param keyPrefix         键前缀，用于区分不同应用的缓存
     * @param strategyFactory   一致性策略工厂
     * @return 多级缓存管理器
     */
    public static CacheManager createRedisCache(
            RedisConnectionFactory connectionFactory,
            CacheManager localCacheManager,
            String keyPrefix,
            ConsistencyStrategyFactory strategyFactory) {

        // 创建Redis组件
        RedisMessageListenerContainer listenerContainer = createListenerContainer(connectionFactory);
        RedisTemplate<String, String> stringRedisTemplate = createStringRedisTemplate(connectionFactory);

        // 创建Redis缓存管理器
        RedisCacheManager redisCacheManager = new RedisCacheManager(connectionFactory, keyPrefix);

        // 创建消息监听器和通知器
        RedisMessageListener messageListener = new RedisMessageListener(
                listenerContainer,
                stringRedisTemplate,
                keyPrefix);

        RedisCacheNotifier cacheNotifier = new RedisCacheNotifier(
                stringRedisTemplate,
                messageListener,
                keyPrefix);

        // 设置相互引用
        messageListener.setCacheNotifier(cacheNotifier);

        // 使用构建器创建多级缓存管理器
        CacheManagerBuilder builder = new CacheManagerBuilder()
                .localCacheManager(localCacheManager)
                .remoteCacheManager(redisCacheManager)
                .notifier(cacheNotifier)
                .strategyFactory(strategyFactory)
                .withDistributedLock(redisCacheManager);

        log.info("创建Redis缓存系统完成, keyPrefix={}", keyPrefix);

        return builder.build();
    }

    /**
     * 创建消息监听容器
     */
    private static RedisMessageListenerContainer createListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.afterPropertiesSet();
        container.start();
        return container;
    }

    /**
     * 创建字符串Redis模板
     */
    private static RedisTemplate<String, String> createStringRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}