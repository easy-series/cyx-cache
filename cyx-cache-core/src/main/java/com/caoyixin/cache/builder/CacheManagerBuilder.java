package com.caoyixin.cache.builder;

import com.caoyixin.cache.api.CacheManager;
import com.caoyixin.cache.consistency.ConsistencyStrategyFactory;
import com.caoyixin.cache.consistency.DefaultConsistencyStrategyFactory;
import com.caoyixin.cache.multilevel.MultiLevelCacheManager;
import com.caoyixin.cache.notification.CacheNotifier;

/**
 * 缓存管理器构建器
 */
public class CacheManagerBuilder {

    private CacheManager localCacheManager;
    private CacheManager remoteCacheManager;
    private CacheNotifier notifier;
    private ConsistencyStrategyFactory strategyFactory;

    /**
     * 设置本地缓存管理器
     *
     * @param localCacheManager 本地缓存管理器
     * @return 当前构建器
     */
    public CacheManagerBuilder localCacheManager(CacheManager localCacheManager) {
        this.localCacheManager = localCacheManager;
        return this;
    }

    /**
     * 设置远程缓存管理器
     *
     * @param remoteCacheManager 远程缓存管理器
     * @return 当前构建器
     */
    public CacheManagerBuilder remoteCacheManager(CacheManager remoteCacheManager) {
        this.remoteCacheManager = remoteCacheManager;
        return this;
    }

    /**
     * 设置缓存通知器
     *
     * @param notifier 缓存通知器
     * @return 当前构建器
     */
    public CacheManagerBuilder notifier(CacheNotifier notifier) {
        this.notifier = notifier;
        return this;
    }

    /**
     * 设置一致性策略工厂
     *
     * @param strategyFactory 一致性策略工厂
     * @return 当前构建器
     */
    public CacheManagerBuilder strategyFactory(ConsistencyStrategyFactory strategyFactory) {
        this.strategyFactory = strategyFactory;
        return this;
    }

    /**
     * 构建缓存管理器
     *
     * @return 缓存管理器
     */
    public CacheManager build() {
        if (strategyFactory == null) {
            strategyFactory = new DefaultConsistencyStrategyFactory();
        }
        // 根据提供的组件选择合适的缓存管理器实现
        if (remoteCacheManager == null) {
            return localCacheManager; // 仅本地缓存
        } else if (localCacheManager == null) {
            return remoteCacheManager; // 仅远程缓存
        } else {
            return new MultiLevelCacheManager(
                    localCacheManager,
                    remoteCacheManager,
                    notifier,
                    strategyFactory);
        }
    }
}