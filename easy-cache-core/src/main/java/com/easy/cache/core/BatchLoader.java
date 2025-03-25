package com.easy.cache.core;

import java.util.Collection;
import java.util.Map;

/**
 * 批量加载器接口
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
@FunctionalInterface
public interface BatchLoader<K, V> {
    
    /**
     * 批量加载多个键的值
     *
     * @param keys 要加载的键集合
     * @return 键值对映射结果
     * @throws Exception 如果加载过程中发生异常
     */
    Map<K, V> loadAll(Collection<K> keys) throws Exception;
} 