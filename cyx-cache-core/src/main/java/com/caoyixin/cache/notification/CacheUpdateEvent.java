package com.caoyixin.cache.notification;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NoArgsConstructor;

/**
 * 缓存更新事件
 */
@NoArgsConstructor
public class CacheUpdateEvent extends CacheEvent {
    /**
     * 构造函数
     *
     * @param cacheName  缓存名称
     * @param key        缓存键
     * @param instanceId 实例ID
     */
    @JsonCreator
    public CacheUpdateEvent(
            @JsonProperty("cacheName") String cacheName,
            @JsonProperty("key") Object key,
            @JsonProperty("instanceId") String instanceId) {
        super(cacheName, key, CacheEventType.UPDATE, instanceId);
    }
}