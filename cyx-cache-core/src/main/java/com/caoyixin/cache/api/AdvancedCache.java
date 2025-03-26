package com.caoyixin.cache.api;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * 高级缓存接口，提供更多高级功能
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
public interface AdvancedCache<K, V> extends Cache<K, V> {

    /**
     * 异步获取缓存值
     *
     * @param key 缓存键
     * @return 包含缓存值的CompletableFuture
     */
    CompletableFuture<V> getAsync(K key);

    /**
     * 如果支持，获取关联的分布式锁
     *
     * @return 分布式锁实例，如果不支持返回null
     */
    DistributedLock<K> getDistributedLock();

    /**
     * 预热缓存
     *
     * @param loader 数据加载器
     */
    void preload(Function<String, Iterable<K>> loader);

    /**
     * 获取指定键的剩余生存时间
     *
     * @param key 缓存键
     * @return 剩余生存时间的毫秒数，如果键不存在或没有设置过期时间则返回-1
     */
    long getExpireTime(K key);
}