package com.easy.cache.core;

/**
 * 缓存统计接口
 * 
 * 定义了获取缓存统计信息的方法
 */
public interface CacheStats {

    /**
     * 获取缓存名称
     * 
     * @return 缓存名称
     */
    String getName();

    /**
     * 获取缓存请求次数
     * 
     * @return 请求次数
     */
    long getRequestCount();

    /**
     * 获取缓存命中次数
     * 
     * @return 命中次数
     */
    long getHitCount();

    /**
     * 获取缓存未命中次数
     * 
     * @return 未命中次数
     */
    long getMissCount();

    /**
     * 获取缓存命中率
     * 
     * @return 命中率（0-1之间的值）
     */
    default double getHitRate() {
        long requestCount = getRequestCount();
        if (requestCount == 0) {
            return 0.0;
        }
        return (double) getHitCount() / requestCount;
    }

    /**
     * 获取缓存条目数量
     * 
     * @return 缓存条目数量
     */
    long getSize();

    /**
     * 获取缓存写入次数
     * 
     * @return 写入次数
     */
    long getWriteCount();

    /**
     * 获取缓存删除次数
     * 
     * @return 删除次数
     */
    long getDeleteCount();

    /**
     * 获取缓存加载次数
     * 
     * @return 加载次数
     */
    long getLoadCount();

    /**
     * 获取缓存加载成功次数
     * 
     * @return 加载成功次数
     */
    long getLoadSuccessCount();

    /**
     * 获取缓存加载失败次数
     * 
     * @return 加载失败次数
     */
    long getLoadFailureCount();

    /**
     * 获取平均加载时间（毫秒）
     * 
     * @return 平均加载时间
     */
    double getAverageLoadPenalty();

    /**
     * 获取总加载时间（毫秒）
     * 
     * @return 总加载时间
     */
    long getTotalLoadTime();

    /**
     * 获取累计驱逐次数（由于容量限制或过期）
     * 
     * @return 驱逐次数
     */
    long getEvictionCount();

    /**
     * 重置统计信息
     */
    void resetStats();

    /**
     * 获取最后一次重置统计的时间戳
     * 
     * @return 时间戳（毫秒）
     */
    long getLastResetTime();

    /**
     * 获取缓存类型
     * 
     * @return 缓存类型
     */
    CacheType getCacheType();

    /**
     * 获取缓存当前容量（如果支持的话）
     * 
     * @return 缓存当前容量，如不支持返回-1
     */
    default long getCapacity() {
        return -1;
    }

    /**
     * 记录缓存访问（用于统计）
     */
    void recordAccess();

    /**
     * 记录缓存请求（用于统计）
     */
    void recordRequest();

    /**
     * 记录缓存命中（用于统计）
     */
    void recordHit();

    /**
     * 记录多次缓存命中（用于统计）
     * 
     * @param count 命中次数
     */
    void recordHits(long count);

    /**
     * 记录缓存未命中（用于统计）
     */
    void recordMiss();

    /**
     * 记录多次缓存未命中（用于统计）
     * 
     * @param count 未命中次数
     */
    void recordMisses(long count);

    /**
     * 记录缓存写入（用于统计）
     */
    void recordWrite();

    /**
     * 记录多次缓存写入（用于统计）
     * 
     * @param count 写入次数
     */
    void recordWrites(long count);

    /**
     * 记录缓存删除（用于统计）
     */
    void recordDelete();

    /**
     * 记录多次缓存删除（用于统计）
     * 
     * @param count 删除次数
     */
    void recordDeletes(long count);

    /**
     * 记录缓存清空（用于统计）
     */
    void recordClear();

    /**
     * 记录加载开始（用于统计）
     */
    void recordLoadStart();

    /**
     * 记录加载结束（用于统计）
     */
    void recordLoadEnd();

    /**
     * 记录加载成功（用于统计）
     */
    void recordLoadSuccess();

    /**
     * 记录加载失败（用于统计）
     */
    void recordLoadFailure();

    /**
     * 设置缓存大小（用于统计）
     * 
     * @param size 缓存大小
     */
    void setSize(long size);
}