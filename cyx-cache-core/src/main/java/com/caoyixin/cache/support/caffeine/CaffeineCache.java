package com.caoyixin.cache.support.caffeine;

import com.caoyixin.cache.api.Cache;
import com.caoyixin.cache.api.CacheStats;
import com.caoyixin.cache.exception.CacheException;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.Weigher;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * 基于Caffeine的本地缓存实现
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
@Slf4j
public class CaffeineCache<K, V> implements Cache<K, V> {

    private final String name;
    private final com.github.benmanes.caffeine.cache.Cache<K, V> cache;
    private final CacheStats stats;
    private final Map<K, Lock> lockMap = new ConcurrentHashMap<>();

    /**
     * 创建Caffeine缓存
     *
     * @param name             缓存名称
     * @param maxSize          最大条目数量
     * @param expireAfterWrite 写入后过期时间
     */
    public CaffeineCache(String name, int maxSize, Duration expireAfterWrite) {
        this.name = name;
        this.stats = new CacheStats(name);

        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .removalListener((K key, V value, RemovalCause cause) -> {
                    if (cause.wasEvicted()) {
                        stats.recordEviction();
                    }
                });

        if (expireAfterWrite != null) {
            builder.expireAfterWrite(expireAfterWrite);
        }

        this.cache = builder.build();
    }

    /**
     * 使用自定义的Caffeine构建器创建缓存
     *
     * @param name    缓存名称
     * @param builder Caffeine构建器
     */
    public CaffeineCache(String name, Caffeine<Object, Object> builder) {
        this.name = name;
        this.stats = new CacheStats(name);

        // 添加移除监听器
        builder.removalListener((K key, V value, RemovalCause cause) -> {
            if (cause.wasEvicted()) {
                stats.recordEviction();
            }
        });

        this.cache = builder.build();
    }

    @Override
    public V get(K key) {
        if (key == null) {
            return null;
        }

        V value = cache.getIfPresent(key);
        if (value != null) {
            stats.recordHit();
        } else {
            stats.recordMiss();
        }

        return value;
    }

    @Override
    public void put(K key, V value) {
        if (key == null) {
            return;
        }

        cache.put(key, value);
        updateStats();
    }

    @Override
    public void put(K key, V value, Duration ttl) {
        if (key == null) {
            return;
        }

        // Caffeine不支持单独为每个键设置过期时间
        // 这里简单地忽略ttl参数，使用全局配置
        cache.put(key, value);
        updateStats();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        if (map == null || map.isEmpty()) {
            return;
        }

        cache.putAll(map);
        updateStats();
    }

    @Override
    public V computeIfAbsent(K key, Function<K, V> loader) {
        if (key == null) {
            return null;
        }

        try {
            V value = cache.getIfPresent(key);
            if (value != null) {
                stats.recordHit();
                return value;
            }

            stats.recordMiss();
            stats.recordLoadStart();
            long startTime = System.currentTimeMillis();

            value = cache.get(key, loader);

            long loadTime = System.currentTimeMillis() - startTime;
            stats.recordLoadSuccess(loadTime);
            updateStats();

            return value;
        } catch (Exception e) {
            stats.recordLoadFailure();
            log.error("加载缓存值异常, cacheName={}, key={}", name, key, e);
            throw new CacheException("加载缓存值异常: " + e.getMessage(), e);
        }
    }

    @Override
    public V computeIfAbsent(K key, Function<K, V> loader, Duration ttl) {
        // Caffeine不支持单独为每个键设置过期时间
        // 这里简单地忽略ttl参数，使用全局配置
        return computeIfAbsent(key, loader);
    }

    @Override
    public boolean remove(K key) {
        if (key == null) {
            return false;
        }

        cache.invalidate(key);
        updateStats();
        return true;
    }

    @Override
    public void clear() {
        cache.invalidateAll();
        updateStats();
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
        stats.updateSize(cache.estimatedSize());
    }

    @Override
    public boolean tryLock(K key, Duration timeout) {
        if (key == null) {
            return false;
        }

        Lock lock = lockMap.computeIfAbsent(key, k -> new ReentrantLock());
        try {
            if (timeout == null || timeout.isZero() || timeout.isNegative()) {
                return lock.tryLock();
            } else {
                return lock.tryLock(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void unlock(K key) {
        if (key == null) {
            return;
        }

        Lock lock = lockMap.get(key);
        if (lock != null) {
            try {
                lock.unlock();
            } catch (IllegalMonitorStateException e) {
                // 忽略未持有锁的异常
            }
        }
    }

    @Override
    public boolean tryLockAndRun(K key, Duration timeout, Runnable action) {
        if (tryLock(key, timeout)) {
            try {
                action.run();
                return true;
            } finally {
                unlock(key);
            }
        }
        return false;
    }

    /**
     * 获取原始的Caffeine缓存对象
     *
     * @return Caffeine缓存对象
     */
    public com.github.benmanes.caffeine.cache.Cache<K, V> getNativeCache() {
        return cache;
    }
}