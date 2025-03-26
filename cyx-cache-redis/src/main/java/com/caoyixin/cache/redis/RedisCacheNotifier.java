package com.caoyixin.cache.redis;

import com.caoyixin.cache.notification.CacheEvent;
import com.caoyixin.cache.notification.CacheEventListener;
import com.caoyixin.cache.notification.CacheEventType;
import com.caoyixin.cache.notification.CacheNotifier;
import com.caoyixin.cache.notification.CacheRemoveEvent;
import com.caoyixin.cache.notification.CacheUpdateEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 基于Redis的缓存通知实现
 */
@Slf4j
public class RedisCacheNotifier implements CacheNotifier {

    private final RedisTemplate<String, String> redisTemplate;
    private final String topicPrefix;
    private final String instanceId;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, ConcurrentMap<CacheEventListener, Boolean>> listeners;
    private final RedisMessageListener messageListener;

    /**
     * 创建Redis缓存通知器
     *
     * @param redisTemplate     Redis模板
     * @param messageListener   Redis消息监听器
     * @param topicPrefix       主题前缀
     */
    public RedisCacheNotifier(RedisTemplate<String, String> redisTemplate, 
                            RedisMessageListener messageListener,
                            String topicPrefix) {
        this.redisTemplate = redisTemplate;
        this.messageListener = messageListener;
        this.topicPrefix = topicPrefix != null ? topicPrefix : "cyx-cache";
        this.instanceId = UUID.randomUUID().toString();
        this.objectMapper = new ObjectMapper();
        this.listeners = new ConcurrentHashMap<>();
        
        log.info("初始化RedisCacheNotifier, instanceId={}, topicPrefix={}", instanceId, this.topicPrefix);
    }

    @Override
    public void notifyUpdate(String cacheName, Object key) {
        publish(CacheEventType.UPDATE, cacheName, key);
    }

    @Override
    public void notifyAdd(String cacheName, Object key) {
        publish(CacheEventType.PUT, cacheName, key);
    }
    
    @Override
    public void notifyRemove(String cacheName, Object key) {
        publish(CacheEventType.REMOVE, cacheName, key);
        
        // 如果key为null，表示清空整个缓存
        if (key == null) {
            publish(CacheEventType.CLEAR, cacheName, null);
        }
    }

    @Override
    public void subscribe(String cacheName, CacheEventListener listener) {
        if (cacheName == null || listener == null) {
            return;
        }
        
        // 向Redis订阅消息
        messageListener.subscribe(cacheName);
        
        // 本地注册监听器
        ConcurrentMap<CacheEventListener, Boolean> cacheListeners = 
            listeners.computeIfAbsent(cacheName, k -> new ConcurrentHashMap<>());
        cacheListeners.put(listener, Boolean.TRUE);
        
        log.info("订阅缓存事件: cacheName={}", cacheName);
    }
    
    /**
     * 分发缓存事件到本地监听器
     *
     * @param event 缓存事件
     */
    public void dispatchEvent(CacheEvent event) {
        if (event == null || event.getInstanceId().equals(instanceId)) {
            return; // 忽略自己发出的事件
        }
        
        String cacheName = event.getCacheName();
        ConcurrentMap<CacheEventListener, Boolean> cacheListeners = listeners.get(cacheName);
        if (cacheListeners != null) {
            for (CacheEventListener listener : cacheListeners.keySet()) {
                try {
                    listener.onEvent(event);
                } catch (Exception e) {
                    log.error("处理缓存事件异常, cacheName={}, eventType={}", 
                            cacheName, event.getEventType(), e);
                }
            }
        }
    }

    /**
     * 发布缓存消息
     *
     * @param eventType 事件类型
     * @param cacheName 缓存名称
     * @param key       缓存键
     */
    private void publish(CacheEventType eventType, String cacheName, Object key) {
        if (cacheName == null) {
            return;
        }
        
        // 创建适当的缓存事件
        CacheEvent event;
        if (eventType == CacheEventType.REMOVE || eventType == CacheEventType.CLEAR) {
            event = new CacheRemoveEvent(cacheName, key, instanceId);
        } else {
            event = new CacheUpdateEvent(cacheName, key, instanceId);
        }

        String topic = buildTopic(cacheName);

        try {
            String message = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(topic, message);
            log.debug("发布缓存消息: topic={}, eventType={}, cacheName={}, key={}",
                    topic, eventType, cacheName, key);
        } catch (JsonProcessingException e) {
            log.error("序列化缓存事件失败", e);
        } catch (Exception e) {
            log.error("发布Redis消息失败", e);
        }
    }

    /**
     * 构建主题名称
     *
     * @param cacheName 缓存名称
     * @return 主题名称
     */
    private String buildTopic(String cacheName) {
        return topicPrefix + ":topic:" + cacheName;
    }
}