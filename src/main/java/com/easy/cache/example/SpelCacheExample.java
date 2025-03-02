package com.easy.cache.example;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import com.easy.cache.annotation.CacheInterceptor;
import com.easy.cache.annotation.Cached;
import com.easy.cache.annotation.Cached.CacheType;
import com.easy.cache.support.SpelKeyGenerator;

/**
 * SpEL表达式缓存示例
 */
public class SpelCacheExample {

    public static void main(String[] args) throws Exception {
        UserService userService = new UserService();
        OrderService orderService = new OrderService();

        // 创建缓存拦截器
        CacheInterceptor interceptor = new CacheInterceptor();

        // 基本SpEL表达式示例
        basicSpelExample(userService, interceptor);

        // 复杂SpEL表达式示例
        complexSpelExample(userService, interceptor);

        // 自定义SpEL键生成器示例
        customSpelKeyGeneratorExample(orderService);
    }

    /**
     * 基本SpEL表达式示例
     */
    private static void basicSpelExample(UserService userService, CacheInterceptor interceptor) throws Exception {
        System.out.println("\n=== 基本SpEL表达式示例 ===");

        Method method = UserService.class.getMethod("getUserById", String.class);

        // 第一次调用，应该执行方法并缓存结果
        Object result1 = interceptor.process(userService, method, new Object[] { "user1" },
                () -> userService.getUserById("user1"));
        System.out.println("第一次调用结果: " + result1);

        // 第二次调用，应该直接从缓存返回
        Object result2 = interceptor.process(userService, method, new Object[] { "user1" },
                () -> userService.getUserById("user1"));
        System.out.println("第二次调用结果: " + result2);
    }

    /**
     * 复杂SpEL表达式示例
     */
    private static void complexSpelExample(UserService userService, CacheInterceptor interceptor) throws Exception {
        System.out.println("\n=== 复杂SpEL表达式示例 ===");

        Method method = UserService.class.getMethod("getUserByIdAndType", String.class, String.class);

        // 第一次调用，应该执行方法并缓存结果
        Object result1 = interceptor.process(userService, method, new Object[] { "user1", "vip" },
                () -> userService.getUserByIdAndType("user1", "vip"));
        System.out.println("第一次调用结果: " + result1);

        // 第二次调用，应该直接从缓存返回
        Object result2 = interceptor.process(userService, method, new Object[] { "user1", "vip" },
                () -> userService.getUserByIdAndType("user1", "vip"));
        System.out.println("第二次调用结果: " + result2);
    }

    /**
     * 自定义SpEL键生成器示例
     */
    private static void customSpelKeyGeneratorExample(OrderService orderService) throws Exception {
        System.out.println("\n=== 自定义SpEL键生成器示例 ===");

        // 创建自定义键生成器
        SpelKeyGenerator keyGenerator = new SpelKeyGenerator("order:#{#p0}:user:#{#p1}");
        CacheInterceptor interceptor = new CacheInterceptor(keyGenerator, null);

        Method method = OrderService.class.getMethod("getOrderById", String.class, String.class);

        // 第一次调用，应该执行方法并缓存结果
        Object result1 = interceptor.process(orderService, method, new Object[] { "order1", "user1" },
                () -> orderService.getOrderById("order1", "user1"));
        System.out.println("第一次调用结果: " + result1);

        // 第二次调用，应该直接从缓存返回
        Object result2 = interceptor.process(orderService, method, new Object[] { "order1", "user1" },
                () -> orderService.getOrderById("order1", "user1"));
        System.out.println("第二次调用结果: " + result2);
    }

    /**
     * 用户服务
     */
    static class UserService {

        /**
         * 使用基本SpEL表达式的缓存方法
         */
        @Cached(key = "user:#{#p0}", expire = 5, timeUnit = TimeUnit.MINUTES)
        public User getUserById(String id) {
            System.out.println("从数据库加载用户: " + id);
            return new User(id, "用户" + id);
        }

        /**
         * 使用复杂SpEL表达式的缓存方法
         */
        @Cached(key = "user:#{#p0}:#{#p1}", expire = 5, timeUnit = TimeUnit.MINUTES, cacheType = CacheType.TWO_LEVEL, refresh = true, refreshInterval = 30, refreshTimeUnit = TimeUnit.SECONDS)
        public User getUserByIdAndType(String id, String type) {
            System.out.println("从数据库加载用户: " + id + ", 类型: " + type);
            return new User(id, "用户" + id + "(" + type + ")");
        }
    }

    /**
     * 订单服务
     */
    static class OrderService {

        /**
         * 不使用SpEL表达式的缓存方法，将使用自定义键生成器
         */
        @Cached(key = "order", expire = 5, timeUnit = TimeUnit.MINUTES)
        public Order getOrderById(String orderId, String userId) {
            System.out.println("从数据库加载订单: " + orderId + ", 用户: " + userId);
            return new Order(orderId, userId, 100.0);
        }
    }

    /**
     * 用户实体
     */
    static class User {
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

    /**
     * 订单实体
     */
    static class Order {
        private String id;
        private String userId;
        private double amount;

        public Order(String id, String userId, double amount) {
            this.id = id;
            this.userId = userId;
            this.amount = amount;
        }

        public String getId() {
            return id;
        }

        public String getUserId() {
            return userId;
        }

        public double getAmount() {
            return amount;
        }

        @Override
        public String toString() {
            return "Order{" +
                    "id='" + id + '\'' +
                    ", userId='" + userId + '\'' +
                    ", amount=" + amount +
                    '}';
        }
    }
}