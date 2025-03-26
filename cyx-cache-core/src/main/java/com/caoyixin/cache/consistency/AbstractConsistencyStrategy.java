package com.caoyixin.cache.consistency;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.caoyixin.cache.api.Cache;
import com.caoyixin.cache.notification.CacheEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * 抽象一致性策略实现，提供通用逻辑
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
@Slf4j
public abstract class AbstractConsistencyStrategy<K, V> implements ConsistencyStrategy<K, V> {

    /**
     * 缓存层次列表，索引0是最靠近应用的缓存，通常是本地缓存
     */
    protected List<Cache<K, V>> caches = new ArrayList<>();

    @Override
    public void initialize(List<Cache<K, V>> caches) {
        if (caches == null || caches.isEmpty()) {
            throw new IllegalArgumentException("缓存列表不能为空");
        }
        this.caches = new ArrayList<>(caches);
    }

    @Override
    public V computeIfAbsent(K key, Function<K, V> loader, Duration ttl) {
        // 首先尝试从缓存获取
        V value = get(key);
        if (value != null) {
            return value;
        }

        // 加载值
        try {
            value = loader.apply(key);
            if (value != null) {
                put(key, value, ttl);
            }
            return value;
        } catch (Exception e) {
            log.error("加载缓存值异常, key={}", key, e);
            throw e;
        }
    }

    @Override
    public void handleCacheUpdate(CacheEvent event) {
        if (event == null) {
            return;
        }

        @SuppressWarnings("unchecked")
        K key = (K) event.getKey();

        switch (event.getEventType()) {
            case PUT:
            case UPDATE:
                // 默认处理：清除本地缓存，强制下次从远程获取最新值
                invalidateLocalCaches(key);
                break;
            case REMOVE:
                remove(key);
                break;
            case CLEAR:
                clear();
                break;
            default:
                log.warn("未知的缓存事件类型: {}", event.getEventType());
        }
    }

    /**
     * 使本地缓存失效
     * 默认实现是使第一级缓存的对应键失效
     *
     * @param key 缓存键
     */
    protected void invalidateLocalCaches(K key) {
        // 默认只处理第一级缓存(通常是本地缓存)
        if (!caches.isEmpty()) {
            caches.get(0).remove(key);
        }
    }

    /**
     * 将值回填到低级别的缓存
     *
     * @param key        缓存键
     * @param value      缓存值
     * @param foundIndex 找到值的缓存索引
     */
    protected void backfillToLowerLevelCaches(K key, V value, int foundIndex) {
        // 将值回填到前面级别的缓存中
        for (int i = 0; i < foundIndex; i++) {
            try {
                caches.get(i).put(key, value);
            } catch (Exception e) {
                log.error("回填缓存值失败, cacheName={}, key={}", caches.get(i).getName(), key, e);
            }
        }
    }
}