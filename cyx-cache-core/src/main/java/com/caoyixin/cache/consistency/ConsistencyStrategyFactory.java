package com.caoyixin.cache.consistency;

import com.caoyixin.cache.api.Cache;
import com.caoyixin.cache.api.ConsistencyStrategy;
import com.caoyixin.cache.enums.ConsistencyType;

import java.util.List;

/**
 * 一致性策略工厂，用于创建不同类型的一致性策略实例
 */
public class ConsistencyStrategyFactory {
    
    /**
     * 创建一致性策略
     *
     * @param type 一致性策略类型
     * @param caches 缓存列表
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 对应的一致性策略实例
     */
    public static <K, V> ConsistencyStrategy<K, V> createStrategy(ConsistencyType type, List<Cache<K, V>> caches) {
        if (type == null) {
            throw new IllegalArgumentException("一致性策略类型不能为空");
        }
        
        if (caches == null || caches.isEmpty()) {
            throw new IllegalArgumentException("缓存列表不能为空");
        }
        
        switch (type) {
            case WRITE_THROUGH:
                return new WriteThroughStrategy<>(caches);
            case WRITE_BACK:
                // 暂未实现写回策略，后续可扩展
                throw new UnsupportedOperationException("暂不支持写回策略");
            case READ_ONLY:
                // 暂未实现只读策略，后续可扩展
                throw new UnsupportedOperationException("暂不支持只读策略");
            default:
                throw new IllegalArgumentException("不支持的一致性策略类型: " + type);
        }
    }
}