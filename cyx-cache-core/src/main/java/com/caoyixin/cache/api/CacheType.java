package com.caoyixin.cache.api;

/**
 * 缓存类型
 */
public enum CacheType {
    /**
     * 仅本地缓存
     */
    LOCAL,

    /**
     * 仅远程缓存
     */
    REMOTE,

    /**
     * 两级缓存(本地+远程)
     */
    BOTH
} 