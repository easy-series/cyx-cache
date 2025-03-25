package com.caoyixin.cache.multilevel;

import com.caoyixin.cache.api.Cache;
import com.caoyixin.cache.api.CacheStats;
import com.caoyixin.cache.api.ConsistencyStrategy;
import com.caoyixin.cache.config.CacheConfig;
import com.caoyixin.cache.event.CacheUpdateEvent;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 多级缓存实现
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
@Slf4j
public class MultiLevelCache<K, V> implements Cache<K, V> {

    private final String name;
    private final List<Cache<K, V>> caches;
    private final ConsistencyStrategy<K, V> consistencyStrategy;
    private final CacheConfig config;
    private final CacheStats stats;
    private final String instanceId;

    /**
     * 创建多级缓存
     *
     * @param name                缓存名称
     * @param caches              缓存列表，顺序从L1到Ln
     * @param consistencyStrategy 一致性策略
     * @param config              缓存配置
     * @param instanceId          实例ID
     */
    public MultiLevelCache(String name, List<Cache<K, V>> caches,
            ConsistencyStrategy<K, V> consistencyStrategy, CacheConfig config, String instanceId) {
        if (caches == null || caches.isEmpty() || caches.size() < 2) {
            throw new IllegalArgumentException("缓存列表必须包含至少两个缓存实例");
        }
        
        this.name = name;
        this.caches = new ArrayList<>(caches);
        this.consistencyStrategy = consistencyStrategy;
        this.config = config;
        this.stats = new CacheStats(name);
        this.instanceId = instanceId;
    }

    @Override
    public V get(K key) {
        try {
            V value = consistencyStrategy.get(key);
            if (value != null) {
                stats.recordHit();
            } else {
                stats.recordMiss();
            }
            return value;
        } catch (Exception e) {
            log.error("多级缓存获取值异常, cacheName={}, key={}", name, key, e);
            stats.recordMiss();
            return null;
        }
    }

    @Override
    public void put(K key, V value) {
        put(key, value, config.getExpire());
    }

    @Override
    public void put(K key, V value, Duration ttl) {
        try {
            consistencyStrategy.put(key, value, ttl);
        } catch (Exception e) {
            log.error("多级缓存存储值异常, cacheName={}, key={}", name, key, e);
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        if (map == null || map.isEmpty()) {
            return;
        }

        try {
            consistencyStrategy.putAll(map);
        } catch (Exception e) {
            log.error("多级缓存批量存储值异常, cacheName={}", name, e);
        }
    }

    @Override
    public V computeIfAbsent(K key, Function<K, V> loader) {
        return computeIfAbsent(key, loader, config.getExpire());
    }

    @Override
    public V computeIfAbsent(K key, Function<K, V> loader, Duration ttl) {
        try {
            V value = consistencyStrategy.computeIfAbsent(key, loader, ttl);
            if (value != null) {
                stats.recordHit();
            } else {
                stats.recordMiss();
            }
            return value;
        } catch (Exception e) {
            stats.recordLoadFailure();
            log.error("多级缓存加载值异常, cacheName={}, key={}", name, key, e);
            throw e;
        }
    }

    @Override
    public boolean remove(K key) {
        try {
            return consistencyStrategy.remove(key);
        } catch (Exception e) {
            log.error("多级缓存移除值异常, cacheName={}, key={}", name, key, e);
            return false;
        }
    }

    @Override
    public void clear() {
        try {
            consistencyStrategy.clear();
        } catch (Exception e) {
            log.error("多级缓存清空异常, cacheName={}", name, e);
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

    @Override
    public boolean tryLock(K key, Duration timeout) {
        // 委托给最高层级的缓存实现分布式锁（通常是远程缓存）
        return caches.get(caches.size() - 1).tryLock(key, timeout);
    }

    @Override
    public void unlock(K key) {
        // 委托给最高层级的缓存实现分布式锁（通常是远程缓存）
        caches.get(caches.size() - 1).unlock(key);
    }

    @Override
    public boolean tryLockAndRun(K key, Duration timeout, Runnable action) {
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
    
    /**
     * 处理缓存更新事件
     *
     * @param event 缓存更新事件
     */
    public void handleCacheUpdate(CacheUpdateEvent event) {
        if (event == null || event.getInstanceId().equals(instanceId)) {
            return; // 忽略自己发出的事件
        }
        
        if (event.getCacheName().equals(name)) {
            try {
                consistencyStrategy.handleCacheUpdate(event);
            } catch (Exception e) {
                log.error("处理缓存更新事件异常, cacheName={}, eventType={}", name, event.getType(), e);
            }
        }
    }
    
    /**
     * 获取缓存列表
     *
     * @return 缓存列表
     */
    public List<Cache<K, V>> getCaches() {
        return new ArrayList<>(caches);
    }
    
    /**
     * 获取实例ID
     *
     * @return 实例ID
     */
    public String getInstanceId() {
        return instanceId;
    }
}