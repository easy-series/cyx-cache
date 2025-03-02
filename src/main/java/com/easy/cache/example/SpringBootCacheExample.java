package com.easy.cache.example;

import java.util.concurrent.TimeUnit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.easy.cache.annotation.Cached;
import com.easy.cache.annotation.Cached.CacheType;

/**
 * Spring Boot缓存示例应用程序
 */
@SpringBootApplication
@ComponentScan("com.easy.cache")
public class SpringBootCacheExample {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootCacheExample.class, args);
    }

    /**
     * 用户控制器
     */
    @RestController
    @RequestMapping("/api/users")
    public static class UserController {

        private final UserService userService;

        public UserController(UserService userService) {
            this.userService = userService;
        }

        @GetMapping("/{id}")
        public User getUserById(@PathVariable String id) {
            return userService.getUserById(id);
        }

        @GetMapping("/{id}/{type}")
        public User getUserByIdAndType(@PathVariable String id, @PathVariable String type) {
            return userService.getUserByIdAndType(id, type);
        }
    }

    /**
     * 用户服务
     */
    @RestController
    public static class UserService {

        /**
         * 使用本地缓存
         */
        @Cached(key = "user:#{#p0}", expire = 5, timeUnit = TimeUnit.MINUTES)
        public User getUserById(String id) {
            System.out.println("从数据库加载用户: " + id);
            return new User(id, "用户" + id);
        }

        /**
         * 使用二级缓存（本地+Redis）
         */
        @Cached(key = "user:#{#p0}:#{#p1}", expire = 5, timeUnit = TimeUnit.MINUTES, cacheType = CacheType.TWO_LEVEL, refresh = true, refreshInterval = 30, refreshTimeUnit = TimeUnit.SECONDS)
        public User getUserByIdAndType(String id, String type) {
            System.out.println("从数据库加载用户: " + id + ", 类型: " + type);
            return new User(id, "用户" + id + "(" + type + ")");
        }
    }

    /**
     * 用户实体
     */
    public static class User {
        private String id;
        private String name;

        public User() {
        }

        public User(String id, String name) {
            this.id = id;
            this.name = name;
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

        @Override
        public String toString() {
            return "User{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    '}';
        }
    }
}