package com.easy.cache.example;

import java.util.concurrent.TimeUnit;

import com.easy.cache.core.Cache;
import com.easy.cache.core.CacheManager;
import com.easy.cache.core.QuickConfig;
import com.easy.cache.core.RedisCache.Serializer;
import com.easy.cache.support.JdkSerializer;
import com.easy.cache.sync.CacheSyncManager;
import com.easy.cache.sync.CacheSyncManager.SyncStrategy;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * 缓存同步示例
 */
public class CacheSyncExample {

    public static void main(String[] args) throws Exception {
        // 创建Jedis连接池
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        JedisPool jedisPool = new JedisPool(poolConfig, "localhost", 6379);

        // 创建序列化器
        Serializer serializer = new JdkSerializer();

        try {
            // 初始化缓存管理器
            CacheManager cacheManager = CacheManager.getInstance();
            cacheManager.setJedisPool(jedisPool);
            cacheManager.setSerializer(serializer);

            // 初始化缓存同步
            cacheManager.initCacheSync();

            // 示例1：失效模式（默认）
            invalidationModeExample(cacheManager);

            // 示例2：更新模式
            updateModeExample(cacheManager);

            // 示例3：异步写入和写透模式
            asyncWriteThroughExample(cacheManager);

            // 清理资源
            cacheManager.shutdown();
        } finally {
            jedisPool.close();
        }
    }

    /**
     * 失效模式示例：收到同步事件后，直接从本地缓存中删除对应的键
     */
    private static void invalidationModeExample(CacheManager cacheManager) throws Exception {
        System.out.println("\n===== 失效模式示例 =====");

        // 创建一个带本地缓存同步的二级缓存（使用默认的失效模式）
        QuickConfig config = QuickConfig.builder()
                .name("user-cache-invalidate")
                .cacheType(QuickConfig.CacheType.BOTH)
                .expire(30, TimeUnit.MINUTES)
                .writeThrough(true)
                .asyncWrite(false)
                .syncLocal(true) // 启用本地缓存同步
                .localLimit(100) // 设置本地缓存大小限制
                .build();

        Cache<String, User> cache = cacheManager.getOrCreateCache(config);

        // 在当前JVM中添加缓存
        User user1 = new User("1", "张三", 30);
        cache.put("user:1", user1);
        System.out.println("添加用户到缓存: " + cache.get("user:1"));

        // 模拟另一个JVM中的操作
        System.out.println("模拟另一个JVM中的操作...");

        // 直接通过Redis更新缓存值
        try (Jedis jedis = cacheManager.getJedisPool().getResource()) {
            // 序列化用户对象
            User updatedUser = new User("1", "张三(已更新)", 31);
            byte[] serializedUser = cacheManager.getSerializer().serialize(updatedUser);
            jedis.set(("user-cache-invalidate:redis:user:1").getBytes(), serializedUser);
        }

        // 等待同步完成
        Thread.sleep(1000);

        // 再次获取缓存值，应该会从Redis获取最新值
        System.out.println("同步后的缓存值: " + cache.get("user:1"));
    }

    /**
     * 更新模式示例：收到同步事件后，直接用事件中的新值更新本地缓存
     */
    private static void updateModeExample(CacheManager cacheManager) throws Exception {
        System.out.println("\n===== 更新模式示例 =====");

        // 设置更新模式
        CacheSyncManager.getInstance().setDefaultSyncStrategy(SyncStrategy.UPDATE);

        // 创建一个带本地缓存同步的二级缓存（使用更新模式）
        QuickConfig config = QuickConfig.builder()
                .name("user-cache-update")
                .cacheType(QuickConfig.CacheType.BOTH)
                .expire(30, TimeUnit.MINUTES)
                .writeThrough(true)
                .asyncWrite(false)
                .syncLocal(true) // 启用本地缓存同步
                .localLimit(100) // 设置本地缓存大小限制
                .build();

        Cache<String, User> cache = cacheManager.getOrCreateCache(config);

        // 为特定缓存启用更新模式
        CacheSyncManager.getInstance().enableSync("user-cache-update", true, SyncStrategy.UPDATE);

        // 在当前JVM中添加缓存
        User user1 = new User("1", "李四", 25);
        cache.put("user:1", user1);
        System.out.println("添加用户到缓存: " + cache.get("user:1"));

        // 模拟另一个JVM中的操作
        System.out.println("模拟另一个JVM中的操作...");

        // 直接通过Redis更新缓存值
        try (Jedis jedis = cacheManager.getJedisPool().getResource()) {
            // 序列化用户对象
            User updatedUser = new User("1", "李四(已更新)", 26);
            byte[] serializedUser = cacheManager.getSerializer().serialize(updatedUser);
            jedis.set(("user-cache-update:redis:user:1").getBytes(), serializedUser);

            // 模拟发布缓存更新事件
            // 在实际场景中，这是由另一个JVM实例自动完成的
            User eventUser = new User("1", "李四(已更新)", 26);
            cache.put("user:1", eventUser);
        }

        // 等待同步完成
        Thread.sleep(1000);

        // 再次获取缓存值，应该直接从本地缓存获取更新后的值
        System.out.println("同步后的缓存值: " + cache.get("user:1"));

        // 恢复默认策略
        CacheSyncManager.getInstance().setDefaultSyncStrategy(SyncStrategy.INVALIDATE);
    }

    /**
     * 异步写入和写透模式示例
     */
    private static void asyncWriteThroughExample(CacheManager cacheManager) throws Exception {
        System.out.println("\n===== 异步写入和写透模式示例 =====");

        // 创建一个带异步写入和写透的二级缓存
        QuickConfig config = QuickConfig.builder()
                .name("user-cache-async")
                .cacheType(QuickConfig.CacheType.BOTH)
                .expire(30, TimeUnit.MINUTES)
                .writeThrough(true) // 启用写透
                .asyncWrite(true) // 启用异步写入
                .syncLocal(true) // 启用本地缓存同步
                .localLimit(100) // 设置本地缓存大小限制
                .build();

        Cache<String, User> cache = cacheManager.getOrCreateCache(config);

        // 在当前JVM中添加缓存（异步写入到Redis）
        User user1 = new User("1", "王五", 35);
        cache.put("user:1", user1);
        System.out.println("异步添加用户到缓存: " + cache.get("user:1"));

        // 等待异步写入完成
        Thread.sleep(1000);

        // 验证Redis中是否有值
        try (Jedis jedis = cacheManager.getJedisPool().getResource()) {
            byte[] bytes = jedis.get(("user-cache-async:redis:user:1").getBytes());
            User redisUser = cacheManager.getSerializer().deserialize(bytes, User.class);
            System.out.println("Redis中的用户: " + redisUser);
        }

        // 测试批量操作
        System.out.println("\n批量添加用户...");
        for (int i = 2; i <= 5; i++) {
            User user = new User(String.valueOf(i), "用户" + i, 20 + i);
            cache.put("user:" + i, user);
            System.out.println("添加用户: " + user);
        }

        // 等待异步写入完成
        Thread.sleep(1000);

        // 测试移除操作
        System.out.println("\n移除用户...");
        cache.remove("user:3");
        System.out.println("已移除用户3");

        // 等待异步操作完成
        Thread.sleep(1000);

        // 验证用户3是否已被移除
        System.out.println("用户3: " + cache.get("user:3"));

        // 测试清空操作
        System.out.println("\n清空缓存...");
        cache.clear();
        System.out.println("已清空缓存");

        // 等待异步操作完成
        Thread.sleep(1000);

        // 验证缓存是否已清空
        System.out.println("用户1: " + cache.get("user:1"));
        System.out.println("用户2: " + cache.get("user:2"));
    }

    /**
     * 用户类
     */
    public static class User implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        private String id;
        private String name;
        private int age;

        public User() {
        }

        public User(String id, String name, int age) {
            this.id = id;
            this.name = name;
            this.age = age;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        @Override
        public String toString() {
            return "User{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", age=" + age +
                    '}';
        }
    }
}