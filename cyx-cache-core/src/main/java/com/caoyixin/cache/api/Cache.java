package com.caoyixin.cache.api;

import java.time.Duration;
import java.util.Map;
import java.util.function.Function;

/**
 * 缓存接口，定义缓存的基本操作
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
public interface Cache<K, V> {
    /**
     * 根据键获取缓存中的值
     *
     * @param key 缓存键
     * @return 缓存值，如果不存在返回null
     */
    V get(K key);

    /**
     * 将键值对放入缓存
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    void put(K key, V value);

    /**
     * 将键值对放入缓存，并设置过期时间
     *
     * @param key   缓存键
     * @param value 缓存值
     * @param ttl   过期时间
     */
    void put(K key, V value, Duration ttl);

    /**
     * 批量将键值对放入缓存
     *
     * @param map 要缓存的键值对
     */
    void putAll(Map<? extends K, ? extends V> map);

    /**
     * 如果缓存中不存在该键，则通过loader加载值并缓存
     *
     * @param key    缓存键
     * @param loader 值加载器
     * @return 缓存中的值或新加载的值
     */
    V computeIfAbsent(K key, Function<K, V> loader);

    /**
     * 如果缓存中不存在该键，则通过loader加载值并缓存，同时设置过期时间
     *
     * @param key    缓存键
     * @param loader 值加载器
     * @param ttl    过期时间
     * @return 缓存中的值或新加载的值
     */
    V computeIfAbsent(K key, Function<K, V> loader, Duration ttl);

    /**
     * 从缓存中移除指定键的值
     *
     * @param key 要移除的键
     * @return 如果值存在并被移除则返回true，否则返回false
     */
    boolean remove(K key);

    /**
     * 清空缓存中的所有内容
     */
    void clear();

    /**
     * 获取缓存名称
     *
     * @return 缓存名称
     */
    String getName();

    /**
     * 获取缓存统计信息
     *
     * @return 缓存统计信息
     */
    CacheStats stats();

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
    boolean tryLockAndRun(K key, Duration timeout, Runnable action);
}