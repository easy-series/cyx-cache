package com.caoyixin.cache.enums;

/**
 * 缓存一致性策略类型枚举
 */
public enum ConsistencyType {
    
    /**
     * 写同步策略：所有写操作同时更新所有级别的缓存
     */
    WRITE_THROUGH,
    
    /**
     * 写回策略：先写L1缓存，异步写L2缓存
     */
    WRITE_BACK,
    
    /**
     * 只读策略：只从缓存读取，不更新缓存
     */
    READ_ONLY
} 