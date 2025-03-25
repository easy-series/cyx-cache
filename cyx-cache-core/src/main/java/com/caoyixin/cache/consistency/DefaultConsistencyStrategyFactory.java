package com.caoyixin.cache.consistency;

import com.caoyixin.cache.api.Cache;
import com.caoyixin.cache.api.ConsistencyStrategy;
import com.caoyixin.cache.config.CacheConfig;
import com.caoyixin.cache.enums.ConsistencyType;

import java.util.List;

/**
 * 默认一致性策略工厂实现
 */
public class DefaultConsistencyStrategyFactory implements ConsistencyStrategyFactory {

    @Override
    public <K, V> ConsistencyStrategy createStrategy(CacheConfig config, List<Cache<K, V>> caches) {
        switch (config.getConsistencyMode()) {
            case WRITE_THROUGH:
                return new WriteThroughStrategy<>(caches);
            case WRITE_BACK:
                throw new UnsupportedOperationException("写回策略尚未实现");
            case READ_ONLY:
                throw new UnsupportedOperationException("只读策略尚未实现");
            default:
                throw new IllegalArgumentException("不支持的一致性模式: " + config.getConsistencyMode());
        }
    }

    @Override
    public <K, V> ConsistencyStrategy<K, V> createStrategy(ConsistencyType type, List<Cache<K, V>> caches) {
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