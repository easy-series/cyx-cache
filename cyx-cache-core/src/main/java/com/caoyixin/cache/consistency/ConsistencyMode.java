package com.caoyixin.cache.consistency;

/**
 * 缓存一致性模式
 */
public enum ConsistencyMode {
    /**
     * 写同步策略 - 先写远程，再写本地，然后通知其他节点
     */
    WRITE_THROUGH,

    /**
     * 写回策略 - 先写本地，然后异步批量写入远程
     */
    WRITE_BACK,

    /**
     * 只读策略 - 只更新远程缓存，不主动更新本地缓存，只通过订阅使本地缓存失效
     */
    READ_ONLY
}