package com.easy.cache.example;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl implements UserService {
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