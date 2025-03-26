package com.caoyixin.cache.consistency;

import com.caoyixin.cache.api.Cache;
import com.caoyixin.cache.api.ConsistencyStrategy;
import com.caoyixin.cache.config.CacheConfig;
import com.caoyixin.cache.enums.ConsistencyType;

import java.util.List;

/**
 * 一致性策略工厂接口
 */
public interface ConsistencyStrategyFactory {

    /**
     * 创建一致性策略
     *
     * @param type   一致性策略类型
     * @param caches 缓存列表
     * @param <K>    键类型
     * @param <V>    值类型
     * @return 一致性策略实例
     */
    <K, V> ConsistencyStrategy<K, V> createStrategy(ConsistencyType type, List<Cache<K, V>> caches);
}