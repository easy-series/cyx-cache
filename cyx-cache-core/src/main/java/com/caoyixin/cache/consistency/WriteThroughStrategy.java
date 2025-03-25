package com.caoyixin.cache.consistency;

import com.caoyixin.cache.api.Cache;
import com.caoyixin.cache.api.ConsistencyStrategy;
import com.caoyixin.cache.notification.CacheUpdateEvent;
import com.caoyixin.cache.notification.CacheEvent;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 写同步策略实现，所有写操作同时写入所有级别的缓存
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
@Slf4j
public class WriteThroughStrategy<K, V> implements ConsistencyStrategy<K, V> {

    private final List<Cache<K, V>> caches;

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
    public void put(K key, V value, Duration ttl) {
        // 写入所有缓存
        for (Cache<K, V> cache : caches) {
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
        // 写入所有缓存
        for (Cache<K, V> cache : caches) {
            try {
                cache.putAll(map);
            } catch (Exception e) {
                log.error("批量写入缓存失败, cacheName={}", cache.getName(), e);
            }
        }
    }

    @Override
    public V get(K key) {
        V value = null;

        // 从L1开始查找，直到找到值
        for (int i = 0; i < caches.size(); i++) {
            Cache<K, V> cache = caches.get(i);
            try {
                value = cache.get(key);
                if (value != null) {
                    // 将值回填到前面级别的缓存
                    fillValueToLowerLevelCaches(key, value, i);
                    return value;
                }
            } catch (Exception e) {
                log.error("从缓存读取失败, cacheName={}, key={}", cache.getName(), key, e);
            }
        }

        return value;
    }

    @Override
    public V computeIfAbsent(K key, Function<K, V> loader, Duration ttl) {
        V value = null;

        // 首先检查缓存
        value = get(key);
        if (value != null) {
            return value;
        }

        // 缓存中不存在，使用loader加载
        try {
            value = loader.apply(key);
            if (value != null) {
                // 写入所有缓存
                put(key, value, ttl);
            }
            return value;
        } catch (Exception e) {
            log.error("加载缓存值失败, key={}", key, e);
            throw e;
        }
    }

    @Override
    public boolean remove(K key) {
        boolean removed = false;

        // 从所有缓存中删除
        for (Cache<K, V> cache : caches) {
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
        // 清空所有缓存
        for (Cache<K, V> cache : caches) {
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
    private void fillValueToLowerLevelCaches(K key, V value, int foundIndex) {
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