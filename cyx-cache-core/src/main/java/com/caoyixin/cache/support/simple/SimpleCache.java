package com.caoyixin.cache.support.simple;

import com.caoyixin.cache.api.Cache;
import com.caoyixin.cache.api.CacheStats;
import com.caoyixin.cache.exception.CacheException;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * 基于LinkedHashMap的简单缓存实现
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
@Slf4j
public class SimpleCache<K, V> implements Cache<K, V> {

    private final String name;
    private final int maxSize;
    private final CacheStats stats;
    private final Map<K, CacheEntry<V>> cacheMap;
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Map<K, Lock> lockMap = new ConcurrentHashMap<>();

    /**
     * 创建简单缓存
     *
     * @param name    缓存名称
     * @param maxSize 最大条目数
     */
    public SimpleCache(String name, int maxSize) {
        this.name = name;
        this.maxSize = maxSize > 0 ? maxSize : 100;
        this.stats = new CacheStats(name);

        this.cacheMap = Collections.synchronizedMap(new LinkedHashMap<K, CacheEntry<V>>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, CacheEntry<V>> eldest) {
                boolean shouldRemove = size() > SimpleCache.this.maxSize;
                if (shouldRemove) {
                    stats.recordEviction();
                }
                return shouldRemove;
            }
        });
    }

    @Override
    public V get(K key) {
        if (key == null) {
            return null;
        }

        rwLock.readLock().lock();
        try {
            CacheEntry<V> entry = cacheMap.get(key);
            if (entry == null) {
                stats.recordMiss();
                return null;
            }

            if (entry.isExpired()) {
                // 移除过期条目
                rwLock.readLock().unlock();
                rwLock.writeLock().lock();
                try {
                    if (cacheMap.remove(key) != null) {
                        stats.recordMiss();
                    }
                    return null;
                } finally {
                    rwLock.writeLock().unlock();
                    rwLock.readLock().lock();
                }
            }

            stats.recordHit();
            return entry.getValue();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public void put(K key, V value) {
        put(key, value, null);
    }

    @Override
    public void put(K key, V value, Duration ttl) {
        if (key == null) {
            return;
        }

        long expireTime = -1;
        if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
            expireTime = System.currentTimeMillis() + ttl.toMillis();
        }

        rwLock.writeLock().lock();
        try {
            cacheMap.put(key, new CacheEntry<>(value, expireTime));
            updateStats();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        if (map == null || map.isEmpty()) {
            return;
        }

        rwLock.writeLock().lock();
        try {
            for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
                cacheMap.put(entry.getKey(), new CacheEntry<>(entry.getValue(), -1));
            }
            updateStats();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public V computeIfAbsent(K key, Function<K, V> loader) {
        return computeIfAbsent(key, loader, null);
    }

    @Override
    public V computeIfAbsent(K key, Function<K, V> loader, Duration ttl) {
        if (key == null) {
            return null;
        }

        // 先查找缓存
        V value = get(key);
        if (value != null) {
            return value;
        }

        // 缓存中没有，需要加载
        rwLock.writeLock().lock();
        try {
            // 再次查找缓存（可能在获取锁的过程中被其他线程更新）
            CacheEntry<V> entry = cacheMap.get(key);
            if (entry != null && !entry.isExpired()) {
                stats.recordHit();
                return entry.getValue();
            }

            // 加载值
            stats.recordLoadStart();
            long startTime = System.currentTimeMillis();
            try {
                value = loader.apply(key);
                if (value != null) {
                    long expireTime = -1;
                    if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
                        expireTime = System.currentTimeMillis() + ttl.toMillis();
                    }

                    cacheMap.put(key, new CacheEntry<>(value, expireTime));
                    long loadTime = System.currentTimeMillis() - startTime;
                    stats.recordLoadSuccess(loadTime);
                    updateStats();
                } else {
                    stats.recordLoadFailure();
                }

                return value;
            } catch (Exception e) {
                stats.recordLoadFailure();
                log.error("加载缓存值异常, cacheName={}, key={}", name, key, e);
                throw new CacheException("加载缓存值异常: " + e.getMessage(), e);
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public boolean remove(K key) {
        if (key == null) {
            return false;
        }

        rwLock.writeLock().lock();
        try {
            boolean removed = cacheMap.remove(key) != null;
            if (removed) {
                updateStats();
            }
            return removed;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void clear() {
        rwLock.writeLock().lock();
        try {
            cacheMap.clear();
            updateStats();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public CacheStats stats() {
        updateStats();
        return stats;
    }

    /**
     * 更新缓存统计信息
     */
    private void updateStats() {
        stats.updateSize(cacheMap.size());
    }

    /**
     * 缓存条目类
     *
     * @param <V> 值类型
     */
    private static class CacheEntry<V> {
        private final V value;
        private final long expireTime;

        public CacheEntry(V value, long expireTime) {
            this.value = value;
            this.expireTime = expireTime;
        }

        public V getValue() {
            return value;
        }

        public boolean isExpired() {
            return expireTime > 0 && System.currentTimeMillis() >= expireTime;
        }
    }
}