# Easy Cache 注解使用指南

Easy Cache 提供了类似 JetCache 的注解功能，可以通过简单的注解来使用缓存功能。

## 1. 启用缓存注解

在配置类上添加 `@EnableCaching` 注解来启用缓存注解功能：

```java
@Configuration
@EnableCaching(enableRemoteCache = true, enableCacheSync = true)
public class AppConfig {
    
    @Bean
    public JedisPool jedisPool() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(10);
        config.setMaxIdle(5);
        return new JedisPool(config, "localhost", 6379);
    }
    
    @Bean
    public Serializer serializer() {
        return new JdkSerializer();
    }
}
```

## 2. 使用 @Cached 注解

使用 `@Cached` 注解标记需要缓存的方法：

```java
public interface UserService {
    
    // 使用默认键生成策略
    @Cached(expire = 3600, cacheType = CacheType.REMOTE)
    User getUserById(long userId);
    
    // 使用SpEL表达式指定缓存键
    @Cached(name = "userCache-", key = "#userId", expire = 3600)
    User getUserByIdWithCustomKey(long userId);
}
```

## 3. 使用 @CacheUpdate 注解

使用 `@CacheUpdate` 注解标记需要更新缓存的方法：

```java
public interface UserService {
    
    @CacheUpdate(name = "userCache-", key = "#user.id", value = "#user")
    void updateUser(User user);
}
```

## 4. 使用 @CacheInvalidate 注解

使用 `@CacheInvalidate` 注解标记需要使缓存失效的方法：

```java
public interface UserService {
    
    @CacheInvalidate(name = "userCache-", key = "#userId")
    void deleteUser(long userId);
    
    @CacheInvalidate(name = "userCache-", allEntries = true)
    void deleteAllUsers();
}
```

## 5. 使用 @CacheRefresh 注解

使用 `@CacheRefresh` 注解标记需要自动刷新的缓存：

```java
public interface SummaryService{
    @Cached(expire = 3600, cacheType = CacheType.REMOTE)
    @CacheRefresh(refresh = 1800, stopRefreshAfterLastAccess = 3600, timeUnit = TimeUnit.SECONDS)
    @CachePenetrationProtect
    BigDecimal summaryOfToday(long categoryId);
}
```

## 6. 使用 @CachePenetrationProtect 注解

使用 `@CachePenetrationProtect` 注解标记需要防止缓存穿透的方法：

```java
public interface SummaryService {
    
    @Cached(expire = 3600, cacheType = CacheType.REMOTE)
    @CachePenetrationProtect
    BigDecimal summaryOfToday(long categoryId);
}
```

## 7. SpEL 表达式支持

Easy Cache 支持在缓存键和值中使用 SpEL 表达式：

- `#参数名`：引用方法参数
- `#参数名.属性`：引用方法参数的属性
- `#result`：引用方法返回值（仅在 `@CacheUpdate` 和 `@CacheInvalidate` 中可用）
- `#p0`, `#p1`, ...：按索引引用方法参数

为了使用参数名称（如 `#userId`），您的 javac 编译器目标必须是 1.8 及以上版本，并且编译时需要添加 `-parameters` 参数。否则，请使用索引来访问参数，例如 `#p0`。

## 8. 完整示例

```java
@EnableCaching(enableRemoteCache = true, enableCacheSync = true)
public class AnnotationExample {
    
    public interface UserService {
        @Cached(expire = 3600, cacheType = CacheType.REMOTE)
        User getUserById(long userId);
        
        @CacheUpdate(name = "userCache-", key = "#user.id", value = "#user")
        void updateUser(User user);
        
        @CacheInvalidate(name = "userCache-", key = "#userId")
        void deleteUser(long userId);
    }
    
    public interface SummaryService {
        @Cached(expire = 3600, cacheType = CacheType.REMOTE)
        @CacheRefresh(refresh = 1800, stopRefreshAfterLastAccess = 3600, timeUnit = TimeUnit.SECONDS)
        @CachePenetrationProtect
        BigDecimal summaryOfToday(long categoryId);
    }
}
```
