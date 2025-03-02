package com.easy.cache.example;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import com.easy.cache.annotation.CacheInterceptor;
import com.easy.cache.annotation.Cached;
import com.easy.cache.core.Cache;
import com.easy.cache.core.CacheBuilder;

/**
 * 缓存使用示例
 */
public class CacheExample {

    public static void main(String[] args) throws Exception {
        // 示例1：直接使用缓存API
        directApiExample();

        // 示例2：使用注解
        annotationExample();
    }

    /**
     * 直接使用缓存API的示例
     */
    private static void directApiExample() {
        // 创建缓存
        Cache<String, String> cache = CacheBuilder.<String, String>newBuilder()
                .name("myCache")
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .buildLocalCache();

        // 放入缓存
        cache.put("key1", "value1");

        // 获取缓存
        String value = cache.get("key1");
        System.out.println("从缓存获取值: " + value);

        // 使用加载器获取缓存
        String value2 = cache.get("key2", k -> {
            System.out.println("加载值: " + k);
            return "value for " + k;
        });
        System.out.println("从缓存获取值: " + value2);

        // 再次获取，应该直接从缓存返回
        String value3 = cache.get("key2", k -> {
            System.out.println("加载值: " + k);
            return "new value for " + k;
        });
        System.out.println("从缓存获取值: " + value3);
    }

    /**
     * 使用注解的示例
     */
    private static void annotationExample() throws Exception {
        UserService userService = new UserService();
        CacheInterceptor interceptor = new CacheInterceptor();

        // 模拟AOP拦截
        Method method = UserService.class.getMethod("getUserById", String.class);

        // 第一次调用，应该执行方法并缓存结果
        Object result1 = interceptor.process(userService, method, new Object[] { "user1" },
                () -> userService.getUserById("user1"));
        System.out.println("第一次调用结果: " + result1);

        // 第二次调用，应该直接从缓存返回
        Object result2 = interceptor.process(userService, method, new Object[] { "user1" },
                () -> userService.getUserById("user1"));
        System.out.println("第二次调用结果: " + result2);

        // 不同参数，应该执行方法并缓存结果
        Object result3 = interceptor.process(userService, method, new Object[] { "user2" },
                () -> userService.getUserById("user2"));
        System.out.println("不同参数调用结果: " + result3);
    }

    /**
     * 用户服务
     */
    static class UserService {

        @Cached(key = "user:#{#p0}", expire = 5, timeUnit = TimeUnit.MINUTES)
        public User getUserById(String id) {
            System.out.println("从数据库加载用户: " + id);
            return new User(id, "用户" + id);
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
}