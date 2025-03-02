package com.easy.cache.example;

import com.easy.cache.annotation.CacheInvalidate;
import com.easy.cache.annotation.CacheUpdate;
import com.easy.cache.annotation.Cached;
import com.easy.cache.core.QuickConfig.CacheType;

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