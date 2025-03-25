package com.easy.cache.remote.redis;

import com.easy.cache.core.Cache;
import com.easy.cache.core.CacheConfig;
import com.easy.cache.core.CacheLoader;
import com.easy.cache.core.CacheStats;
import com.easy.cache.core.serializer.KeySerializer;
import com.easy.cache.core.serializer.ValueSerializer;
import com.easy.cache.remote.serializer.SerializerFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Redis缓存抽象基类
 * <p>
 * 提供Redis缓存的基本实现和通用方法
 *
 * @param <K> 缓存键类型
 * @param <V> 缓存值类型
 */
@Slf4j
public abstract class AbstractRedisCache<K, V> implements Cache<K, V> {

    /**
     * 缓存名称
     */
    protected final String name;

    /**
     * 缓存配置
     */
    protected final CacheConfig config;

    /**
     * 缓存加载器
     */
    protected final CacheLoader<K, V> cacheLoader;

    /**
     * 键序列化器
     */
    protected final KeySerializer<K> keySerializer;

    /**
     * 值序列化器
     */
    protected final ValueSerializer<V> valueSerializer;

    /**
     * 缓存统计信息
     */
    protected final RedisStats stats;

    /**
     * 键前缀，用于隔离不同的缓存实例
     */
    protected final String keyPrefix;

    /**
     * 默认过期时间（秒）
     */
    protected final long defaultExpireSeconds;

    /**
     * 创建Redis缓存实例
     *
     * @param name   缓存名称
     * @param config 缓存配置
     */
    protected AbstractRedisCache(String name, CacheConfig config) {
        this(name, config, null);
    }

    /**
     * 创建Redis缓存实例
     *
     * @param name        缓存名称
     * @param config      缓存配置
     * @param cacheLoader 缓存加载器
     */
    @SuppressWarnings("unchecked")
    protected AbstractRedisCache(String name, CacheConfig config, CacheLoader<K, V> cacheLoader) {
        this.name = name;
        this.config = config != null ? config : CacheConfig.remoteConfig();
        this.cacheLoader = cacheLoader;
        this.stats = new RedisStats(name);

        // 设置键前缀
        String configKeyPrefix = this.config.getKeyPrefix();
        this.keyPrefix = configKeyPrefix != null && !configKeyPrefix.isEmpty()
                ? configKeyPrefix + ":" + name + ":"
                : name + ":";

        // 设置过期时间
        this.defaultExpireSeconds = this.config.getRemoteExpireSeconds() > 0
                ? this.config.getRemoteExpireSeconds()
                : 0;

        // 初始化序列化器
        this.keySerializer = (KeySerializer<K>) SerializerFactory.createStringKeySerializer();

        // 根据配置选择值序列化器
        String valueSerializerType = this.config.getRemoteValueSerializer();
        SerializerFactory.SerializerType serializerType;

        if ("kryo".equalsIgnoreCase(valueSerializerType)) {
            serializerType = SerializerFactory.SerializerType.KRYO;
        } else if ("jackson".equalsIgnoreCase(valueSerializerType)) {
            serializerType = SerializerFactory.SerializerType.JACKSON;
        } else {
            serializerType = SerializerFactory.SerializerType.JAVA;
        }

        // 创建值序列化器，由于泛型擦除，这里我们无法直接获取V的Class对象
        // 在子类构造函数中需要设置正确的值序列化器
        this.valueSerializer = null;
    }

    /**
     * 获取带前缀的完整键
     *
     * @param key 原始键
     * @return 带前缀的完整键
     */
    protected String getFullKey(K key) {
        return keyPrefix + keySerializer.toString(key);
    }

    /**
     * 使用加载器加载值
     *
     * @param key 缓存键
     * @return 加载的值
     */
    protected V loadValue(K key) {
        if (cacheLoader == null) {
            return null;
        }

        try {
            stats.recordLoadStart();
            V value = cacheLoader.load(key);
            stats.recordLoadEnd();

            if (cacheLoader.isLoadSuccess(key, value)) {
                stats.recordLoadSuccess();
                return cacheLoader.beforeCache(key, value);
            } else {
                stats.recordLoadFailure();
                return null;
            }
        } catch (Exception e) {
            stats.recordLoadFailure();
            log.error("加载缓存值失败: key={}, error={}", key, e.getMessage(), e);
            return cacheLoader.onLoadFailure(key, e);
        }
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
    public CacheStats stats() {
        return stats;
    }

    /**
     * Redis缓存统计信息类
     */
    protected static class RedisStats implements CacheStats {
        private final String name;
        private final java.util.concurrent.atomic.LongAdder requestCount = new java.util.concurrent.atomic.LongAdder();
        private final java.util.concurrent.atomic.LongAdder hitCount = new java.util.concurrent.atomic.LongAdder();
        private final java.util.concurrent.atomic.LongAdder missCount = new java.util.concurrent.atomic.LongAdder();
        private final java.util.concurrent.atomic.LongAdder loadCount = new java.util.concurrent.atomic.LongAdder();
        private final java.util.concurrent.atomic.LongAdder loadSuccessCount = new java.util.concurrent.atomic.LongAdder();
        private final java.util.concurrent.atomic.LongAdder loadFailureCount = new java.util.concurrent.atomic.LongAdder();
        private final java.util.concurrent.atomic.LongAdder totalLoadTime = new java.util.concurrent.atomic.LongAdder();
        private final java.util.concurrent.atomic.LongAdder writeCount = new java.util.concurrent.atomic.LongAdder();
        private final java.util.concurrent.atomic.LongAdder deleteCount = new java.util.concurrent.atomic.LongAdder();
        private final java.util.concurrent.atomic.AtomicLong lastResetTime = new java.util.concurrent.atomic.AtomicLong(
                System.currentTimeMillis());
        private volatile long loadStartTime;
        private volatile long size;

        /**
         * 创建RedisStats实例
         *
         * @param name 缓存名称
         */
        public RedisStats(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public long getRequestCount() {
            return requestCount.sum();
        }

        @Override
        public long getHitCount() {
            return hitCount.sum();
        }

        @Override
        public long getMissCount() {
            return missCount.sum();
        }

        @Override
        public long getSize() {
            return size;
        }

        @Override
        public long getWriteCount() {
            return writeCount.sum();
        }

        @Override
        public long getDeleteCount() {
            return deleteCount.sum();
        }

        @Override
        public long getLoadCount() {
            return loadCount.sum();
        }

        @Override
        public long getLoadSuccessCount() {
            return loadSuccessCount.sum();
        }

        @Override
        public long getLoadFailureCount() {
            return loadFailureCount.sum();
        }

        @Override
        public double getAverageLoadPenalty() {
            long totalLoadCount = getLoadSuccessCount() + getLoadFailureCount();
            return totalLoadCount == 0 ? 0.0 : (double) getTotalLoadTime() / totalLoadCount;
        }

        @Override
        public long getTotalLoadTime() {
            return totalLoadTime.sum();
        }

        @Override
        public long getEvictionCount() {
            return 0; // Redis不支持驱逐统计
        }

        @Override
        public void resetStats() {
            requestCount.reset();
            hitCount.reset();
            missCount.reset();
            loadCount.reset();
            loadSuccessCount.reset();
            loadFailureCount.reset();
            totalLoadTime.reset();
            writeCount.reset();
            deleteCount.reset();
            lastResetTime.set(System.currentTimeMillis());
        }

        @Override
        public long getLastResetTime() {
            return lastResetTime.get();
        }

        @Override
        public com.easy.cache.core.CacheType getCacheType() {
            return com.easy.cache.core.CacheType.REMOTE;
        }

        @Override
        public void setSize(long size) {
            this.size = size;
        }

        @Override
        public void recordAccess() {
            // 空实现
        }

        @Override
        public void recordRequest() {
            requestCount.increment();
        }

        @Override
        public void recordHit() {
            hitCount.increment();
        }

        @Override
        public void recordHits(long count) {
            hitCount.add(count);
        }

        @Override
        public void recordMiss() {
            missCount.increment();
        }

        @Override
        public void recordMisses(long count) {
            missCount.add(count);
        }

        @Override
        public void recordWrite() {
            writeCount.increment();
        }

        @Override
        public void recordWrites(long count) {
            writeCount.add(count);
        }

        @Override
        public void recordDelete() {
            deleteCount.increment();
        }

        @Override
        public void recordDeletes(long count) {
            deleteCount.add(count);
        }

        @Override
        public void recordClear() {
            // 空实现
        }

        @Override
        public void recordLoadStart() {
            loadCount.increment();
            loadStartTime = System.nanoTime();
        }

        @Override
        public void recordLoadEnd() {
            long loadTime = System.nanoTime() - loadStartTime;
            totalLoadTime.add(loadTime / 1_000_000); // 转换为毫秒
        }

        @Override
        public void recordLoadSuccess() {
            loadSuccessCount.increment();
        }

        @Override
        public void recordLoadFailure() {
            loadFailureCount.increment();
        }
    }
}