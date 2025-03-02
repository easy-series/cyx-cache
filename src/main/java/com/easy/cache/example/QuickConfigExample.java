package com.easy.cache.example;

import java.util.concurrent.TimeUnit;

import com.easy.cache.core.Cache;
import com.easy.cache.core.CacheManager;
import com.easy.cache.core.QuickConfig;
import com.easy.cache.core.RefreshableCache;

/**
 * QuickConfig 使用示例
 * 演示如何使用 QuickConfig 快速配置和使用缓存
 */
public class QuickConfigExample {

    public static void main(String[] args) {
        // 创建缓存管理器
        CacheManager cacheManager = CacheManager.getInstance();

        // 示例1：创建一个简单的本地缓存
        QuickConfig localConfig = QuickConfig.builder()
                .name("userCache")
                .expire(30, TimeUnit.MINUTES)
                .cacheType(QuickConfig.CacheType.LOCAL)
                .build();

        Cache<String, User> userCache = cacheManager.getOrCreateCache(localConfig);

        // 使用缓存
        User user = new User("1", "张三", 25);
        userCache.put("user:1", user);
        User cachedUser = userCache.get("user:1");
        System.out.println("从本地缓存获取用户: " + cachedUser);

        // 示例2：创建一个Redis缓存
        QuickConfig redisConfig = QuickConfig.builder()
                .name("orderCache")
                .expire(1, TimeUnit.HOURS)
                .cacheType(QuickConfig.CacheType.REMOTE)
                .build();

        Cache<String, Order> orderCache = cacheManager.getOrCreateCache(redisConfig);

        // 使用缓存
        Order order = new Order("O001", "用户1", 199.99);
        orderCache.put("order:O001", order);
        Order cachedOrder = orderCache.get("order:O001");
        System.out.println("从Redis缓存获取订单: " + cachedOrder);

        // 示例3：创建一个两级缓存
        QuickConfig twoLevelConfig = QuickConfig.builder()
                .name("productCache")
                .expire(2, TimeUnit.HOURS)
                .cacheType(QuickConfig.CacheType.BOTH)
                .writeThrough(true) // 启用写透模式
                .asyncWrite(false) // 同步写入
                .build();

        Cache<String, Product> productCache = cacheManager.getOrCreateCache(twoLevelConfig);

        // 使用缓存
        Product product = new Product("P001", "笔记本电脑", 5999.00);
        productCache.put("product:P001", product);
        Product cachedProduct = productCache.get("product:P001");
        System.out.println("从两级缓存获取产品: " + cachedProduct);

        // 示例4：创建一个可自动刷新的缓存
        QuickConfig refreshableConfig = QuickConfig.builder()
                .name("stockCache")
                .expire(5, TimeUnit.MINUTES)
                .cacheType(QuickConfig.CacheType.LOCAL)
                .refreshable(true)
                .refreshInterval(1, TimeUnit.MINUTES)
                .build();

        Cache<String, Integer> stockCache = cacheManager.getOrCreateCache(refreshableConfig);

        // 使用缓存
        stockCache.put("stock:P001", 100);
        Integer stock = stockCache.get("stock:P001");
        System.out.println("从可刷新缓存获取库存: " + stock);

        // 注册刷新函数 - 需要转换为 RefreshableCache 类型
        if (stockCache instanceof RefreshableCache) {
            RefreshableCache<String, Integer> refreshableCache = (RefreshableCache<String, Integer>) stockCache;
            refreshableCache.registerLoader("stock:P001", key -> {
                // 模拟从数据库获取最新库存
                System.out.println("刷新库存数据...");
                return 95; // 返回新的库存值
            });

            // 等待刷新
            try {
                System.out.println("等待缓存自动刷新...");
                Thread.sleep(70 * 1000); // 等待超过刷新间隔

                // 再次获取，应该是刷新后的值
                Integer newStock = stockCache.get("stock:P001");
                System.out.println("刷新后的库存: " + newStock);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("缓存不是 RefreshableCache 类型，无法注册刷新函数");
        }
    }

    // 用户实体类
    static class User {
        private String id;
        private String name;
        private int age;

        public User(String id, String name, int age) {
            this.id = id;
            this.name = name;
            this.age = age;
        }

        @Override
        public String toString() {
            return "User{id='" + id + "', name='" + name + "', age=" + age + '}';
        }
    }

    // 订单实体类
    static class Order {
        private String orderId;
        private String userId;
        private double amount;

        public Order(String orderId, String userId, double amount) {
            this.orderId = orderId;
            this.userId = userId;
            this.amount = amount;
        }

        @Override
        public String toString() {
            return "Order{orderId='" + orderId + "', userId='" + userId + "', amount=" + amount + '}';
        }
    }

    // 产品实体类
    static class Product {
        private String productId;
        private String name;
        private double price;

        public Product(String productId, String name, double price) {
            this.productId = productId;
            this.name = name;
            this.price = price;
        }

        @Override
        public String toString() {
            return "Product{productId='" + productId + "', name='" + name + "', price=" + price + '}';
        }
    }
}