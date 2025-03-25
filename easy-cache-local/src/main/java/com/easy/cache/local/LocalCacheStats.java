package com.easy.cache.local;

import com.easy.cache.core.CacheStats;
import com.easy.cache.core.CacheType;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 本地缓存统计信息
 */
public class LocalCacheStats implements CacheStats {

    private final String name;
    private final LongAdder requestCount = new LongAdder();
    private final LongAdder hitCount = new LongAdder();
    private final LongAdder missCount = new LongAdder();
    private final LongAdder loadCount = new LongAdder();
    private final LongAdder loadSuccessCount = new LongAdder();
    private final LongAdder loadFailureCount = new LongAdder();
    private final LongAdder totalLoadTime = new LongAdder();
    private final LongAdder evictionCount = new LongAdder();
    private final LongAdder writeCount = new LongAdder();
    private final LongAdder deleteCount = new LongAdder();
    private final AtomicLong lastResetTime = new AtomicLong(System.currentTimeMillis());
    private volatile long loadStartTime;

    @Getter
    @Setter
    private long size;

    /**
     * 创建LocalCacheStats实例
     *
     * @param name 缓存名称
     */
    public LocalCacheStats(String name) {
        this.name = name;
    }

    /**
     * 从Caffeine的统计信息更新
     *
     * @param caffeineStats Caffeine的统计信息
     * @param statsCounter  Caffeine的统计计数器
     */
    public void updateFrom(com.github.benmanes.caffeine.cache.stats.CacheStats caffeineStats,
            CaffeineLocalCache.CaffeineStatsCounter statsCounter) {
        // Caffeine的统计信息是快照，需要增量更新
        long hits = caffeineStats.hitCount();
        long misses = caffeineStats.missCount();
        long loadSuccesses = caffeineStats.loadSuccessCount();
        long loadFailures = caffeineStats.loadFailureCount();
        long evictions = caffeineStats.evictionCount();

        // 更新本地统计信息
        this.hitCount.reset();
        this.hitCount.add(hits);

        this.missCount.reset();
        this.missCount.add(misses);

        this.loadSuccessCount.reset();
        this.loadSuccessCount.add(loadSuccesses);

        this.loadFailureCount.reset();
        this.loadFailureCount.add(loadFailures);

        this.evictionCount.reset();
        this.evictionCount.add(evictions);

        this.totalLoadTime.reset();
        this.totalLoadTime.add(caffeineStats.totalLoadTime());
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
        return evictionCount.sum();
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
        evictionCount.reset();
        writeCount.reset();
        deleteCount.reset();
        lastResetTime.set(System.currentTimeMillis());
    }

    @Override
    public long getLastResetTime() {
        return lastResetTime.get();
    }

    @Override
    public CacheType getCacheType() {
        return CacheType.LOCAL;
    }

    @Override
    public void recordAccess() {
        // 空实现，在get/getAll方法中已统计
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
        // 暂不统计clear次数
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