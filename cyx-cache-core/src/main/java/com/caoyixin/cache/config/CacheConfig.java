package com.caoyixin.cache.config;

import java.time.Duration;

import com.caoyixin.cache.api.CacheType;
import com.caoyixin.cache.consistency.ConsistencyMode;
import com.caoyixin.cache.enums.ConsistencyType;

import lombok.Builder;
import lombok.Data;

/**
 * 缓存配置类
 */
@Data
@Builder
public class CacheConfig {
    /**
     * 缓存名称
     */
    private final String name;

    /**
     * 过期时间
     */
    private Duration expire;

    /**
     * 缓存类型
     */
    @Builder.Default
    private CacheType cacheType = CacheType.REMOTE;

    /**
     * 本地缓存最大容量
     */
    private int localLimit = 200;
    /**
     * 本地缓存过期时间
     */
    private Duration localExpire;
    /**
     * 一致性模式
     */
    private ConsistencyMode consistencyMode = ConsistencyMode.WRITE_THROUGH;
    /**
     * 一致性类型
     */
    private ConsistencyType consistencyType;
    /**
     * 是否同步本地缓存
     */
    private boolean syncLocal = true;
    /**
     * 键转换器类型
     */
    private String keyConvertor = "fastjson";
    /**
     * 值编码器类型
     */
    private String valueEncoder = "java";
    /**
     * 值解码器类型
     */
    private String valueDecoder = "java";
    /**
     * 是否启用缓存统计
     */
    @Builder.Default
    private boolean statsEnabled = true;
    /**
     * 是否防止缓存击穿
     */
    private boolean penetrationProtect = false;
    /**
     * 刷新策略配置
     */
    private RefreshPolicy refreshPolicy;

    /**
     * 本地缓存配置
     */
    private LocalCacheConfig localConfig;

    /**
     * 远程缓存配置
     */
    private RemoteCacheConfig remoteConfig;

    /**
     * 获取缓存最大容量
     *
     * @return 缓存最大容量
     */
    public int getMaxSize() {
        return localLimit;
    }

    /**
     * 获取一致性类型
     *
     * @return 一致性类型
     */
    public ConsistencyType getConsistencyType() {
        if (consistencyType != null) {
            return consistencyType;
        }

        // 从ConsistencyMode映射到ConsistencyType
        switch (consistencyMode) {
            case WRITE_THROUGH:
                return ConsistencyType.WRITE_THROUGH;
            case WRITE_BACK:
                return ConsistencyType.WRITE_BACK;
            case READ_ONLY:
                return ConsistencyType.READ_ONLY;
            default:
                return ConsistencyType.WRITE_THROUGH;
        }
    }

    /**
     * 获取本地缓存配置，如果不存在则创建默认配置
     */
    public LocalCacheConfig getLocalConfig() {
        if (localConfig == null && (cacheType == CacheType.LOCAL || cacheType == CacheType.BOTH)) {
            localConfig = LocalCacheConfig.builder().build();
        }
        return localConfig;
    }

    /**
     * 获取远程缓存配置，如果不存在则创建默认配置
     */
    public RemoteCacheConfig getRemoteConfig() {
        if (remoteConfig == null && (cacheType == CacheType.REMOTE || cacheType == CacheType.BOTH)) {
            remoteConfig = RemoteCacheConfig.builder().build();
        }
        return remoteConfig;
    }
}