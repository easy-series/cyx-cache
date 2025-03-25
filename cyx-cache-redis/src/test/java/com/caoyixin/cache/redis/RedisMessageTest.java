package com.caoyixin.cache.redis;

import com.caoyixin.cache.multilevel.MultiLevelCacheManager;
import com.caoyixin.cache.notification.CacheEvent;
import com.caoyixin.cache.notification.CacheEventType;
import com.caoyixin.cache.notification.CacheUpdateEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class RedisMessageTest {

    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate stringRedisTemplate;
    private RedisMessageListenerContainer listenerContainer;
    private MultiLevelCacheManager cacheManager;
    private RedisMessagePublisher publisher;
    private RedisMessageListener listener;

    @BeforeEach
    public void setup() {
        // 配置Redis连接
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName("localhost"); // 使用本地Redis
        config.setPort(6379); // 默认端口
        config.setPassword("123456"); // 设置Redis密码
        // config.setPassword("your_password"); // 如果有密码，取消注释并设置

        connectionFactory = new LettuceConnectionFactory(config);
        connectionFactory.afterPropertiesSet();

        // 创建Redis模板
        stringRedisTemplate = new StringRedisTemplate(connectionFactory);
        stringRedisTemplate.afterPropertiesSet();

        // 创建消息监听容器
        listenerContainer = new RedisMessageListenerContainer();
        listenerContainer.setConnectionFactory(connectionFactory);
        listenerContainer.afterPropertiesSet();
        listenerContainer.start();

        // 创建缓存管理器（模拟对象）
        cacheManager = Mockito.mock(MultiLevelCacheManager.class);
        when(cacheManager.getInstanceId()).thenReturn("testInstance");

        // 创建消息发布器和监听器
        publisher = new RedisMessagePublisher(stringRedisTemplate, "test", "publisherInstance");
        listener = new RedisMessageListener(listenerContainer, stringRedisTemplate, cacheManager, "test",
                "listenerInstance");

        // 清理可能存在的测试主题数据
        stringRedisTemplate.delete("test:topic:testCache");
    }

    @Test
    public void testPublishAndReceiveMessage() throws InterruptedException {
        // 设置CountDownLatch以等待消息处理
        CountDownLatch latch = new CountDownLatch(1);

        // 模拟cacheManager行为
        doAnswer(invocation -> {
            CacheUpdateEvent event = invocation.getArgument(0);
            assertEquals(CacheEventType.PUT, event.getEventType());
            assertEquals("testCache", event.getCacheName());
            assertEquals("testKey", event.getKey());
            assertEquals("publisherInstance", event.getInstanceId());
            latch.countDown();
            return null;
        }).when(cacheManager).handleCacheUpdateEvent(any(CacheUpdateEvent.class));

        // 订阅主题
        listener.subscribe("testCache");

        // 发布消息
        publisher.publishUpdate("testCache", "testKey");

        // 等待消息处理
        assertTrue(latch.await(5, TimeUnit.SECONDS), "应该在超时前收到消息");

        // 验证cacheManager.handleCacheUpdateEvent被调用
        verify(cacheManager, timeout(5000).times(1)).handleCacheUpdateEvent(any(CacheUpdateEvent.class));

        // 取消订阅，清理环境
        listener.unsubscribe("testCache");
    }

    @Test
    public void testIgnoreSelfPublishedMessages() throws InterruptedException {
        // 当发布者和监听者使用相同的实例ID时，应该忽略消息
        RedisMessagePublisher sameInstancePublisher = new RedisMessagePublisher(
                stringRedisTemplate, "test", "listenerInstance");

        // 订阅主题
        listener.subscribe("testCache");

        // 发布消息
        sameInstancePublisher.publishUpdate("testCache", "testKey");

        // 验证cacheManager.handleCacheUpdateEvent没有被调用
        verify(cacheManager, after(1000).never()).handleCacheUpdateEvent(any(CacheUpdateEvent.class));

        // 取消订阅，清理环境
        listener.unsubscribe("testCache");
    }

    @Test
    public void testUnsubscribe() throws InterruptedException {
        // 订阅主题
        listener.subscribe("testCache");

        // 发布消息，应该收到
        publisher.publishUpdate("testCache", "testKey");
        verify(cacheManager, timeout(1000).times(1)).handleCacheUpdateEvent(any(CacheEvent.class));

        // 重置mock
        reset(cacheManager);

        // 取消订阅
        listener.unsubscribe("testCache");

        // 再次发布消息，应该不再收到
        publisher.publishUpdate("testCache", "testKey");
        verify(cacheManager, after(1000).never()).handleCacheUpdateEvent(any(CacheEvent.class));
    }
}