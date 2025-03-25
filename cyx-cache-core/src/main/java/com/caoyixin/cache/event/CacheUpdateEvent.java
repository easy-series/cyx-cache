package com.caoyixin.cache.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 缓存更新事件
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheUpdateEvent implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 事件类型
     */
    private CacheEventType type;
    
    /**
     * 缓存名称
     */
    private String cacheName;
    
    /**
     * 缓存键
     */
    private Object key;
    
    /**
     * 实例ID，用于识别事件来源
     */
    private String instanceId;
    
    /**
     * 创建一个缓存更新事件
     * 
     * @param type 事件类型
     * @param cacheName 缓存名称
     * @param key 缓存键
     * @param instanceId 实例ID
     * @return 缓存更新事件
     */
    public static CacheUpdateEvent of(CacheEventType type, String cacheName, Object key, String instanceId) {
        return new CacheUpdateEvent(type, cacheName, key, instanceId);
    }
}