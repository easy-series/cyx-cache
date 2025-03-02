# Easy Cache 框架

Easy Cache 是一个简单易用的 Java 缓存框架，提供了本地缓存、分布式缓存和多级缓存实现以及注解支持。

## 特性

- 简单易用的 API
- 支持本地缓存
- 支持分布式缓存（Redis）
- 支持多级缓存（本地缓存 + Redis缓存）
- 支持缓存自动刷新
- 支持缓存穿透防护
- 支持缓存雪崩防护
- 支持缓存击穿防护
- 支持热点数据保护
- 支持数据加密
- 支持 Spring Boot 自动配置

## 快速开始

### Maven 依赖

```xml
<dependency>
    <groupId>com.easy</groupId>
    <artifactId>easy-cache</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 基本使用

```java
// 获取缓存管理器
CacheManager cacheManager = CacheManager.getInstance();

// 创建本地缓存
Cache<String, User> userCache = cacheManager.getOrCreateLocalCache("userCache");

// 放入缓存
User user = new User(1, "张三", 20);
userCache.put("user:1", user, 10, TimeUnit.MINUTES);

// 获取缓存
User cachedUser = userCache.get("user:1");

// 删除缓存
userCache.remove("user:1");
```

### 使用注解

1. 启用缓存注解

```java
@Configuration
@EnableCaching(enableRemoteCache = true, enableCacheSync = true)
public class CacheConfig {
}
```

2. 使用缓存注解

```java
@Service
public class UserService {

    @Cached(expire = 3600, cacheType = CacheType.BOTH)
    public User getUserById(long userId) {
        // 从数据库查询用户
        return userRepository.findById(userId);
    }

    @CacheUpdate(key = "'user:' + #user.id", value = "#user")
    public User updateUser(User user) {
        // 更新用户
        return userRepository.save(user);
    }

    @CacheInvalidate(key = "'user:' + #userId")
    public void deleteUser(long userId) {
        // 删除用户
        userRepository.deleteById(userId);
    }
}
```

### Spring Boot 自动配置

在 `application.properties` 或 `application.yml` 中配置：

```yaml
easy:
  cache:
    enabled: true
    local:
      enabled: true
      maximum-size: 10000
      expire-after-write: 30
      time-unit: MINUTES
    redis:
      enabled: true
      host: localhost
      port: 6379
      password: 
      database: 0
      expire-after-write: 60
      time-unit: MINUTES
      serializer: JSON
    multi-level:
      enabled: true
      write-through: true
      async-write: false
```

## 高级特性

### 缓存穿透防护

使用布隆过滤器防止缓存穿透：

```java
// 创建布隆过滤器缓存
BloomFilterCache<String, User> userCache = new BloomFilterCache<>(
    cacheManager.getOrCreateLocalCache("userCache"),
    10000,  // 预期插入数量
    0.01    // 误判率
);

// 使用缓存
userCache.put("user:1", user);
User cachedUser = userCache.get("user:1");
```

或者使用注解：

```java
@Cached(expire = 3600, cacheType = CacheType.REMOTE)
@CachePenetrationProtect(timeout = 5000)
public User getUserById(long userId) {
    // 从数据库查询用户
    return userRepository.findById(userId);
}
```

### 缓存雪崩防护

使用熔断器防止缓存雪崩：

```java
// 创建熔断器缓存
CircuitBreakerCache<String, User> userCache = new CircuitBreakerCache<>(
    cacheManager.getOrCreateRemoteCache("userCache"),
    5,                  // 失败阈值
    30, TimeUnit.SECONDS // 重置超时时间
);

// 使用缓存
userCache.put("user:1", user);
User cachedUser = userCache.get("user:1");
```

### 热点数据保护

使用热点数据缓存防止缓存击穿：

```java
// 创建热点数据缓存
HotKeyCache<String, User> userCache = new HotKeyCache<>(
    cacheManager.getOrCreateCache(
        QuickConfig.builder()
            .name("userCache")
            .cacheType(CacheType.BOTH)
            .build()
    ),
    1000,                // 访问阈值
    60, TimeUnit.SECONDS, // 时间窗口
    300, TimeUnit.SECONDS // 本地缓存过期时间
);

// 使用缓存
userCache.put("user:1", user);
User cachedUser = userCache.get("user:1");
```

或者使用注解：

```java
@Cached(expire = 3600, cacheType = CacheType.BOTH)
@HotKeyProtect(threshold = 1000, timeWindow = 60, localExpire = 300)
public User getUserById(long userId) {
    // 从数据库查询用户
    return userRepository.findById(userId);
}
```

### 数据加密

使用加密缓存保护敏感数据：

```java
// 创建加密缓存
EncryptedCache<String, User> userCache = new EncryptedCache<>(
    cacheManager.getOrCreateRemoteCache("userCache"),
    new AesEncryptor("your-secret-key")
);

// 使用缓存
userCache.put("user:1", user);
User cachedUser = userCache.get("user:1");
```

## 最佳实践

### 缓存键设计

- 使用有意义的前缀，例如：`user:1`、`order:1001`
- 对于复杂对象，使用多个字段组合，例如：`user:1:roles`
- 避免使用太长的键，会影响性能

### 缓存过期策略

- 根据数据更新频率设置合理的过期时间
- 对于频繁访问但很少变化的数据，可以设置较长的过期时间
- 对于频繁变化的数据，可以设置较短的过期时间或使用缓存更新注解

### 缓存穿透、击穿和雪崩防护

- 使用布隆过滤器防止缓存穿透
- 使用热点数据保护防止缓存击穿
- 使用熔断器防止缓存雪崩
- 为不同的缓存设置不同的过期时间，避免同时失效

### 多级缓存策略

- 对于频繁访问的数据，使用多级缓存（本地 + Redis）
- 对于较大的数据，可以只在本地缓存中存储
- 对于需要在多个实例间共享的数据，使用 Redis 缓存

## 性能优化

- 使用 JSON 序列化代替 JDK 序列化，提高序列化/反序列化性能
- 对于 Redis 缓存，使用批量操作提高性能
- 使用异步写入减少写入延迟
- 合理设置本地缓存大小，避免频繁淘汰

## 监控与管理

- 使用 `CacheStats` 收集缓存统计信息
- 定期检查缓存命中率，调整缓存策略
- 对于热点数据，可以预加载到缓存中
- 在系统负载较低时执行缓存预热

## 示例代码

更多示例代码请参考 `com.easy.cache.example` 包。
