package com.caoyixin.cache.redis;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

/**
 * Redis测试配置类
 */
public class RedisTestConfig {

    // Redis连接配置
    private static final String REDIS_HOST = "localhost";
    private static final int REDIS_PORT = 6379;
    private static final String REDIS_PASSWORD = "123456";

    // 测试使用的键前缀
    private static final String TEST_KEY_PREFIX = "cyx-cache-test:";

    /**
     * 创建Redis连接工厂
     */
    public static RedisConnectionFactory createConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(REDIS_HOST);
        redisConfig.setPort(REDIS_PORT);
        redisConfig.setPassword(RedisPassword.of(REDIS_PASSWORD));

        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfig);
        factory.afterPropertiesSet();
        return factory;
    }

    /**
     * 获取测试用的键前缀
     */
    public static String getTestKeyPrefix() {
        return TEST_KEY_PREFIX;
    }

    /**
     * 清理测试环境
     */
    public static void cleanTestEnvironment(RedisConnectionFactory connectionFactory) {
        // 可以添加清理测试数据的代码，例如删除所有以TEST_KEY_PREFIX开头的键
    }
}