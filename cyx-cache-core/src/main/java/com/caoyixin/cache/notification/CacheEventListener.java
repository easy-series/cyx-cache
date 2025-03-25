package com.caoyixin.cache.notification;

/**
 * 缓存事件监听器
 */
public interface CacheEventListener {
    /**
     * 处理缓存事件
     *
     * @param event 缓存事件
     */
    void onEvent(CacheEvent event);
} 