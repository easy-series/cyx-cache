package com.caoyixin.cache.consistency;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.caoyixin.cache.api.Cache;
import com.caoyixin.cache.notification.CacheEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * 写同步策略实现，所有写操作同时写入所有级别的缓存
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
@Slf4j
public class WriteThroughStrategy<K, V> extends AbstractConsistencyStrategy<K, V> {

    /**
     * 创建写同步策略
     *
     * @param caches 所有缓存实例列表，顺序从L1到Ln
     */
    public WriteThroughStrategy(List<Cache<K, V>> caches) {
        if (caches == null || caches.isEmpty() || caches.size() < 2) {
            throw new IllegalArgumentException("缓存列表必须包含至少两个缓存实例");
        }
        this.caches = caches;
    }

    @Override
    public String getName() {
        return "WriteThrough";
    }

    @Override
    public V get(K key) {
        if (key == null || caches.isEmpty()) {
            return null;
        }

        V value = null;

        // 从L1开始查找，直到找到值
        for (int i = 0; i < caches.size(); i++) {
            Cache<K, V> cache = caches.get(i);
            try {
                value = cache.get(key);
                if (value != null) {
                    // 将值回填到前面级别的缓存
                    backfillToLowerLevelCaches(key, value, i);
                    return value;
                }
            } catch (Exception e) {
                log.error("从缓存读取失败, cacheName={}, key={}", cache.getName(), key, e);
            }
        }

        return value;
    }

    @Override
    public void put(K key, V value, Duration ttl) {
        if (key == null || value == null || caches.isEmpty()) {
            return;
        }

        // 写入所有缓存，从后往前写入
        // 这样保证即使过程中出现故障，本地缓存也不会保存远程缓存没有的数据
        for (int i = caches.size() - 1; i >= 0; i--) {
            Cache<K, V> cache = caches.get(i);
            try {
                if (ttl != null) {
                    cache.put(key, value, ttl);
                } else {
                    cache.put(key, value);
                }
            } catch (Exception e) {
                log.error("写入缓存失败, cacheName={}, key={}", cache.getName(), key, e);
            }
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        if (map == null || map.isEmpty() || caches.isEmpty()) {
            return;
        }

        // 写入所有缓存，从后往前写入
        for (int i = caches.size() - 1; i >= 0; i--) {
            Cache<K, V> cache = caches.get(i);
            try {
                cache.putAll(map);
            } catch (Exception e) {
                log.error("批量写入缓存失败, cacheName={}", cache.getName(), e);
            }
        }
    }

    @Override
    public boolean remove(K key) {
        if (key == null || caches.isEmpty()) {
            return false;
        }

        boolean removed = false;

        // 从所有缓存中删除，从后往前删除
        for (int i = caches.size() - 1; i >= 0; i--) {
            Cache<K, V> cache = caches.get(i);
            try {
                boolean result = cache.remove(key);
                if (result) {
                    removed = true;
                }
            } catch (Exception e) {
                log.error("从缓存删除失败, cacheName={}, key={}", cache.getName(), key, e);
            }
        }

        return removed;
    }

    @Override
    public void clear() {
        if (caches.isEmpty()) {
            return;
        }

        // 清空所有缓存，从后往前清空
        for (int i = caches.size() - 1; i >= 0; i--) {
            Cache<K, V> cache = caches.get(i);
            try {
                cache.clear();
            } catch (Exception e) {
                log.error("清空缓存失败, cacheName={}", cache.getName(), e);
            }
        }
    }

    @Override
    public void handleCacheUpdate(CacheEvent event) {
        if (event == null) {
            return;
        }

        // 处理远程缓存更新事件
        @SuppressWarnings("unchecked")
        K key = (K) event.getKey();

        switch (event.getEventType()) {
            case PUT:
            case UPDATE:
                // 本地缓存也需要删除，因为无法获取远程缓存的值
                remove(key);
                break;
            case REMOVE:
                remove(key);
                break;
            case CLEAR:
                clear();
                break;
            default:
                log.warn("未知的缓存更新事件类型: {}", event.getEventType());
        }
    }

    /**
     * 将值回填到低级别的缓存中
     *
     * @param key        缓存键
     * @param value      缓存值
     * @param foundIndex 找到值的缓存索引
     */
    private void backfillToLowerLevelCaches(K key, V value, int foundIndex) {
        // 将值回填到前面级别的缓存中
        for (int j = 0; j < foundIndex; j++) {
            try {
                caches.get(j).put(key, value);
            } catch (Exception e) {
                log.error("回填缓存值失败, cacheName={}, key={}", caches.get(j).getName(), key, e);
            }
        }
    }
}