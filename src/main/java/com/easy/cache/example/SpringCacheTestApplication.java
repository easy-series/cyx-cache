package com.easy.cache.example;

import java.math.BigDecimal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Spring Boot应用程序，用于测试缓存注解
 */
@SpringBootApplication
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ComponentScan("com.easy.cache")
public class SpringCacheTestApplication {

    public static void main(String[] args) throws Exception {
        ConfigurableApplicationContext context = SpringApplication.run(SpringCacheTestApplication.class, args);

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
    }
}