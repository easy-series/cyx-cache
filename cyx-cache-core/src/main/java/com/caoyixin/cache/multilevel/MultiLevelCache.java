package com.caoyixin.cache.multilevel;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.caoyixin.cache.api.AbstractCache;
import com.caoyixin.cache.api.AdvancedCache;
import com.caoyixin.cache.api.Cache;
import com.caoyixin.cache.api.DistributedLock;
import com.caoyixin.cache.config.CacheConfig;
import com.caoyixin.cache.consistency.ConsistencyStrategy;
import com.caoyixin.cache.notification.CacheEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * 多级缓存实现
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
@Slf4j
public class MultiLevelCache<K, V> extends AbstractCache<K, V> implements AdvancedCache<K, V> {

    private final List<Cache<K, V>> caches;
    private final ConsistencyStrategy<K, V> consistencyStrategy;
    private final CacheConfig config;
    private final String instanceId;
    private final DistributedLock<K> distributedLock;

    /**
     * 创建多级缓存
     *
     * @param name                缓存名称
     * @param caches              缓存列表，顺序从L1到Ln
     * @param consistencyStrategy 一致性策略
     * @param config              缓存配置
     * @param distributedLock     分布式锁
     */
    public MultiLevelCache(
            String name,
            List<Cache<K, V>> caches,
            ConsistencyStrategy<K, V> consistencyStrategy,
            CacheConfig config,
            DistributedLock<K> distributedLock) {
        super(name);

        if (caches == null || caches.isEmpty() || caches.size() < 2) {
            throw new IllegalArgumentException("缓存列表必须包含至少两个缓存实例");
        }

        this.caches = new ArrayList<>(caches);
        this.consistencyStrategy = consistencyStrategy;
        this.consistencyStrategy.initialize(this.caches);
        this.config = config;
        this.instanceId = UUID.randomUUID().toString();
        this.distributedLock = distributedLock;
    }

    @Override
    protected V doGet(K key) {
        return consistencyStrategy.get(key);
    }

    @Override
    protected void doPut(K key, V value, Duration ttl) {
        consistencyStrategy.put(key, value, ttl);
    }

    @Override
    protected void doPutAll(Map<? extends K, ? extends V> map) {
        consistencyStrategy.putAll(map);
    }

    @Override
    protected V doComputeIfAbsent(K key, Function<K, V> loader, Duration ttl) {
        return consistencyStrategy.computeIfAbsent(key, loader, ttl);
    }

    @Override
    protected boolean doRemove(K key) {
        return consistencyStrategy.remove(key);
    }

    @Override
    protected void doClear() {
        consistencyStrategy.clear();
    }

    @Override
    public CompletableFuture<V> getAsync(K key) {
        return CompletableFuture.supplyAsync(() -> get(key));
    }

    @Override
    public DistributedLock<K> getDistributedLock() {
        return distributedLock;
    }

    @Override
    public void preload(Function<String, Iterable<K>> loader) {
        if (loader == null) {
            return;
        }

        Iterable<K> keys = loader.apply(getName());
        if (keys == null) {
            return;
        }

        for (K key : keys) {
            try {
                get(key);
            } catch (Exception e) {
                log.warn("预加载缓存键失败: {}", key, e);
            }
        }
    }

    @Override
    public long getExpireTime(K key) {
        // 获取最后一个缓存（通常是远程缓存）的过期时间
        Cache<K, V> lastCache = caches.get(caches.size() - 1);
        if (lastCache instanceof AdvancedCache) {
            return ((AdvancedCache<K, V>) lastCache).getExpireTime(key);
        }
        return -1;
    }

    /**
     * 处理缓存更新事件
     *
     * @param event 缓存事件
     */
    public void handleCacheUpdate(CacheEvent event) {
        if (event == null || event.getInstanceId().equals(instanceId)) {
            return; // 忽略自己发出的事件
        }

        if (event.getCacheName().equals(getName())) {
            try {
                consistencyStrategy.handleCacheUpdate(event);
            } catch (Exception e) {
                log.error("处理缓存更新事件异常, cacheName={}, eventType={}", getName(), event.getEventType(), e);
            }
        }
    }

    /**
     * 获取实例ID
     *
     * @return 实例ID
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * 获取缓存列表
     *
     * @return 缓存列表
     */
    public List<Cache<K, V>> getCaches() {
        return new ArrayList<>(caches);
    }
}