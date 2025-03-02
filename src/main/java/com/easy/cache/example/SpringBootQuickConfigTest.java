package com.easy.cache.example;

import java.util.concurrent.TimeUnit;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.easy.cache.annotation.Cached;
import com.easy.cache.core.Cache;
import com.easy.cache.core.CacheManager;
import com.easy.cache.core.QuickConfig;
import com.easy.cache.core.RefreshableCache;

/**
 * Spring Boot QuickConfig 测试类
 */
@SpringBootApplication
@ComponentScan("com.easy.cache")
public class SpringBootQuickConfigTest {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(SpringBootQuickConfigTest.class);
        app.setAddCommandLineProperties(true);
        app.setAdditionalProfiles("test");
        app.setDefaultProperties(
                java.util.Collections.singletonMap("spring.main.allow-bean-definition-overriding", "true"));
        app.run(args);
    }

    @Bean
    public CacheManager cacheManager() {
        return CacheManager.getInstance();
    }

    /**
     * QuickConfig 测试组件
     */
    @Component
    public static class QuickConfigTester implements CommandLineRunner {

        private final CacheManager cacheManager;
        private final UserService userService;

        public QuickConfigTester(CacheManager cacheManager, UserService userService) {
            this.cacheManager = cacheManager;
            this.userService = userService;
        }

        @Override
        public void run(String... args) throws Exception {
            System.out.println("\n===== 开始测试 QuickConfig =====");

            // 测试本地缓存
            testLocalCache();

            // 测试可刷新缓存
            testRefreshableCache();

            // 测试注解
            testAnnotation();

            System.out.println("\n===== 测试完成 =====");
        }

        /**
         * 测试本地缓存
         */
        private void testLocalCache() {
            System.out.println("\n----- 测试本地缓存 -----");

            // 创建本地缓存配置
            QuickConfig config = QuickConfig.builder()
                    .name("userCache")
                    .expire(5, TimeUnit.MINUTES)
                    .cacheType(QuickConfig.CacheType.LOCAL)
                    .cacheNull(false)
                    .build();

            // 获取或创建缓存
            Cache<String, User> userCache = cacheManager.getOrCreateCache(config);

            // 测试缓存操作
            String key = "user:1001";
            User user = new User("1001", "张三", 30);

            // 放入缓存
            userCache.put(key, user);
            System.out.println("已将用户放入缓存: " + user);

            // 从缓存获取
            User cachedUser = userCache.get(key);
            System.out.println("从缓存获取用户: " + cachedUser);

            // 使用加载器获取不存在的用户
            User newUser = userCache.get("user:1002", k -> {
                System.out.println("从数据库加载用户: " + k);
                return new User("1002", "李四", 25);
            });
            System.out.println("使用加载器获取用户: " + newUser);

            // 再次获取，应该直接从缓存返回
            User cachedUser2 = userCache.get("user:1002");
            System.out.println("再次从缓存获取用户: " + cachedUser2);
        }

        /**
         * 测试可刷新缓存
         */
        private void testRefreshableCache() {
            System.out.println("\n----- 测试可刷新缓存 -----");

            // 创建可刷新缓存配置
            QuickConfig config = QuickConfig.builder()
                    .name("stockCache")
                    .expire(1, TimeUnit.MINUTES)
                    .cacheType(QuickConfig.CacheType.LOCAL)
                    .refreshable(true)
                    .refreshInterval(3, TimeUnit.SECONDS)
                    .build();

            // 获取或创建可刷新缓存
            Cache<String, Integer> cache = cacheManager.getOrCreateCache(config);
            // 由于registerLoader方法在RefreshableCache中，需要进行类型转换
            RefreshableCache<String, Integer> stockCache = (RefreshableCache<String, Integer>) cache;

            // 初始库存
            String productId = "P10086";
            stockCache.put(productId, 100);
            System.out.println("初始库存: " + stockCache.get(productId));

            // 注册刷新函数，模拟从数据库获取最新库存
            stockCache.registerLoader(productId, k -> {
                // 模拟数据库操作，每次返回递减的库存
                int currentStock = stockCache.get(k);
                int newStock = Math.max(0, currentStock - 10);
                System.out.println("刷新库存: " + k + " -> " + newStock);
                return newStock;
            });

            // 等待自动刷新
            try {
                System.out.println("等待自动刷新...");
                for (int i = 0; i < 3; i++) {
                    Thread.sleep(3500);
                    System.out.println("当前库存: " + stockCache.get(productId));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        /**
         * 测试注解
         */
        private void testAnnotation() {
            System.out.println("\n----- 测试注解 -----");

            // 测试基本缓存注解
            String userId = "U10010";
            System.out.println("第一次调用 getUserById:");
            User user1 = userService.getUserById(userId);
            System.out.println("返回结果: " + user1);

            System.out.println("第二次调用 getUserById (应该从缓存获取):");
            User user2 = userService.getUserById(userId);
            System.out.println("返回结果: " + user2);

            // 测试可刷新缓存注解
            System.out.println("\n测试可刷新缓存注解:");
            String refreshUserId = "U10086";
            System.out.println("第一次调用 getRefreshableUser:");
            User refreshUser1 = userService.getRefreshableUser(refreshUserId);
            System.out.println("返回结果: " + refreshUser1);

            try {
                System.out.println("等待自动刷新...");
                Thread.sleep(3500);
                System.out.println("第二次调用 getRefreshableUser (应该触发自动刷新):");
                User refreshUser2 = userService.getRefreshableUser(refreshUserId);
                System.out.println("返回结果: " + refreshUser2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 用户服务
     */
    @Service
    public static class UserService {

        /**
         * 使用基本缓存注解
         */
        @Cached(key = "user:#{#p0}", expire = 30, timeUnit = TimeUnit.MINUTES)
        public User getUserById(String id) {
            System.out.println("从数据库加载用户: " + id);
            return new User(id, "用户" + id, 20 + id.hashCode() % 50);
        }

        /**
         * 使用可刷新缓存注解
         */
        @Cached(key = "refreshUser:#{#p0}", expire = 10, timeUnit = TimeUnit.MINUTES, refresh = true, refreshInterval = 3, refreshTimeUnit = TimeUnit.SECONDS)
        public User getRefreshableUser(String id) {
            System.out.println("从数据库刷新用户: " + id);
            return new User(id, "刷新用户" + id, 20 + (int) (System.currentTimeMillis() % 100));
        }
    }

    /**
     * 用户实体
     */
    public static class User {
        private String id;
        private String name;
        private int age;

        public User(String id, String name, int age) {
            this.id = id;
            this.name = name;
            this.age = age;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
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