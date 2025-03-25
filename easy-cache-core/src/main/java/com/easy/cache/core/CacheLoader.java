package com.easy.cache.core;

/**
 * 缓存加载器接口
 * 
 * 定义了缓存未命中时如何加载数据的方法
 *
 * @param <K> 缓存键类型
 * @param <V> 缓存值类型
 */
public interface CacheLoader<K, V> {

    /**
     * 当缓存未命中时加载数据
     * 
     * @param key 缓存键
     * @return 加载的值，如果加载失败可能返回null
     * @throws Exception 如果加载过程中发生异常
     */
    V load(K key) throws Exception;

    /**
     * 判断加载结果是否成功
     * <p>
     * 默认实现为只要结果不为null就认为成功
     * 
     * @param key   缓存键
     * @param value 加载的值
     * @return 是否加载成功
     */
    default boolean isLoadSuccess(K key, V value) {
        return value != null;
    }

    /**
     * 在缓存数据前处理加载的值
     * <p>
     * 默认实现为不做任何处理直接返回
     * 
     * @param key   缓存键
     * @param value 加载的值
     * @return 处理后的值
     */
    default V beforeCache(K key, V value) {
        return value;
    }

    /**
     * 在加载失败时的处理策略
     * <p>
     * 默认实现为返回null
     * 
     * @param key       缓存键
     * @param exception 异常
     * @return 替代的值，通常为null
     */
    default V onLoadFailure(K key, Exception exception) {
        return null;
    }

    /**
     * 批量加载多个键的值
     * <p>
     * 默认实现为依次调用单个加载
     * 
     * @param keys 缓存键集合
     * @return 批量加载的结果
     */
    default BatchLoader<K, V> toBatchLoader() {
        return keys -> {
            throw new UnsupportedOperationException("批量加载未实现");
        };
    }

    /**
     * 是否支持批量加载
     * 
     * @return 是否支持批量加载
     */
    default boolean supportBatch() {
        return false;
    }
} 