package com.caoyixin.cache.api;

import com.caoyixin.cache.notification.CacheUpdateEvent;
import com.caoyixin.cache.notification.CacheEvent;

import java.time.Duration;
import java.util.Map;
import java.util.function.Function;

/**
 * 缓存一致性策略接口
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
public interface ConsistencyStrategy<K, V> {

    /**
     * 获取缓存值
     *
     * @param key 缓存键
     * @return 缓存值
     */
    V get(K key);

    /**
     * 存储缓存值
     *
     * @param key   缓存键
     * @param value 缓存值
     * @param ttl   过期时间
     */
    void put(K key, V value, Duration ttl);

    /**
     * 批量存储缓存值
     *
     * @param map 缓存键值对
     */
    void putAll(Map<? extends K, ? extends V> map);

    /**
     * 如果缓存中不存在，则计算并存储值
     *
     * @param key    缓存键
     * @param loader 值加载器
     * @param ttl    过期时间
     * @return 缓存值
     */
    V computeIfAbsent(K key, Function<K, V> loader, Duration ttl);

    /**
     * 从缓存中移除值
     *
     * @param key 缓存键
     * @return 是否成功移除
     */
    boolean remove(K key);

    /**
     * 清空缓存
     */
    void clear();

    /**
     * 处理缓存更新事件
     *
     * @param event 缓存事件
     */
    void handleCacheUpdate(CacheEvent event);
}