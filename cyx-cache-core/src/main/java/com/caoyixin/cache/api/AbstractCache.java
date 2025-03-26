package com.caoyixin.cache.api;

import java.time.Duration;
import java.util.Map;
import java.util.function.Function;

import com.caoyixin.cache.exception.CacheException;

import lombok.extern.slf4j.Slf4j;

/**
 * 抽象缓存实现，处理通用逻辑和统计功能
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
@Slf4j
public abstract class AbstractCache<K, V> implements Cache<K, V> {

    protected final String name;
    protected final CacheStats stats;

    /**
     * 创建抽象缓存
     *
     * @param name 缓存名称
     */
    public AbstractCache(String name) {
        this.name = name;
        this.stats = new CacheStats(name);
    }

    @Override
    public V get(K key) {
        if (key == null) {
            return null;
        }

        try {
            V value = doGet(key);
            if (value != null) {
                stats.recordHit();
            } else {
                stats.recordMiss();
            }
            return value;
        } catch (Exception e) {
            stats.recordMiss();
            handleException("获取缓存值异常", e, key);
            return null;
        }
    }

    @Override
    public void put(K key, V value) {
        if (key == null) {
            return;
        }

        try {
            doPut(key, value, null);
        } catch (Exception e) {
            handleException("存储缓存值异常", e, key);
        }
    }

    @Override
    public void put(K key, V value, Duration ttl) {
        if (key == null) {
            return;
        }

        try {
            doPut(key, value, ttl);
        } catch (Exception e) {
            handleException("存储缓存值异常", e, key);
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        if (map == null || map.isEmpty()) {
            return;
        }

        try {
            doPutAll(map);
        } catch (Exception e) {
            handleException("批量存储缓存值异常", e, null);
        }
    }

    @Override
    public V computeIfAbsent(K key, Function<K, V> loader) {
        return computeIfAbsent(key, loader, null);
    }

    @Override
    public V computeIfAbsent(K key, Function<K, V> loader, Duration ttl) {
        if (key == null || loader == null) {
            return null;
        }

        V value = get(key);
        if (value != null) {
            return value;
        }

        try {
            stats.recordLoadStart();
            long startTime = System.currentTimeMillis();

            value = doComputeIfAbsent(key, loader, ttl);

            if (value != null) {
                stats.recordLoadSuccess(System.currentTimeMillis() - startTime);
            } else {
                stats.recordLoadFailure();
            }

            return value;
        } catch (Exception e) {
            stats.recordLoadFailure();
            handleException("加载缓存值异常", e, key);
            throw new CacheException("加载缓存值异常", e);
        }
    }

    @Override
    public boolean remove(K key) {
        if (key == null) {
            return false;
        }

        try {
            return doRemove(key);
        } catch (Exception e) {
            handleException("移除缓存值异常", e, key);
            return false;
        }
    }

    @Override
    public void clear() {
        try {
            doClear();
        } catch (Exception e) {
            handleException("清空缓存异常", e, null);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public CacheStats stats() {
        return stats;
    }

    /**
     * 处理异常
     *
     * @param message 错误消息
     * @param e       异常
     * @param key     相关的键
     */
    protected void handleException(String message, Exception e, K key) {
        if (key != null) {
            log.error("{}, cacheName={}, key={}", message, name, key, e);
        } else {
            log.error("{}, cacheName={}", message, name, e);
        }
    }

    /**
     * 实际获取缓存值的实现
     *
     * @param key 缓存键
     * @return 缓存值
     */
    protected abstract V doGet(K key);

    /**
     * 实际存储缓存值的实现
     *
     * @param key   缓存键
     * @param value 缓存值
     * @param ttl   过期时间
     */
    protected abstract void doPut(K key, V value, Duration ttl);

    /**
     * 实际批量存储缓存值的实现
     *
     * @param map 缓存键值对
     */
    protected abstract void doPutAll(Map<? extends K, ? extends V> map);

    /**
     * 实际计算缓存值的实现
     *
     * @param key    缓存键
     * @param loader 值加载器
     * @param ttl    过期时间
     * @return 缓存值
     */
    protected abstract V doComputeIfAbsent(K key, Function<K, V> loader, Duration ttl);

    /**
     * 实际移除缓存值的实现
     *
     * @param key 缓存键
     * @return 是否成功移除
     */
    protected abstract boolean doRemove(K key);

    /**
     * 实际清空缓存的实现
     */
    protected abstract void doClear();
}