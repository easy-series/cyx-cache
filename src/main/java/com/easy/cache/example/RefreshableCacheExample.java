package com.easy.cache.example;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.easy.cache.core.Cache;
import com.easy.cache.core.CacheBuilder;
import com.easy.cache.core.CacheManager;
import com.easy.cache.core.RefreshableCache;
import com.easy.cache.support.JdkSerializer;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * 可自动刷新缓存使用示例
 */
public class RefreshableCacheExample {

    public static void main(String[] args) throws InterruptedException {
        // 初始化Redis连接池
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);

        JedisPool jedisPool = new JedisPool(poolConfig, "localhost", 6379);

        // 设置缓存管理器
        CacheManager cacheManager = CacheManager.getInstance();
        cacheManager.setJedisPool(jedisPool);
        cacheManager.setSerializer(new JdkSerializer());

        try {
            // 示例1：使用本地可刷新缓存
            localRefreshableCacheExample();

            // 示例2：使用Redis可刷新缓存
            redisRefreshableCacheExample();

            // 示例3：使用二级可刷新缓存
            twoLevelRefreshableCacheExample();

        } finally {
            // 关闭连接池
            jedisPool.close();
        }
    }

    /**
     * 本地可刷新缓存示例
     */
    private static void localRefreshableCacheExample() throws InterruptedException {
        System.out.println("\n===== 本地可刷新缓存示例 =====");

        // 创建可刷新缓存，每5秒刷新一次
        Cache<String, String> timeCache = CacheBuilder.<String, String>newBuilder()
                .name("timeCache")
                .refreshable()
                .refreshInterval(5, TimeUnit.SECONDS)
                .build();

        // 使用加载器获取缓存，同时注册自动刷新
        String time = timeCache.get("current_time", k -> {
            String currentTime = new Date().toString();
            System.out.println("初始加载时间: " + currentTime);
            return currentTime;
        });

        System.out.println("第一次获取时间: " + time);

        // 等待几秒，让自动刷新发生
        for (int i = 1; i <= 3; i++) {
            Thread.sleep(6000); // 等待6秒
            String refreshedTime = timeCache.get("current_time");
            System.out.println("第" + (i + 1) + "次获取时间: " + refreshedTime);
        }

        // 手动注册刷新器
        if (timeCache instanceof RefreshableCache) {
            RefreshableCache<String, String> refreshableCache = (RefreshableCache<String, String>) timeCache;
            refreshableCache.registerLoader("manual_time", k -> {
                String currentTime = "手动注册: " + new Date().toString();
                System.out.println("手动注册加载时间: " + currentTime);
                return currentTime;
            });
        }

        // 获取手动注册的缓存项
        String manualTime = timeCache.get("manual_time");
        System.out.println("手动注册的时间: " + manualTime);

        // 再等待几秒，查看手动注册的刷新效果
        Thread.sleep(6000);
        String refreshedManualTime = timeCache.get("manual_time");
        System.out.println("刷新后的手动注册时间: " + refreshedManualTime);
    }

    /**
     * Redis可刷新缓存示例
     */
    private static void redisRefreshableCacheExample() throws InterruptedException {
        System.out.println("\n===== Redis可刷新缓存示例 =====");

        // 创建可刷新Redis缓存，每10秒刷新一次
        Cache<String, User> userCache = CacheBuilder.<String, User>newBuilder()
                .name("userCache")
                .useRedis()
                .refreshable()
                .refreshInterval(10, TimeUnit.SECONDS)
                .build();

        // 使用加载器获取缓存，同时注册自动刷新
        User user = userCache.get("user:1", k -> {
            User newUser = new User("1", "张三 - " + new Date().getTime());
            System.out.println("初始加载用户: " + newUser);
            return newUser;
        });

        System.out.println("第一次获取用户: " + user);

        // 等待几秒，让自动刷新发生
        Thread.sleep(12000); // 等待12秒
        User refreshedUser = userCache.get("user:1");
        System.out.println("刷新后的用户: " + refreshedUser);
    }

    /**
     * 二级可刷新缓存示例
     */
    private static void twoLevelRefreshableCacheExample() throws InterruptedException {
        System.out.println("\n===== 二级可刷新缓存示例 =====");

        // 创建可刷新二级缓存，每8秒刷新一次
        Cache<String, User> userCache = CacheBuilder.<String, User>newBuilder()
                .name("userTwoLevelCache")
                .useTwoLevel()
                .writeThrough(true)
                .asyncWrite(true)
                .refreshable()
                .refreshInterval(8, TimeUnit.SECONDS)
                .build();

        // 使用加载器获取缓存，同时注册自动刷新
        User user = userCache.get("user:2", k -> {
            User newUser = new User("2", "李四 - " + new Date().getTime());
            System.out.println("初始加载用户: " + newUser);
            return newUser;
        });

        System.out.println("第一次获取用户: " + user);

        // 等待几秒，让自动刷新发生
        Thread.sleep(10000); // 等待10秒
        User refreshedUser = userCache.get("user:2");
        System.out.println("刷新后的用户: " + refreshedUser);
    }

    /**
     * 用户实体
     */
    public static class User implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        private String id;
        private String name;

        public User(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return "User{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    '}';
        }
    }
}