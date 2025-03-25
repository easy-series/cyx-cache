package com.easy.cache.core;

/**
 * 缓存类型枚举
 */
public enum CacheType {
    /**
     * 本地缓存
     */
    LOCAL,
    
    /**
     * 远程缓存
     */
    REMOTE,
    
    /**
     * 多级缓存（本地+远程）
     */
    MULTILEVEL
} 