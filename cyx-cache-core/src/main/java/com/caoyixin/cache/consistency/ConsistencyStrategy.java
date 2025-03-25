package com.caoyixin.cache.consistency;

import com.caoyixin.cache.api.Cache;
import com.caoyixin.cache.notification.CacheNotifier;

import java.time.Duration;

/**
 * 缓存一致性策略接口
 */
public interface ConsistencyStrategy {
    /**
     * 处理缓存写入操作
     *
     * @param cacheName   缓存名称
     * @param localCache  本地缓存
     * @param remoteCache 远程缓存
     * @param key         缓存键
     * @param value       缓存值
     * @param ttl         过期时间
     * @param notifier    缓存通知器
     * @param <K>         键类型
     * @param <V>         值类型
     */
    <K, V> void onPut(String cacheName, Cache<K, V> localCache, Cache<K, V> remoteCache,
            K key, V value, Duration ttl, CacheNotifier notifier);

    /**
     * 处理缓存移除操作
     *
     * @param cacheName   缓存名称
     * @param localCache  本地缓存
     * @param remoteCache 远程缓存
     * @param key         缓存键
     * @param notifier    缓存通知器
     * @param <K>         键类型
     * @param <V>         值类型
     */
    <K, V> void onRemove(String cacheName, Cache<K, V> localCache, Cache<K, V> remoteCache,
            K key, CacheNotifier notifier);

    /**
     * 处理缓存获取操作
     *
     * @param cacheName   缓存名称
     * @param localCache  本地缓存
     * @param remoteCache 远程缓存
     * @param key         缓存键
     * @param <K>         键类型
     * @param <V>         值类型
     * @return 缓存值
     */
    <K, V> V onGet(String cacheName, Cache<K, V> localCache, Cache<K, V> remoteCache, K key);
}