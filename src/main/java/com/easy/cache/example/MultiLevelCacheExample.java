package com.easy.cache.example;

import java.util.concurrent.TimeUnit;

import com.easy.cache.core.Cache;
import com.easy.cache.core.CacheBuilder;
import com.easy.cache.core.CacheManager;
import com.easy.cache.core.MultiLevelCache;
import com.easy.cache.support.JdkSerializer;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * 多级缓存使用示例
 */
public class MultiLevelCacheExample {

    public static void main(String[] args) {
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
            // 示例1：使用二级缓存（本地缓存 + Redis缓存）
            twoLevelCacheExample();

            // 示例2：使用自定义多级缓存
            multiLevelCacheExample();

        } finally {
            // 关闭连接池
            jedisPool.close();
        }
    }

    /**
     * 二级缓存示例（本地缓存 + Redis缓存）
     */
    private static void twoLevelCacheExample() {
        System.out.println("\n===== 二级缓存示例 =====");

        // 创建二级缓存
        Cache<String, User> userCache = CacheBuilder.<String, User>newBuilder()
                .name("userTwoLevelCache")
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .useTwoLevel()
                .writeThrough(true) // 写透模式
                .asyncWrite(true) // 异步写入Redis
                .build();

        // 放入缓存
        User user1 = new User("1", "张三");
        userCache.put("user:1", user1);
        System.out.println("已将用户放入二级缓存: " + user1);

        // 从缓存获取（应该从本地缓存获取）
        User cachedUser = userCache.get("user:1");
        System.out.println("从二级缓存获取用户: " + cachedUser);

        // 获取缓存层信息
        MultiLevelCache<String, User> multiLevelCache = (MultiLevelCache<String, User>) userCache;
        System.out.println("缓存层数: " + multiLevelCache.getLevelCount());

        // 使用加载器获取缓存
        User user2 = userCache.get("user:2", k -> {
            System.out.println("从数据库加载用户: " + k);
            return new User("2", "李四");
        });
        System.out.println("从二级缓存获取用户: " + user2);

        // 再次获取，应该直接从本地缓存返回
        User cachedUser2 = userCache.get("user:2");
        System.out.println("再次从二级缓存获取用户: " + cachedUser2);
    }

    /**
     * 自定义多级缓存示例
     */
    private static void multiLevelCacheExample() {
        System.out.println("\n===== 自定义多级缓存示例 =====");

        // 创建各级缓存
        Cache<String, User> localCache1 = CacheBuilder.<String, User>newBuilder()
                .name("localCache1")
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .buildLocalCache();

        Cache<String, User> localCache2 = CacheBuilder.<String, User>newBuilder()
                .name("localCache2")
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .buildLocalCache();

        Cache<String, User> redisCache = CacheBuilder.<String, User>newBuilder()
                .name("redisCache")
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .buildRedisCache();

        // 创建多级缓存
        Cache<String, User> userCache = CacheBuilder.<String, User>newBuilder()
                .name("userMultiLevelCache")
                .useMultiLevel()
                .addCache(localCache1) // 一级缓存（最高优先级）
                .addCache(localCache2) // 二级缓存
                .addCache(redisCache) // 三级缓存（最低优先级）
                .writeThrough(true)
                .asyncWrite(true)
                .build();

        // 放入缓存
        User user1 = new User("1", "张三");
        userCache.put("user:1", user1);
        System.out.println("已将用户放入多级缓存: " + user1);

        // 从缓存获取（应该从一级缓存获取）
        User cachedUser = userCache.get("user:1");
        System.out.println("从多级缓存获取用户: " + cachedUser);

        // 模拟一级缓存过期，从二级缓存获取
        localCache1.clear();
        System.out.println("一级缓存已清空");

        User cachedUser2 = userCache.get("user:1");
        System.out.println("从多级缓存获取用户（一级缓存已清空）: " + cachedUser2);

        // 模拟一级和二级缓存过期，从三级缓存获取
        localCache1.clear();
        localCache2.clear();
        System.out.println("一级和二级缓存已清空");

        User cachedUser3 = userCache.get("user:1");
        System.out.println("从多级缓存获取用户（一级和二级缓存已清空）: " + cachedUser3);

        // 再次获取，应该从一级缓存获取（因为回填机制）
        User cachedUser4 = userCache.get("user:1");
        System.out.println("再次从多级缓存获取用户: " + cachedUser4);
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