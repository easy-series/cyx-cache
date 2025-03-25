package com.caoyixin.cache.notification;

import lombok.Getter;
import lombok.ToString;

/**
 * 缓存事件
 */
@Getter
@ToString
public abstract class CacheEvent {
    /**
     * 缓存名称
     */
    private final String cacheName;

    /**
     * 缓存键
     */
    private final Object key;

    /**
     * 事件类型
     */
    private final CacheEventType eventType;

    /**
     * 实例ID，用于识别事件来源
     */
    private final String instanceId;

    /**
     * 构造函数
     *
     * @param cacheName  缓存名称
     * @param key        缓存键
     * @param eventType  事件类型
     * @param instanceId 实例ID
     */
    protected CacheEvent(String cacheName, Object key, CacheEventType eventType, String instanceId) {
        this.cacheName = cacheName;
        this.key = key;
        this.eventType = eventType;
        this.instanceId = instanceId;
    }
}