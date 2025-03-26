package com.caoyixin.cache.multilevel;

import com.caoyixin.cache.api.AbstractCache;
import com.caoyixin.cache.api.Cache;
import com.caoyixin.cache.api.ConsistencyStrategy;
import com.caoyixin.cache.api.DistributedLock;
import com.caoyixin.cache.config.CacheConfig;
import com.caoyixin.cache.notification.CacheEvent;
import com.caoyixin.cache.notification.CacheNotifier;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * 多级缓存实现
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
@Slf4j
public class MultiLevelCache<K, V> extends AbstractCache<K, V> {

    private final List<Cache<K, V>> caches;
    private final ConsistencyStrategy<K, V> consistencyStrategy;
    private final CacheConfig config;
    private final String instanceId;
    private final DistributedLock<K> distributedLock;
    private final CacheNotifier notifier;

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
            DistributedLock<K> distributedLock,
            CacheNotifier cacheNotifier) {
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
        this.notifier = cacheNotifier;
    }

    @Override
    protected V doGet(K key) {
        return consistencyStrategy.get(key);
    }

    @Override
    protected void doPut(K key, V value, Duration ttl) {
        consistencyStrategy.put(key, value, ttl);

        notifier.notifyAdd(getName(), key);
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
        boolean remove = consistencyStrategy.remove(key);
        if (remove) {
            notifier.notifyRemove(getName(), key);
        }
        return remove;
    }

    @Override
    protected void doClear() {
        consistencyStrategy.clear();
        notifier.notifyRemove(getName(), null);
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

}