package com.caoyixin.cache.consistency;

import com.caoyixin.cache.config.CacheConfig;

/**
 * 默认一致性策略工厂实现
 */
public class DefaultConsistencyStrategyFactory implements ConsistencyStrategyFactory {

    @Override
    public ConsistencyStrategy createStrategy(CacheConfig config) {
        switch (config.getConsistencyMode()) {
            case WRITE_THROUGH:
                return new WriteThroughStrategy();
            case WRITE_BACK:
                throw new UnsupportedOperationException("写回策略尚未实现");
            case READ_ONLY:
                throw new UnsupportedOperationException("只读策略尚未实现");
            default:
                throw new IllegalArgumentException("不支持的一致性模式: " + config.getConsistencyMode());
        }
    }
}