package com.easy.cache.core;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * 缓存接口
 * 
 * 定义了缓存的基本操作，包括获取、设置、删除等功能
 */
public interface Cache<K, V> {

    /**
     * 获取缓存值
     * 
     * @param key 缓存键
     * @return 缓存值，如果不存在则返回null
     */
    V get(K key);

    /**
     * 获取缓存值，返回Optional
     * 
     * @param key 缓存键
     * @return 包装缓存值的Optional
     */
    default Optional<V> getOptional(K key) {
        return Optional.ofNullable(get(key));
    }

    /**
     * 获取缓存值，如果不存在则调用valueLoader并缓存结果
     * 
     * @param key         缓存键
     * @param valueLoader 值加载器
     * @return 缓存值或加载的值
     */
    V get(K key, Callable<V> valueLoader);

    /**
     * 异步获取缓存值
     * 
     * @param key 缓存键
     * @return 包含缓存值的CompletableFuture
     */
    default CompletableFuture<V> getAsync(K key) {
        return CompletableFuture.supplyAsync(() -> get(key));
    }

    /**
     * 异步获取缓存值，如果不存在则异步调用valueLoader并缓存结果
     * 
     * @param key         缓存键
     * @param valueLoader 值加载器
     * @return 包含缓存值或加载值的CompletableFuture
     */
    default CompletableFuture<V> getAsync(K key, Callable<V> valueLoader) {
        return CompletableFuture.supplyAsync(() -> get(key, valueLoader));
    }

    /**
     * 批量获取缓存值
     * 
     * @param keys 缓存键集合
     * @return 键值对映射，不存在的键不会包含在结果中
     */
    default Map<K, V> getAll(Set<K> keys) {
        return getAll((Collection<K>) keys);
    }

    /**
     * 批量获取缓存值
     * 
     * @param keys 缓存键集合
     * @return 键值对映射，不存在的键不会包含在结果中
     */
    Map<K, V> getAll(Collection<K> keys);

    /**
     * 异步批量获取缓存值
     * 
     * @param keys 缓存键集合
     * @return 包含键值对映射的CompletableFuture
     */
    default CompletableFuture<Map<K, V>> getAllAsync(Collection<K> keys) {
        return CompletableFuture.supplyAsync(() -> getAll(keys));
    }

    /**
     * 设置缓存值
     * 
     * @param key   缓存键
     * @param value 缓存值
     */
    void put(K key, V value);

    /**
     * 设置缓存值，带过期时间
     * 
     * @param key           缓存键
     * @param value         缓存值
     * @param expireSeconds 过期时间，单位为秒
     */
    void put(K key, V value, long expireSeconds);

    /**
     * 异步设置缓存值
     * 
     * @param key   缓存键
     * @param value 缓存值
     * @return 表示操作完成的CompletableFuture
     */
    default CompletableFuture<Void> putAsync(K key, V value) {
        return CompletableFuture.runAsync(() -> put(key, value));
    }

    /**
     * 异步设置缓存值，带过期时间
     * 
     * @param key           缓存键
     * @param value         缓存值
     * @param expireSeconds 过期时间，单位为秒
     * @return 表示操作完成的CompletableFuture
     */
    default CompletableFuture<Void> putAsync(K key, V value, long expireSeconds) {
        return CompletableFuture.runAsync(() -> put(key, value, expireSeconds));
    }

    /**
     * 批量设置缓存值
     * 
     * @param map 键值对映射
     */
    void putAll(Map<? extends K, ? extends V> map);

    /**
     * 批量设置缓存值，带过期时间
     * 
     * @param map           键值对映射
     * @param expireSeconds 过期时间，单位为秒
     */
    void putAll(Map<K, V> map, long expireSeconds);

    /**
     * 异步批量设置缓存值
     * 
     * @param map 键值对映射
     * @return 表示操作完成的CompletableFuture
     */
    default CompletableFuture<Void> putAllAsync(Map<? extends K, ? extends V> map) {
        return CompletableFuture.runAsync(() -> putAll(map));
    }

    /**
     * 仅当键不存在时，才设置缓存值
     * 
     * @param key   缓存键
     * @param value 缓存值
     * @return 如果设置成功（键不存在）则返回true，否则返回false
     */
    boolean putIfAbsent(K key, V value);

    /**
     * 删除缓存
     * 
     * @param key 缓存键
     */
    void remove(K key);

    /**
     * 异步删除缓存
     * 
     * @param key 缓存键
     * @return 表示操作完成的CompletableFuture
     */
    default CompletableFuture<Void> removeAsync(K key) {
        return CompletableFuture.runAsync(() -> remove(key));
    }

    /**
     * 批量删除缓存
     * 
     * @param keys 缓存键集合
     */
    void removeAll(Collection<? extends K> keys);

    /**
     * 异步批量删除缓存
     * 
     * @param keys 缓存键集合
     * @return 表示操作完成的CompletableFuture
     */
    default CompletableFuture<Void> removeAllAsync(Collection<? extends K> keys) {
        return CompletableFuture.runAsync(() -> removeAll(keys));
    }

    /**
     * 清空缓存
     */
    void clear();

    /**
     * 异步清空缓存
     * 
     * @return 表示操作完成的CompletableFuture
     */
    default CompletableFuture<Void> clearAsync() {
        return CompletableFuture.runAsync(this::clear);
    }

    /**
     * 获取缓存名称
     * 
     * @return 缓存名称
     */
    String getName();

    /**
     * 获取缓存配置
     * 
     * @return 缓存配置
     */
    CacheConfig getConfig();

    /**
     * 检查键是否存在
     * 
     * @param key 缓存键
     * @return 如果存在则返回true，否则返回false
     */
    boolean containsKey(K key);

    /**
     * 获取缓存统计信息
     * 
     * @return 缓存统计信息
     */
    CacheStats stats();

    /**
     * 设置过期时间
     * 
     * @param key           缓存键
     * @param expireSeconds 过期时间，单位为秒
     */
    void expire(K key, long expireSeconds);

    /**
     * 获取剩余过期时间
     * 
     * @param key 缓存键
     * @return 剩余过期时间（秒），-1表示永不过期，-2表示键不存在
     */
    long ttl(K key);

    /**
     * 增加数值
     * 
     * @param key   缓存键
     * @param delta 增量
     * @return 增加后的值
     */
    long increment(K key, long delta);

    /**
     * 减少数值
     * 
     * @param key   缓存键
     * @param delta 减量
     * @return 减少后的值
     */
    default long decrement(K key, long delta) {
        return increment(key, -delta);
    }
}