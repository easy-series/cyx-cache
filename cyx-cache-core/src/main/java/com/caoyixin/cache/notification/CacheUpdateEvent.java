package com.caoyixin.cache.notification;

/**
 * 缓存更新事件
 */
public class CacheUpdateEvent extends CacheEvent {
    /**
     * 构造函数
     *
     * @param cacheName  缓存名称
     * @param key        缓存键
     * @param instanceId 实例ID
     */
    public CacheUpdateEvent(String cacheName, Object key, String instanceId) {
        super(cacheName, key, CacheEventType.UPDATE, instanceId);
    }
}