package com.caoyixin.cache.notification;

/**
 * 缓存事件类型
 */
public enum CacheEventType {
    /**
     * 更新事件
     */
    UPDATE,

    /**
     * 移除事件
     */
    REMOVE,

    /**
     * 刷新事件
     */
    REFRESH,

    /**
     * PUT 事件
     */
    PUT,

    CLEAR,
}