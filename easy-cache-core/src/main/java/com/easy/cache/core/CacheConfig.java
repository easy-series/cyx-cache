package com.easy.cache.core;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 缓存配置类
 * 
 * 包含本地缓存和远程缓存的配置参数
 */
@Data
@Slf4j
@Builder
public class CacheConfig {

    /**
     * 统计模式
     */
    public enum StatisticsMode {
        /**
         * 不统计
         */
        NONE,
        /**
         * 基本统计
         */
        BASIC,
        /**
         * 详细统计
         */
        DETAILED
    }

    /**
     * 缓存名称
     */
    private String name;

    /**
     * 缓存类型
     */
    @Builder.Default
    private CacheType cacheType = CacheType.LOCAL;

    /**
     * 统计模式
     */
    @Builder.Default
    private StatisticsMode statisticsMode = StatisticsMode.BASIC;

    /**
     * 缓存键的前缀
     */
    private String keyPrefix;

    /**
     * 是否启用缓存
     */
    @Builder.Default
    private boolean enabled = true;

    /**
     * 是否允许空值缓存
     */
    @Builder.Default
    private boolean allowNullValues = true;

    // 本地缓存配置

    /**
     * 本地缓存过期时间（秒）
     */
    @Builder.Default
    private long localExpireSeconds = 300;

    /**
     * 本地缓存最大容量
     */
    @Builder.Default
    private long localMaxSize = 10000;

    /**
     * 是否使用软引用值
     */
    @Builder.Default
    private boolean localSoftValues = false;

    /**
     * 是否使用弱引用键
     */
    @Builder.Default
    private boolean localWeakKeys = false;

    /**
     * 是否记录本地缓存统计
     */
    @Builder.Default
    private boolean localRecordStats = true;

    /**
     * 本地缓存预加载大小
     */
    @Builder.Default
    private int localInitialCapacity = 100;

    /**
     * 本地缓存自动刷新时间（秒），0表示不自动刷新
     */
    @Builder.Default
    private long localRefreshSeconds = 0;
    
    // 远程缓存配置

    /**
     * 远程缓存过期时间（秒）
     */
    @Builder.Default
    private long remoteExpireSeconds = 600;

    /**
     * 是否记录远程缓存统计
     */
    @Builder.Default
    private boolean remoteRecordStats = true;

    /**
     * 远程缓存自动刷新时间（秒），0表示不自动刷新
     */
    @Builder.Default
    private long remoteRefreshSeconds = 0;

    /**
     * 远程缓存Key的序列化器
     */
    @Builder.Default
    private String remoteKeySerializer = "string";

    /**
     * 远程缓存Value的序列化器
     */
    @Builder.Default
    private String remoteValueSerializer = "java";

    /**
     * 远程缓存连接超时（毫秒）
     */
    @Builder.Default
    private long remoteConnectTimeout = 5000;

    /**
     * 远程缓存操作超时（毫秒）
     */
    @Builder.Default
    private long remoteOperationTimeout = 3000;

    /**
     * 是否使用远程缓存的异步操作
     */
    @Builder.Default
    private boolean remoteUseAsyncOperations = true;

    /**
     * 缓存过期后的移除策略
     */
    public enum CacheRemovalPolicy {
        /**
         * 立即移除
         */
        IMMEDIATE,
        /**
         * 延迟移除
         */
        LAZY
    }

    /**
     * 本地缓存移除策略
     */
    @Builder.Default
    private CacheRemovalPolicy localRemovalPolicy = CacheRemovalPolicy.LAZY;

    /**
     * 远程缓存移除策略
     */
    @Builder.Default
    private CacheRemovalPolicy remoteRemovalPolicy = CacheRemovalPolicy.LAZY;

    /**
     * 多级缓存的写策略
     */
    public enum WritePolicy {
        /**
         * 写透传：同时写入本地和远程缓存
         */
        WRITE_THROUGH,
        /**
         * 写回：仅写入本地缓存，延迟写入远程缓存
         */
        WRITE_BACK,
        /**
         * 写入本地：仅写入本地缓存
         */
        WRITE_LOCAL_ONLY,
        /**
         * 写入远程：仅写入远程缓存
         */
        WRITE_REMOTE_ONLY
    }

    /**
     * 多级缓存的写策略
     */
    @Builder.Default
    private WritePolicy writePolicy = WritePolicy.WRITE_THROUGH;

    /**
     * 多级缓存的读策略
     */
    public enum ReadPolicy {
        /**
         * 先读本地，如果本地不存在则读远程并回填本地
         */
        READ_LOCAL_REMOTE_BACKFILL,
        /**
         * 仅读本地
         */
        READ_LOCAL_ONLY,
        /**
         * 仅读远程
         */
        READ_REMOTE_ONLY,
        /**
         * 读本地和远程，取最新值（基于版本号）
         */
        READ_LATEST
    }

    /**
     * 多级缓存的读策略
     */
    @Builder.Default
    private ReadPolicy readPolicy = ReadPolicy.READ_LOCAL_REMOTE_BACKFILL;

    /**
     * 写回策略的延迟时间（毫秒）
     */
    @Builder.Default
    private long writeBackDelayMillis = 500;

    /**
     * 是否启用版本控制
     */
    @Builder.Default
    private boolean enableVersioning = false;

    /**
     * 是否启用缓存事件监听
     */
    @Builder.Default
    private boolean enableCacheEvents = false;

    /**
     * 创建默认配置
     * 
     * @return 默认缓存配置
     */
    public static CacheConfig defaultConfig() {
        return CacheConfig.builder().build();
    }

    /**
     * 创建本地缓存默认配置
     * 
     * @return 本地缓存默认配置
     */
    public static CacheConfig localConfig() {
        return CacheConfig.builder()
                .cacheType(CacheType.LOCAL)
                .build();
    }

    /**
     * 创建远程缓存默认配置
     * 
     * @return 远程缓存默认配置
     */
    public static CacheConfig remoteConfig() {
        return CacheConfig.builder()
                .cacheType(CacheType.REMOTE)
                .build();
    }

    /**
     * 创建多级缓存默认配置
     * 
     * @return 多级缓存默认配置
     */
    public static CacheConfig multiLevelConfig() {
        return CacheConfig.builder()
                .cacheType(CacheType.MULTILEVEL)
                .build();
    }
} 