package com.caoyixin.cache.notification;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 缓存事件
 */
@Getter
@ToString
@NoArgsConstructor // 添加无参构造函数，用于Jackson反序列化
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CacheUpdateEvent.class, name = "UPDATE"),
        @JsonSubTypes.Type(value = CacheRemoveEvent.class, name = "REMOVE")
})
public abstract class CacheEvent {
    /**
     * 缓存名称
     */
    private String cacheName;

    /**
     * 缓存键
     */
    private Object key;

    /**
     * 事件类型
     */
    private CacheEventType eventType;

    /**
     * 实例ID，用于识别事件来源
     */
    private String instanceId;

    /**
     * 构造函数
     *
     * @param cacheName  缓存名称
     * @param key        缓存键
     * @param eventType  事件类型
     * @param instanceId 实例ID
     */
    @JsonCreator
    protected CacheEvent(
            @JsonProperty("cacheName") String cacheName,
            @JsonProperty("key") Object key,
            @JsonProperty("eventType") CacheEventType eventType,
            @JsonProperty("instanceId") String instanceId) {
        this.cacheName = cacheName;
        this.key = key;
        this.eventType = eventType;
        this.instanceId = instanceId;
    }
}