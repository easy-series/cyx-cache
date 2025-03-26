package com.caoyixin.cache.config;

import java.time.Duration;

import lombok.Builder;
import lombok.Data;

/**
 * 本地缓存配置
 */
@Data
@Builder
public class LocalCacheConfig {
    /**
     * 本地缓存最大容量
     */
    @Builder.Default
    private int maxSize = 200;

    /**
     * 本地缓存过期时间
     */
    private Duration expire;

    /**
     * 键转换器类型
     */
    @Builder.Default
    private String keyConvertor = "fastjson";

    /**
     * 是否启用过期监听
     */
    @Builder.Default
    private boolean expireListenerEnabled = false;

    /**
     * 缓存类型 (caffeine, linkedHashMap等)
     */
    @Builder.Default
    private String type = "caffeine";

    /**
     * 刷新策略配置
     */
    private RefreshPolicy refreshPolicy;
}