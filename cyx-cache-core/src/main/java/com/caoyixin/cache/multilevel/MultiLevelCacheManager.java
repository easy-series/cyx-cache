package com.caoyixin.cache.multilevel;

import com.caoyixin.cache.api.*;
import com.caoyixin.cache.config.CacheConfig;
import com.caoyixin.cache.consistency.ConsistencyStrategyFactory;
import com.caoyixin.cache.enums.ConsistencyType;
import com.caoyixin.cache.notification.CacheEvent;
import com.caoyixin.cache.notification.CacheEventType;
import com.caoyixin.cache.notification.CacheNotifier;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多级缓存管理器
 */
@Slf4j
public class MultiLevelCacheManager implements CacheManager {

    private final CacheManager localCacheManager;
    private final CacheManager remoteCacheManager;
    private final Map<String, Cache<?, ?>> caches = new ConcurrentHashMap<>();
    private final String instanceId;
    private final ConsistencyStrategyFactory strategyFactory;
    private final CacheNotifier notifier;
    private final DistributedLock lock;


    /**
     * 创建多级缓存管理器
     *
     * @param localCacheManager  本地缓存管理器
     * @param remoteCacheManager 远程缓存管理器
     * @param notifier           缓存通知器
     * @param strategyFactory    一致性策略工厂
     */
    public MultiLevelCacheManager(CacheManager localCacheManager, CacheManager remoteCacheManager,
                                  CacheNotifier notifier, ConsistencyStrategyFactory strategyFactory, DistributedLock lock) {
        if (localCacheManager == null) {
            throw new IllegalArgumentException("本地缓存管理器不能为空");
        }

        this.localCacheManager = localCacheManager;
        this.remoteCacheManager = remoteCacheManager;
        this.notifier = notifier;
        this.strategyFactory = strategyFactory;
        this.lock = lock;
        this.instanceId = UUID.randomUUID().toString();

        log.info("初始化MultiLevelCacheManager, instanceId={}", instanceId);
    }


    @Override
    public <K, V> Cache<K, V> getCache(String name) {
        @SuppressWarnings("unchecked")
        Cache<K, V> cache = (Cache<K, V>) caches.get(name);
        return cache;
    }

    @Override
    public <K, V> Cache<K, V> createCache(String name, CacheConfig config) {
        validateConfig(config);

        if (caches.containsKey(name)) {
            throw new IllegalArgumentException("缓存已存在: " + name);
        }

        Cache<K, V> cache = doCreateCache(name, config);
        caches.put(name, cache);

        log.info("创建多级缓存: {} with config: {}", name, config);
        return cache;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getOrCreateCache(String name, CacheConfig config) {
        Cache<K, V> cache = (Cache<K, V>) caches.get(name);
        if (cache != null) {
            return cache;
        }

        synchronized (this) {
            @SuppressWarnings("unchecked")
            Cache<K, V> existingCache = (Cache<K, V>) caches.get(name);
            if (existingCache != null) {
                return existingCache;
            }

            return createCache(name, config);
        }
    }

    @Override
    public void removeCache(String name) {
        Cache<?, ?> cache = caches.remove(name);
        if (cache != null) {
            // 从本地和远程缓存管理器中也移除
            localCacheManager.removeCache(name + ":local");
            if (remoteCacheManager != null) {
                remoteCacheManager.removeCache(name + ":remote");
            }

            log.info("移除多级缓存: {}", name);
        }
    }

    @Override
    public Set<String> getCacheNames() {
        return caches.keySet();
    }

    @Override
    public void close() {
        caches.clear();
        localCacheManager.close();
        if (remoteCacheManager != null) {
            remoteCacheManager.close();
        }

        log.info("关闭MultiLevelCacheManager");
    }

    /**
     * 处理缓存更新事件
     *
     * @param event 缓存事件
     */
    public void handleCacheUpdateEvent(CacheEvent event) {
        if (event == null || event.getInstanceId().equals(instanceId)) {
            return; // 忽略自己发出的事件
        }

        Cache<?, ?> cache = caches.get(event.getCacheName());
        if (cache instanceof MultiLevelCache) {
            ((MultiLevelCache<?, ?>) cache).handleCacheUpdate(event);
        }
    }

    /**
     * 发布缓存更新事件
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param type      事件类型
     */
    public void publishCacheUpdateEvent(String cacheName, Object key, CacheEventType type) {
        // 这个方法需要在实际实现中连接到消息中间件（如Redis）
        // 在这里我们只是记录一下日志
        log.debug("发布缓存更新事件: cacheName={}, key={}, type={}", cacheName, key, type);
    }

    @SuppressWarnings("unchecked")
    private <K, V> Cache<K, V> doCreateCache(String name, CacheConfig config) {
        CacheType cacheType = config.getCacheType();

        if (cacheType == CacheType.LOCAL) {
            // 只使用本地缓存
            return (Cache<K, V>) localCacheManager.createCache(name, config);
        } else if (cacheType == CacheType.REMOTE) {
            // 只使用远程缓存
            if (remoteCacheManager == null) {
                throw new IllegalArgumentException("未配置远程缓存管理器，无法创建REMOTE类型的缓存");
            }
            return (Cache<K, V>) remoteCacheManager.createCache(name, config);
        } else if (cacheType == CacheType.BOTH) {
            if (remoteCacheManager == null) {
                throw new IllegalArgumentException("未配置远程缓存管理器，无法创建BOTH类型的缓存");
            }

            // 创建本地和远程缓存
            Cache<K, V> localCache = (Cache<K, V>) localCacheManager.createCache(
                    name + ":local", config);
            Cache<K, V> remoteCache = (Cache<K, V>) remoteCacheManager.createCache(
                    name + ":remote", config);

            // 创建缓存列表，按L1到Ln的顺序
            List<Cache<K, V>> caches = new ArrayList<>(Arrays.asList(localCache, remoteCache));

            // 创建一致性策略
            ConsistencyType consistencyType = config.getConsistencyType() != null ? config.getConsistencyType()
                    : ConsistencyType.WRITE_THROUGH;

            ConsistencyStrategy<K, V> strategy = strategyFactory.createStrategy(
                    consistencyType, caches);


            // 创建多级缓存
            return new MultiLevelCache<>(name, caches, strategy, config, lock, notifier);
        } else {
            throw new IllegalArgumentException("不支持的缓存类型: " + cacheType);
        }
    }

    /**
     * 验证缓存配置
     *
     * @param config 缓存配置
     */
    private void validateConfig(CacheConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("缓存配置不能为空");
        }

        if (config.getCacheType() == null) {
            throw new IllegalArgumentException("缓存类型不能为空");
        }

        if (config.getCacheType() == CacheType.REMOTE || config.getCacheType() == CacheType.BOTH) {
            if (remoteCacheManager == null) {
                throw new IllegalArgumentException("远程缓存类型需要配置远程缓存管理器");
            }
        }
    }

}