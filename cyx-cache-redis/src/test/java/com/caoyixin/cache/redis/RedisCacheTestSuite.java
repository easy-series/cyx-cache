package com.caoyixin.cache.redis;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Redis缓存测试套件
 * 
 * 使用方法：运行此类可以一次执行所有测试
 * 请确保本地Redis服务已启动，密码设置为123456
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        RedisCacheTest.class,
        RedisDistributedLockTest.class,
        MultiLevelCacheTest.class,
        CacheNotificationTest.class
})
public class RedisCacheTestSuite {
    // 测试套件类不需要具体实现
}