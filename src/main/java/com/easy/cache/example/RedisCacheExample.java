package com.easy.cache.example;

import java.util.concurrent.TimeUnit;

import com.easy.cache.core.Cache;
import com.easy.cache.core.CacheBuilder;
import com.easy.cache.core.CacheManager;
import com.easy.cache.support.JdkSerializer;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Redis缓存使用示例
 */
public class RedisCacheExample {

    public static void main(String[] args) {
        // 初始化Redis连接池
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);

        JedisPool jedisPool = new JedisPool(poolConfig, "localhost", 6379);

        // 设置缓存管理器
        CacheManager cacheManager = CacheManager.getInstance();
        cacheManager.setJedisPool(jedisPool);
        cacheManager.setSerializer(new JdkSerializer());

        try {
            // 使用Builder API创建Redis缓存
            Cache<String, User> userCache = CacheBuilder.<String, User>newBuilder()
                    .name("userCache")
                    .expireAfterWrite(10, TimeUnit.MINUTES)
                    .useRedis()
                    .build();

            // 放入缓存
            User user1 = new User("1", "张三");
            userCache.put("user:1", user1);
            System.out.println("已将用户放入Redis缓存: " + user1);

            // 从缓存获取
            User cachedUser = userCache.get("user:1");
            System.out.println("从Redis缓存获取用户: " + cachedUser);

            // 使用加载器获取缓存
            User user2 = userCache.get("user:2", k -> {
                System.out.println("从数据库加载用户: " + k);
                return new User("2", "李四");
            });
            System.out.println("从Redis缓存获取用户: " + user2);

            // 再次获取，应该直接从缓存返回
            User cachedUser2 = userCache.get("user:2");
            System.out.println("再次从Redis缓存获取用户: " + cachedUser2);

            // 移除缓存
            userCache.remove("user:1");
            System.out.println("已从Redis缓存移除用户1");

            // 验证移除后获取
            User removedUser = userCache.get("user:1");
            System.out.println("移除后获取用户1: " + removedUser);

        } finally {
            // 关闭连接池
            jedisPool.close();
        }
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