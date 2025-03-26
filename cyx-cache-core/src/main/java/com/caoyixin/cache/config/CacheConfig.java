package com.caoyixin.cache.config;

import com.caoyixin.cache.api.CacheType;
import com.caoyixin.cache.enums.ConsistencyType;
import lombok.Builder;
import lombok.Data;

import java.time.Duration;

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
     * 一致性类型
     */
    private ConsistencyType consistencyType = ConsistencyType.WRITE_THROUGH;
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
     * 获取缓存最大容量
     *
     * @return 缓存最大容量
     */
    public int getMaxSize() {
        return localLimit;
    }

}