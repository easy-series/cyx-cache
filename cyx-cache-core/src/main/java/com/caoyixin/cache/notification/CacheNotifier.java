package com.caoyixin.cache.notification;

/**
 * 缓存通知接口，用于在分布式环境中同步缓存变更
 */
public interface CacheNotifier {
    /**
     * 通知缓存更新
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     */
    void notifyUpdate(String cacheName, Object key);

    /**
     * 通知缓存移除
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     */
    void notifyRemove(String cacheName, Object key);

    /**
     * 订阅缓存事件
     *
     * @param cacheName 缓存名称
     * @param listener  事件监听器
     */
    void subscribe(String cacheName, CacheEventListener listener);
} 