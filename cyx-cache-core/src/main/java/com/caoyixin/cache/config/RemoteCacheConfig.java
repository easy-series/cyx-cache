package com.caoyixin.cache.config;

import com.caoyixin.cache.consistency.ConsistencyMode;

import lombok.Builder;
import lombok.Data;

/**
 * 远程缓存配置
 */
@Data
@Builder
public class RemoteCacheConfig {
    /**
     * 远程缓存类型 (redis, memcached等)
     */
    @Builder.Default
    private String type = "redis";

    /**
     * 键转换器类型
     */
    @Builder.Default
    private String keyConvertor = "fastjson";

    /**
     * 值编码器类型
     */
    @Builder.Default
    private String valueEncoder = "java";

    /**
     * 值解码器类型
     */
    @Builder.Default
    private String valueDecoder = "java";

    /**
     * 一致性模式
     */
    @Builder.Default
    private ConsistencyMode consistencyMode = ConsistencyMode.WRITE_THROUGH;

    /**
     * 是否同步本地缓存
     */
    @Builder.Default
    private boolean syncLocal = true;

    /**
     * 是否防止缓存击穿
     */
    @Builder.Default
    private boolean penetrationProtect = false;

    /**
     * 缓存区域
     */
    private String area;

    /**
     * 广播通道
     */
    private String broadcastChannel;
}