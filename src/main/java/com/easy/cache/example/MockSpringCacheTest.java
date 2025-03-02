package com.easy.cache.example;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

/**
 * 模拟Spring缓存的测试类
 */
@SpringBootApplication
public class MockSpringCacheTest {

    // 简单的缓存实现
    private static final Map<String, Object> CACHE = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(1);

    public static void main(String[] args) throws Exception {
        ConfigurableApplicationContext context = SpringApplication.run(MockSpringCacheTest.class, args);

        // 获取用户服务
        UserService userService = context.getBean(UserService.class);

        // 获取用户
        User user1 = userService.getUserById(1L);
        System.out.println("获取用户1: " + user1);

        // 再次获取用户（应该从缓存中获取）
        user1 = userService.getUserById(1L);
        System.out.println("再次获取用户1: " + user1);

        // 更新用户
        user1.setName("张三（已更新）");
        userService.updateUser(user1);
        System.out.println("更新用户1: " + user1);

        // 再次获取用户（应该获取更新后的用户）
        user1 = userService.getUserById(1L);
        System.out.println("更新后获取用户1: " + user1);

        // 删除用户
        userService.deleteUser(1L);
        System.out.println("删除用户1");

        // 再次获取用户（应该返回null）
        user1 = userService.getUserById(1L);
        System.out.println("删除后获取用户1: " + user1);

        // 获取汇总服务
        SummaryService summaryService = context.getBean(SummaryService.class);

        // 获取汇总数据
        BigDecimal summary = summaryService.summaryOfToday(1L);
        System.out.println("获取汇总数据: " + summary);

        // 再次获取汇总数据（应该从缓存中获取）
        summary = summaryService.summaryOfToday(1L);
        System.out.println("再次获取汇总数据: " + summary);

        // 等待自动刷新
        System.out.println("等待自动刷新...");
        Thread.sleep(2000);

        // 再次获取汇总数据（应该获取刷新后的数据）
        summary = summaryService.summaryOfToday(1L);
        System.out.println("刷新后获取汇总数据: " + summary);

        // 关闭线程池
        SCHEDULER.shutdown();
    }

    @Bean
    public UserService userService() {
        return new UserServiceImpl();
    }

    @Bean
    public SummaryService summaryService() {
        return new SummaryServiceImpl();
    }

    /**
     * 用户服务接口
     */
    public interface UserService {
        /**
         * 根据ID获取用户
         * 
         * @param userId 用户ID
         * @return 用户对象
         */
        User getUserById(long userId);

        /**
         * 更新用户
         * 
         * @param user 用户对象
         */
        void updateUser(User user);

        /**
         * 删除用户
         * 
         * @param userId 用户ID
         */
        void deleteUser(long userId);
    }

    /**
     * 用户服务实现类
     */
    @Service
    public static class UserServiceImpl implements UserService {
        private final Map<Long, User> userMap = new HashMap<>();

        public UserServiceImpl() {
            // 初始化用户数据
            userMap.put(1L, new User(1L, "张三", 30));
            userMap.put(2L, new User(2L, "李四", 25));
            userMap.put(3L, new User(3L, "王五", 35));
        }

        @Override
        public User getUserById(long userId) {
            String cacheKey = "userCache-" + userId;

            // 尝试从缓存获取
            if (CACHE.containsKey(cacheKey)) {
                System.out.println("从缓存获取用户: " + userId);
                return (User) CACHE.get(cacheKey);
            }

            // 从数据库获取
            System.out.println("从数据库获取用户: " + userId);
            User user = userMap.get(userId);

            // 放入缓存
            if (user != null) {
                CACHE.put(cacheKey, user);
            }

            return user;
        }

        @Override
        public void updateUser(User user) {
            System.out.println("更新用户: " + user);
            userMap.put(user.getId(), user);

            // 更新缓存
            String cacheKey = "userCache-" + user.getId();
            CACHE.put(cacheKey, user);
        }

        @Override
        public void deleteUser(long userId) {
            System.out.println("删除用户: " + userId);
            userMap.remove(userId);

            // 删除缓存
            String cacheKey = "userCache-" + userId;
            CACHE.remove(cacheKey);
        }
    }

    /**
     * 汇总服务接口
     */
    public interface SummaryService {
        /**
         * 获取今日汇总数据
         * 
         * @param categoryId 分类ID
         * @return 汇总数据
         */
        BigDecimal summaryOfToday(long categoryId);
    }

    /**
     * 汇总服务实现类
     */
    @Service
    public static class SummaryServiceImpl implements SummaryService {
        private int count = 0;
        private boolean refreshScheduled = false;

        @Override
        public BigDecimal summaryOfToday(long categoryId) {
            String cacheKey = "summaryCache-" + categoryId;

            // 尝试从缓存获取
            if (CACHE.containsKey(cacheKey)) {
                System.out.println("从缓存获取汇总数据: " + categoryId);

                // 如果还没有设置自动刷新，则设置
                if (!refreshScheduled) {
                    scheduleRefresh(categoryId);
                    refreshScheduled = true;
                }

                return (BigDecimal) CACHE.get(cacheKey);
            }

            // 计算汇总数据
            System.out.println("计算汇总数据: " + categoryId);
            count++;
            BigDecimal result = new BigDecimal("100.00").add(new BigDecimal(count));

            // 放入缓存
            CACHE.put(cacheKey, result);

            // 设置自动刷新
            if (!refreshScheduled) {
                scheduleRefresh(categoryId);
                refreshScheduled = true;
            }

            return result;
        }

        private void scheduleRefresh(long categoryId) {
            SCHEDULER.scheduleAtFixedRate(() -> {
                String cacheKey = "summaryCache-" + categoryId;
                System.out.println("自动刷新汇总数据: " + categoryId);
                count++;
                BigDecimal result = new BigDecimal("100.00").add(new BigDecimal(count));
                CACHE.put(cacheKey, result);
            }, 1, 1, TimeUnit.SECONDS);
        }
    }

    /**
     * 用户类
     */
    public static class User {
        private Long id;
        private String name;
        private int age;

        public User() {
        }

        public User(Long id, String name, int age) {
            this.id = id;
            this.name = name;
            this.age = age;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
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
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", age=" + age +
                    '}';
        }
    }
}