package com.easy.cache.local;

import com.easy.cache.core.Cache;
import com.easy.cache.core.CacheConfig;
import com.easy.cache.core.CacheLoader;
import com.easy.cache.core.CacheStats;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.stats.StatsCounter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.index.qual.NonNegative;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 基于Caffeine的本地缓存实现
 */
@Slf4j
public class CaffeineLocalCache<K, V> implements Cache<K, V> {

    private final String name;
    private final CacheConfig config;
    private final com.github.benmanes.caffeine.cache.Cache<K, V> caffeineCache;
    private final CaffeineStatsCounter statsCounter;
    private final LocalCacheStats cacheStats;

    /**
     * 创建CaffeineLocalCache实例
     *
     * @param name   缓存名称
     * @param config 缓存配置
     */
    public CaffeineLocalCache(String name, CacheConfig config) {
        this.name = name;
        this.config = config;
        this.statsCounter = new CaffeineStatsCounter();
        this.cacheStats = new LocalCacheStats(name);

        Caffeine<Object, Object> builder = Caffeine.newBuilder();

        // 应用配置
        if (config.getLocalExpireSeconds() > 0) {
            builder.expireAfterWrite(config.getLocalExpireSeconds(), TimeUnit.SECONDS);
        }

        if (config.getLocalMaxSize() > 0) {
            builder.maximumSize(config.getLocalMaxSize());
        }

        if (config.isLocalSoftValues()) {
            builder.softValues();
        }

        if (config.isLocalWeakKeys()) {
            builder.weakKeys();
        }

        if (config.isLocalRecordStats()) {
            builder.recordStats(() -> statsCounter);
        }

        // 添加移除监听器
        builder.removalListener((K key, V value, RemovalCause cause) -> {
            log.debug("缓存[{}]中的键[{}]被移除，原因: {}", name, key, cause);
        });

        this.caffeineCache = builder.build();
    }

    /**
     * 使用CacheLoader创建CaffeineLocalCache实例
     *
     * @param name        缓存名称
     * @param config      缓存配置
     * @param cacheLoader 缓存加载器
     */
    public CaffeineLocalCache(String name, CacheConfig config, CacheLoader<K, V> cacheLoader) {
        this.name = name;
        this.config = config;
        this.statsCounter = new CaffeineStatsCounter();
        this.cacheStats = new LocalCacheStats(name);

        Caffeine<Object, Object> builder = Caffeine.newBuilder();

        // 应用配置
        if (config.getLocalExpireSeconds() > 0) {
            builder.expireAfterWrite(config.getLocalExpireSeconds(), TimeUnit.SECONDS);
        }

        if (config.getLocalMaxSize() > 0) {
            builder.maximumSize(config.getLocalMaxSize());
        }

        if (config.isLocalSoftValues()) {
            builder.softValues();
        }

        if (config.isLocalWeakKeys()) {
            builder.weakKeys();
        }

        if (config.isLocalRecordStats()) {
            builder.recordStats(() -> statsCounter);
        }

        // 添加移除监听器
        builder.removalListener((K key, V value, RemovalCause cause) -> {
            log.debug("缓存[{}]中的键[{}]被移除，原因: {}", name, key, cause);
        });

        // 使用cacheLoader
        LoadingCache<K, V> loadingCache = builder.build(key -> {
            try {
                cacheStats.recordLoadStart();
                V value = cacheLoader.load(key);
                if (cacheLoader.isLoadSuccess(key, value)) {
                    value = cacheLoader.beforeCache(key, value);
                    cacheStats.recordLoadSuccess();
                    return value;
                } else {
                    cacheStats.recordLoadFailure();
                    return null;
                }
            } catch (Exception e) {
                log.error("加载缓存键[{}]失败: {}", key, e.getMessage(), e);
                cacheStats.recordLoadFailure();
                return cacheLoader.onLoadFailure(key, e);
            } finally {
                cacheStats.recordLoadEnd();
            }
        });

        this.caffeineCache = loadingCache;
    }

    @Override
    public V get(K key) {
        if (key == null) {
            return null;
        }

        cacheStats.recordRequest();
        V value = caffeineCache.getIfPresent(key);

        if (value != null) {
            cacheStats.recordHit();
            cacheStats.recordAccess();
        } else {
            cacheStats.recordMiss();
        }

        return value;
    }

    @Override
    public Optional<V> getOptional(K key) {
        return Optional.ofNullable(get(key));
    }

    @Override
    public V get(K key, Callable<V> valueLoader) {
        if (key == null) {
            return null;
        }

        cacheStats.recordRequest();
        try {
            return caffeineCache.get(key, k -> {
                try {
                    return valueLoader.call();
                } catch (Exception e) {
                    if (e instanceof RuntimeException) {
                        throw (RuntimeException) e;
                    }
                    throw new RuntimeException("加载缓存值失败", e);
                }
            });
        } catch (Exception e) {
            log.error("加载缓存键[{}]失败: {}", key, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public Map<K, V> getAll(Collection<K> keys) {
        if (keys == null || keys.isEmpty()) {
            return new HashMap<>();
        }

        cacheStats.recordRequest();
        // 过滤掉null键
        Set<K> validKeys = keys.stream().filter(k -> k != null).collect(Collectors.toSet());

        Map<K, V> result = caffeineCache.getAllPresent(validKeys);
        if (result.size() == validKeys.size()) {
            cacheStats.recordHits(validKeys.size());
        } else {
            cacheStats.recordHits(result.size());
            cacheStats.recordMisses(validKeys.size() - result.size());
        }

        return result;
    }

    @Override
    public void put(K key, V value) {
        if (key == null) {
            return;
        }

        cacheStats.recordWrite();
        caffeineCache.put(key, value);
    }

    @Override
    public void put(K key, V value, long expireSeconds) {
        if (key == null) {
            return;
        }

        cacheStats.recordWrite();
        caffeineCache.put(key, value);
        // 注意：Caffeine不支持对单个键设置过期时间，所以这里只能简单地put
        // 如果需要对单个键设置过期时间，需要使用扩展机制
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        if (map == null || map.isEmpty()) {
            return;
        }

        cacheStats.recordWrites(map.size());
        caffeineCache.putAll(map);
    }

    @Override
    public void putAll(Map<K, V> map, long expireSeconds) {
        putAll(map); // 同样，Caffeine不支持批量设置过期时间
    }

    @Override
    public boolean putIfAbsent(K key, V value) {
        if (key == null) {
            return false;
        }

        V oldValue = caffeineCache.getIfPresent(key);
        if (oldValue == null) {
            cacheStats.recordWrite();
            caffeineCache.put(key, value);
            return true;
        }
        return false;
    }

    @Override
    public void remove(K key) {
        if (key == null) {
            return;
        }

        V oldValue = caffeineCache.getIfPresent(key);
        if (oldValue != null) {
            cacheStats.recordDelete();
            caffeineCache.invalidate(key);
        }
    }

    @Override
    public void removeAll(Collection<? extends K> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }

        // 过滤掉null键
        Set<K> validKeys = keys.stream()
                .filter(k -> k != null)
                .map(k -> (K) k)
                .collect(Collectors.toSet());

        if (!validKeys.isEmpty()) {
            cacheStats.recordDeletes(validKeys.size());
            caffeineCache.invalidateAll(validKeys);
        }
    }

    @Override
    public void clear() {
        cacheStats.recordClear();
        caffeineCache.invalidateAll();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public CacheConfig getConfig() {
        return config;
    }

    @Override
    public boolean containsKey(K key) {
        if (key == null) {
            return false;
        }
        return caffeineCache.getIfPresent(key) != null;
    }

    @Override
    public CacheStats stats() {
        // 更新统计信息
        com.github.benmanes.caffeine.cache.stats.CacheStats caffeineStats = caffeineCache.stats();
        cacheStats.updateFrom(caffeineStats, statsCounter);
        cacheStats.setSize(caffeineCache.estimatedSize());

        return cacheStats;
    }

    @Override
    public void expire(K key, long expireSeconds) {
        // Caffeine不直接支持对单个键设置过期时间
        // 此处可以通过重新put来实现近似效果，但不是真正的expire
        V value = get(key);
        if (value != null) {
            put(key, value);
        }
    }

    @Override
    public long ttl(K key) {
        // Caffeine不支持查询剩余过期时间
        if (containsKey(key)) {
            return -1; // 存在但无法确定剩余时间
        }
        return -2; // 不存在
    }

    @Override
    public long increment(K key, long delta) {
        throw new UnsupportedOperationException("本地缓存不支持increment操作");
    }

    /**
     * Caffeine统计计数器
     */
    private static class CaffeineStatsCounter implements StatsCounter {
        private final ConcurrentHashMap<String, Long> counters = new ConcurrentHashMap<>();

        private void increment(String key, long delta) {
            counters.compute(key, (k, v) -> (v == null) ? delta : v + delta);
        }

        private long get(String key) {
            return counters.getOrDefault(key, 0L);
        }

        @Override
        public void recordHits(@NonNegative int count) {
            increment("hits", count);
        }

        @Override
        public void recordMisses(@NonNegative int count) {
            increment("misses", count);
        }

        @Override
        public void recordLoadSuccess(@NonNegative long loadTime) {
            increment("loadSuccess", 1);
            increment("loadTime", loadTime);
        }

        @Override
        public void recordLoadFailure(@NonNegative long loadTime) {
            increment("loadFailure", 1);
            increment("loadTime", loadTime);
        }

        @Override
        public void recordEviction(@NonNegative int i, RemovalCause removalCause) {
            increment("eviction", 1);

        }

        @Override
        public com.github.benmanes.caffeine.cache.stats.CacheStats snapshot() {
            return null;
        }
    }
}