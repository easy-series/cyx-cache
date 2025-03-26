package com.caoyixin.cache.api;

import java.time.Duration;

/**
 * 分布式锁接口
 *
 * @param <K> 锁键类型
 */
public interface DistributedLock<K> {
    /**
     * 尝试获取分布式锁
     *
     * @param key     锁的键
     * @param timeout 超时时间
     * @return 获取成功返回true，否则返回false
     */
    boolean tryLock(K key, Duration timeout);

    /**
     * 释放分布式锁
     *
     * @param key 锁的键
     */
    void unlock(K key);

    /**
     * 尝试获取锁并执行操作
     *
     * @param key     锁的键
     * @param timeout 超时时间
     * @param action  要执行的操作
     * @return 获取锁并执行成功返回true，否则返回false
     */
    default boolean tryLockAndRun(K key, Duration timeout, Runnable action) {
        if (tryLock(key, timeout)) {
            try {
                action.run();
                return true;
            } finally {
                unlock(key);
            }
        }
        return false;
    }
}