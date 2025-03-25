package com.caoyixin.cache.notification;

/**
 * 缓存移除事件
 */
public class CacheRemoveEvent extends CacheEvent {
    /**
     * 构造函数
     *
     * @param cacheName  缓存名称
     * @param key        缓存键
     * @param instanceId 实例ID
     */
    public CacheRemoveEvent(String cacheName, Object key, String instanceId) {
        super(cacheName, key, CacheEventType.REMOVE, instanceId);
    }
} 