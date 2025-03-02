package com.easy.cache.example;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.easy.cache.annotation.CacheInvalidate;
import com.easy.cache.annotation.CachePenetrationProtect;
import com.easy.cache.annotation.CacheRefresh;
import com.easy.cache.annotation.CacheUpdate;
import com.easy.cache.annotation.Cached;
import com.easy.cache.annotation.EnableCaching;
import com.easy.cache.core.QuickConfig.CacheType;

/**
 * 缓存注解使用示例
 */
@EnableCaching(enableRemoteCache = true, enableCacheSync = true)
public class AnnotationExample {

    public static void main(String[] args) throws Exception {
        // 创建用户服务
        UserService userService = new UserServiceImpl();

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

        // 创建汇总服务
        SummaryService summaryService = new SummaryServiceImpl();

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
        @Cached(expire = 3600, cacheType = CacheType.REMOTE)
        User getUserById(long userId);

        /**
         * 更新用户
         * 
         * @param user 用户对象
         */
        @CacheUpdate(name = "userCache-", key = "#user.id", value = "#user")
        void updateUser(User user);

        /**
         * 删除用户
         * 
         * @param userId 用户ID
         */
        @CacheInvalidate(name = "userCache-", key = "#userId")
        void deleteUser(long userId);
    }

    /**
     * 用户服务实现类
     */
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
            System.out.println("从数据库获取用户: " + userId);
            return userMap.get(userId);
        }

        @Override
        public void updateUser(User user) {
            System.out.println("更新用户: " + user);
            userMap.put(user.getId(), user);
        }

        @Override
        public void deleteUser(long userId) {
            System.out.println("删除用户: " + userId);
            userMap.remove(userId);
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
        @Cached(expire = 3600, cacheType = CacheType.REMOTE)
        @CacheRefresh(refresh = 1, stopRefreshAfterLastAccess = 3600, timeUnit = TimeUnit.SECONDS)
        @CachePenetrationProtect
        BigDecimal summaryOfToday(long categoryId);
    }

    /**
     * 汇总服务实现类
     */
    public static class SummaryServiceImpl implements SummaryService {
        private int count = 0;

        @Override
        public BigDecimal summaryOfToday(long categoryId) {
            System.out.println("计算汇总数据: " + categoryId);
            // 模拟计算汇总数据
            count++;
            return new BigDecimal("100.00").add(new BigDecimal(count));
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