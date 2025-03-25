package com.caoyixin.cache.redis;

import com.caoyixin.cache.notification.CacheEvent;
import com.caoyixin.cache.notification.CacheEventType;
import com.caoyixin.cache.notification.CacheRemoveEvent;
import com.caoyixin.cache.notification.CacheUpdateEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Redis缓存消息发布器
 */
@Slf4j
public class RedisMessagePublisher {

    private final RedisTemplate<String, String> redisTemplate;
    private final String topicPrefix;
    private final String instanceId;
    private final ObjectMapper objectMapper;

    /**
     * 创建Redis消息发布器
     *
     * @param redisTemplate Redis模板
     * @param topicPrefix   主题前缀
     * @param instanceId    实例ID
     */
    public RedisMessagePublisher(RedisTemplate<String, String> redisTemplate, String topicPrefix, String instanceId) {
        this.redisTemplate = redisTemplate;
        this.topicPrefix = topicPrefix != null ? topicPrefix : "cyx-cache";
        this.instanceId = instanceId;
        this.objectMapper = new ObjectMapper();
        log.info("初始化RedisMessagePublisher, instanceId={}, topicPrefix={}", instanceId, this.topicPrefix);
    }

    /**
     * 发布缓存更新消息
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     */
    public void publishUpdate(String cacheName, Object key) {
        publish(CacheEventType.UPDATE, cacheName, key);
    }

    /**
     * 发布缓存删除消息
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     */
    public void publishRemove(String cacheName, Object key) {
        publish(CacheEventType.REMOVE, cacheName, key);
    }

    /**
     * 发布缓存清空消息
     *
     * @param cacheName 缓存名称
     */
    public void publishClear(String cacheName) {
        publish(CacheEventType.CLEAR, cacheName, null);
    }

    /**
     * 发布缓存消息
     *
     * @param eventType 事件类型
     * @param cacheName 缓存名称
     * @param key       缓存键
     */
    private void publish(CacheEventType eventType, String cacheName, Object key) {
        // 创建适当的缓存事件
        CacheEvent event;
        if (eventType == CacheEventType.REMOVE) {
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