package com.caoyixin.cache.api;

import lombok.Getter;
import lombok.ToString;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 缓存统计信息
 */
@Getter
@ToString
public class CacheStats {
    private final String cacheName;
    private final LongAdder hits = new LongAdder();
    private final LongAdder misses = new LongAdder();
    private final LongAdder loads = new LongAdder();
    private final LongAdder loadSuccesses = new LongAdder();
    private final LongAdder loadFailures = new LongAdder();
    private final LongAdder totalLoadTime = new LongAdder();
    private final LongAdder evictions = new LongAdder();
    private final AtomicLong size = new AtomicLong();

    /**
     * 创建缓存统计对象
     *
     * @param cacheName 缓存名称
     */
    public CacheStats(String cacheName) {
        this.cacheName = cacheName;
    }

    /**
     * 记录缓存命中
     */
    public void recordHit() {
        hits.increment();
    }

    /**
     * 记录缓存未命中
     */
    public void recordMiss() {
        misses.increment();
    }

    /**
     * 记录加载开始
     */
    public void recordLoadStart() {
        loads.increment();
    }

    /**
     * 记录加载成功
     *
     * @param loadTime 加载耗时(毫秒)
     */
    public void recordLoadSuccess(long loadTime) {
        loadSuccesses.increment();
        totalLoadTime.add(loadTime);
    }

    /**
     * 记录加载失败
     */
    public void recordLoadFailure() {
        loadFailures.increment();
    }

    /**
     * 记录缓存淘汰
     */
    public void recordEviction() {
        evictions.increment();
    }

    /**
     * 更新缓存大小
     *
     * @param currentSize 当前大小
     */
    public void updateSize(long currentSize) {
        size.set(currentSize);
    }

    /**
     * 获取缓存命中率
     *
     * @return 命中率(0 - 1)
     */
    public double hitRate() {
        long hitCount = hits.sum();
        long requestCount = hitCount + misses.sum();
        return requestCount == 0 ? 1.0 : (double) hitCount / requestCount;
    }

    /**
     * 获取负载平均时间
     *
     * @return 平均加载时间(毫秒)
     */
    public double avgLoadTime() {
        long totalSuccesses = loadSuccesses.sum();
        return totalSuccesses == 0 ? 0.0 : (double) totalLoadTime.sum() / totalSuccesses;
    }

    /**
     * 获取加载成功率
     *
     * @return 加载成功率(0 - 1)
     */
    public double loadSuccessRate() {
        long totalLoads = loads.sum();
        return totalLoads == 0 ? 1.0 : (double) loadSuccesses.sum() / totalLoads;
    }

    /**
     * 获取请求次数(命中+未命中)
     *
     * @return 请求次数
     */
    public long requestCount() {
        return hits.sum() + misses.sum();
    }
}