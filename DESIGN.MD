# cyx-cache 缓存框架设计方案

## 一、总体架构

cyx-cache采用三模块架构设计，提供统一的缓存抽象和灵活的使用方式：

```
                  +------------------+
                  |    应用层代码     |
                  +------------------+
                          ↓ 
+---------------+  +-------------------------+  +---------------+
| cyx-cache-core |←-| c y x-spring-boot-starter |←-| cyx-cache-redis |
+---------------+  +-------------------------+  +---------------+
```

## 二、核心模块

### 1. cyx-cache-core

基础模块，提供核心抽象和本地缓存实现：

#### 主要组件：

1. **基础接口**
   ```java
   public interface Cache<K, V> {
       V get(K key);
       void put(K key, V value);
       void put(K key, V value, Duration ttl);
       boolean remove(K key);
       void clear();
       // 其他基础方法...
   }
   
   public interface CacheManager {
       <K, V> Cache<K, V> getCache(String name);
       <K, V> Cache<K, V> createCache(CacheConfig config);
   }
   ```

2. **配置体系**
   ```java
   public class CacheConfig {
       private String name;
       private Duration expire;
       private CacheType cacheType;
       private int localLimit;
       // 构建器模式...
   }
   ```

3. **序列化接口**
   ```java
   public interface KeyConvertor<K> {
       String convert(K key);
   }
   
   public interface ValueEncoder<V> {
       byte[] encode(V value);
   }
   ```

4. **本地缓存实现**
   - 基于LinkedHashMap的简单实现
   - 基于Caffeine的高性能实现

5. **统计组件**
   ```java
   public class CacheStats {
       // 命中率、读写次数等统计数据
   }
   ```

### 2. cyx-cache-redis

提供基于Redis的远程缓存实现：

#### 主要组件：

1. **Redis缓存实现**
   ```java
   public class RedisCache<K, V> implements Cache<K, V> {
       // Redis缓存实现
   }
   
   public class RedisCacheManager implements CacheManager {
       // Redis缓存管理器
   }
   ```

2. **分布式特性**
   ```java
   public class RedisLock {
       public boolean tryLock(K key, Duration timeout);
       public void unlock(K key);
   }
   
   public class RefreshPolicy {
       // 自动刷新策略
   }
   ```

3. **高级序列化实现**
   ```java
   public class KryoValueEncoder implements ValueEncoder<Object>;
   public class JacksonKeyConvertor implements KeyConvertor<Object>;
   ```

4. **广播机制**
   ```java
   public class RedisPubSubService {
       // 用于同步多节点缓存
   }
   ```

### 3. cyx-cache-spring-boot-starter

与Spring Boot框架的集成：

#### 主要组件：

1. **缓存注解**
   ```java
   @Target({ElementType.METHOD})
   @Retention(RetentionPolicy.RUNTIME)
   public @interface Cached {
       String name() default "";
       String key() default "";
       long expire() default 0;
       CacheType cacheType() default CacheType.REMOTE;
   }
   
   @Target({ElementType.METHOD})
   @Retention(RetentionPolicy.RUNTIME)
   public @interface CacheUpdate {
       // 缓存更新注解
   }
   
   @Target({ElementType.METHOD})
   @Retention(RetentionPolicy.RUNTIME)
   public @interface CacheInvalidate {
       // 缓存失效注解
   }
   ```

2. **Spring Boot自动配置**
   ```java
   @Configuration
   @EnableConfigurationProperties(cyx-cacheProperties.class)
   public class cyx-cacheAutoConfiguration {
       // 自动配置类
   }
   ```

3. **属性配置**
   ```java
   @ConfigurationProperties(prefix = "cyx-cache")
   public class cyx-cacheProperties {
       private Map<String, LocalConfig> local = new HashMap<>();
       private Map<String, RedisConfig> remote = new HashMap<>();
       // 其他配置项...
   }
   ```

## 三、核心功能特性

### 1. 统一的缓存抽象

```java
// 基础缓存操作
V value = cache.get("key");
cache.put("key", value, Duration.ofSeconds(60));

// 高级操作
cache.computeIfAbsent("key", k -> loadFromDb(k));
```

### 2. 多级缓存支持

```java
CacheConfig config = CacheConfig.newBuilder("userCache")
    .cacheType(CacheType.BOTH)  // 同时使用本地和远程缓存
    .localLimit(100)            // 本地缓存限制
    .expire(Duration.ofMinutes(10))
    .build();
```

### 3. 强大的注解支持

```java
@Service
public class UserService {
    @Cached(name="user:", key="#userId", expire=3600)
    public User getUser(long userId) {
        // 方法实现
    }
    
    @CacheUpdate(name="user:", key="#user.id", value="#user")
    public void updateUser(User user) {
        // 更新方法
    }
    
    @CacheInvalidate(name="user:", key="#userId")
    public void deleteUser(long userId) {
        // 删除方法
    }
}
```

### 4. 自动刷新与防击穿

```java
CacheConfig config = CacheConfig.newBuilder("hotData")
    .refreshPolicy(RefreshPolicy.newPolicy()
        .refreshInterval(Duration.ofMinutes(5))
        .stopRefreshAfterLastAccess(Duration.ofHours(1)))
    .penetrationProtect(true)  // 防止缓存击穿
    .loader(this::loadDataFromDb)
    .build();
```

### 5. 分布式锁

```java
cache.tryLockAndRun("resourceKey", Duration.ofSeconds(30), () -> {
    // 需要分布式锁保护的操作
});
```

### 6. 统计监控

```java
CacheStats stats = cache.stats();
System.out.println("命中率: " + stats.hitRate());
System.out.println("平均加载时间: " + stats.avgLoadTime() + "ms");
```

## 四、配置示例

### 1. Spring Boot配置

```yaml
cyx-cache:
  stats-interval: 60  # 统计间隔(秒)
  local:
    default:
      type: caffeine
      limit: 200
      key-convertor: fastjson
  remote:
    default:
      type: redis
      host: localhost
      port: 6379
      database: 0
      key-convertor: fastjson
      value-encoder: java
      value-decoder: java
      broadcast-channel: app1
```

### 2. Java代码配置

```java
@Configuration
@EnableMethodCache(basePackages = "com.example.service")
public class CacheConfig {
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // Redis连接配置
    }
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        // 缓存管理器配置
    }
}
```

## 五、模块依赖关系

### Maven依赖

1. **仅使用本地缓存**
```xml
<dependency>
    <groupId>com.caoyixin</groupId>
    <artifactId>cyx-cache-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

2. **使用Redis缓存**
```xml
<dependency>
    <groupId>com.caoyixin</groupId>
    <artifactId>cyx-cache-core</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>com.caoyixin</groupId>
    <artifactId>cyx-cache-redis</artifactId>
    <version>1.0.0</version>
</dependency>
```

3. **与Spring Boot集成**
```xml
<dependency>
    <groupId>com.caoyixin</groupId>
    <artifactId>cyx-cache-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 六、扩展性设计

### 1. 自定义键生成器

```java
public class CustomKeyGenerator implements KeyGenerator {
    @Override
    public Object generate(Object target, Method method, Object... params) {
        // 自定义键生成逻辑
    }
}
```

### 2. 自定义缓存实现

```java
public class CustomCache<K, V> implements Cache<K, V> {
    // 自定义缓存实现
}
```

### 3. 事件监听机制

```java
@Component
public class CacheEventListener {
    @EventListener
    public void onCacheEvent(CacheEvent event) {
        // 处理缓存事件
    }
}
```

## 七、实现注意事项

1. **性能优化**：
   - 使用高效的序列化方式
   - 批量操作优化
   - 连接池管理

2. **线程安全**：
   - 所有实现保证线程安全
   - 并发访问优化

3. **异常处理**：
   - 缓存访问异常不应影响业务逻辑
   - 降级机制设计

4. **可测试性**：
   - 提供模拟实现供测试使用
   - 单元测试覆盖关键功能

## 八、与JetCache的对比

1. **相同点**：
   - 统一的缓存抽象
   - 多级缓存支持
   - 注解方式使用
   - 自动刷新机制

2. **不同点**：
   - 更简洁的模块设计
   - 更现代的API设计
   - 更强的类型安全
   - 更完善的文档和示例

## 九、多级缓存设计

### 1. 多级缓存的实现原理

多级缓存结合了本地缓存和远程缓存的优势，既保证了高性能，又提供了数据共享能力：

```
+---------------------+        +---------------------+
|  应用实例1           |        |  应用实例2           |
|  +-------------+    |        |  +-------------+    |
|  | 本地缓存(L1) |    |        |  | 本地缓存(L1) |    |
|  +-------------+    |        |  +-------------+    |
+---------------------+        +---------------------+
          ↓                              ↓
          |                              |
          ↓                              ↓
    +----------------------------------+
    |        远程缓存(L2)(Redis)        |
    +----------------------------------+
```

#### 核心类设计

```java
public class MultiLevelCache<K, V> implements Cache<K, V> {
    private final String name;
    private final Cache<K, V> localCache;
    private final Cache<K, V> remoteCache;
    private final ConsistencyStrategy consistencyStrategy;
    private final CacheConfig config;
    
    // 构造函数、访问器等...
    
    @Override
    public V get(K key) {
        return consistencyStrategy.onGet(name, localCache, remoteCache, key);
    }
    
    @Override
    public void put(K key, V value, Duration ttl) {
        consistencyStrategy.onPut(name, localCache, remoteCache, key, value, ttl);
    }
    
    @Override
    public boolean remove(K key) {
        consistencyStrategy.onRemove(name, localCache, remoteCache, key);
        return true;
    }
    
    // 其他方法实现...
}
```

### 2. 一致性策略设计

为了支持多种一致性策略并提供良好的扩展性，我们设计了一致性策略接口：

```java
public interface ConsistencyStrategy {
    <K, V> void onPut(String cacheName, Cache<K, V> localCache, Cache<K, V> remoteCache, 
                      K key, V value, Duration ttl);
    
    <K, V> void onRemove(String cacheName, Cache<K, V> localCache, Cache<K, V> remoteCache, K key);
    
    <K, V> V onGet(String cacheName, Cache<K, V> localCache, Cache<K, V> remoteCache, K key);
}
```

#### 写同步策略 (Write-Through)

默认实现的写同步策略，保证了操作的有序性和一致性：

```java
public class WriteThroughStrategy implements ConsistencyStrategy {
    private final RedisPubSubService pubSubService;
    
    @Override
    public <K, V> void onPut(String cacheName, Cache<K, V> localCache, Cache<K, V> remoteCache, 
                          K key, V value, Duration ttl) {
        // 1. 先写远程缓存
        remoteCache.put(key, value, ttl);
        
        // 2. 再写本地缓存
        localCache.put(key, value, ttl);
        
        // 3. 发布缓存更新消息，通知其他节点
        pubSubService.publish(new CacheUpdateMessage(cacheName, key));
    }
    
    @Override
    public <K, V> void onRemove(String cacheName, Cache<K, V> localCache, Cache<K, V> remoteCache, K key) {
        // 1. 先从远程缓存删除
        remoteCache.remove(key);
        
        // 2. 再从本地缓存删除
        localCache.remove(key);
        
        // 3. 发布缓存删除消息
        pubSubService.publish(new CacheInvalidateMessage(cacheName, key));
    }
    
    @Override
    public <K, V> V onGet(String cacheName, Cache<K, V> localCache, Cache<K, V> remoteCache, K key) {
        // 1. 先查本地缓存
        V value = localCache.get(key);
        if (value != null) {
            return value;
        }
        
        // 2. 本地未命中，查远程缓存
        value = remoteCache.get(key);
        if (value != null) {
            // 将远程缓存的值放入本地缓存
            localCache.put(key, value);
        }
        
        return value;
    }
}
```

### 3. 一致性维护机制

#### 消息通知机制

基于Redis的发布/订阅功能实现的缓存同步通知机制：

```java
public class RedisPubSubService {
    private final StringRedisTemplate redisTemplate;
    private final Map<String, MessageListenerContainer> containers = new ConcurrentHashMap<>();
    
    public void subscribe(String channel, MessageListener listener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisTemplate.getConnectionFactory());
        container.addMessageListener(listener, new ChannelTopic(channel));
        container.afterPropertiesSet();
        container.start();
        
        containers.put(channel, container);
    }
    
    public void publish(CacheMessage message) {
        String channel = "cyx-cache:" + message.getCacheName();
        redisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(message));
    }
}

// 消息监听器
public class CacheMessageListener implements MessageListener {
    private final Cache<Object, Object> localCache;
    private final String instanceId = UUID.randomUUID().toString(); // 实例唯一标识
    
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            CacheMessage cacheMessage = objectMapper.readValue(message.getBody(), CacheMessage.class);
            
            // 仅处理来自其他实例的消息，避免循环处理
            if (!cacheMessage.getInstanceId().equals(instanceId)) {
                // 使本地缓存中的数据失效
                localCache.remove(cacheMessage.getKey());
            }
        } catch (Exception e) {
            log.error("处理缓存消息异常", e);
        }
    }
}
```

#### 缓存消息定义

```java
public abstract class CacheMessage {
    private final String cacheName;
    private final Object key;
    private final String instanceId;
    
    // getter方法...
}

public class CacheUpdateMessage extends CacheMessage {
    // 缓存更新消息
}

public class CacheInvalidateMessage extends CacheMessage {
    // 缓存失效消息
}
```

### 4. 配置扩展点

为支持不同一致性策略的配置及扩展，设计了以下配置结构：

```java
public enum ConsistencyMode {
    WRITE_THROUGH,  // 写同步策略
    WRITE_BACK,     // 写回策略(未来扩展)
    READ_ONLY       // 只读策略(未来扩展)
}

public class CacheConfig {
    // 基本配置
    private String name;
    private Duration expire;
    private CacheType cacheType;
    
    // 一致性配置
    private ConsistencyMode consistencyMode = ConsistencyMode.WRITE_THROUGH; // 默认使用写同步
    private boolean syncLocal = true;  // 是否同步本地缓存
    
    // 构建器模式...
    public static Builder newBuilder(String name) {
        return new Builder(name);
    }
    
    public static class Builder {
        private final CacheConfig config = new CacheConfig();
        
        public Builder(String name) {
            config.name = name;
        }
        
        public Builder cacheType(CacheType cacheType) {
            config.cacheType = cacheType;
            return this;
        }
        
        public Builder expire(Duration expire) {
            config.expire = expire;
            return this;
        }
        
        public Builder consistencyMode(ConsistencyMode mode) {
            config.consistencyMode = mode;
            return this;
        }
        
        public Builder syncLocal(boolean syncLocal) {
            config.syncLocal = syncLocal;
            return this;
        }
        
        public CacheConfig build() {
            return config;
        }
    }
}
```

### 5. 策略工厂

为支持策略的动态选择和创建，实现了策略工厂类：

```java
@Component
public class ConsistencyStrategyFactory {
    @Autowired
    private RedisPubSubService pubSubService;
    
    public ConsistencyStrategy createStrategy(CacheConfig config) {
        switch (config.getConsistencyMode()) {
            case WRITE_THROUGH:
                return new WriteThroughStrategy(pubSubService);
            case WRITE_BACK:
                // 未来扩展：写回策略
                throw new UnsupportedOperationException("写回策略尚未实现");
            case READ_ONLY:
                // 未来扩展：只读策略
                throw new UnsupportedOperationException("只读策略尚未实现");
            default:
                throw new IllegalArgumentException("不支持的一致性模式: " + config.getConsistencyMode());
        }
    }
}
```

### 6. 使用示例

在实际应用中使用多级缓存的示例：

```java
@Service
public class UserService {
    @Autowired
    private CacheManager cacheManager;
    private Cache<Long, User> userCache;
    
    @PostConstruct
    public void init() {
        // 创建使用写同步策略的两级缓存
        CacheConfig config = CacheConfig.newBuilder("userCache")
            .cacheType(CacheType.BOTH)  // 两级缓存
            .expire(Duration.ofHours(1))
            .consistencyMode(ConsistencyMode.WRITE_THROUGH) // 写同步策略
            .syncLocal(true)  // 启用跨节点本地缓存同步
            .build();
            
        userCache = cacheManager.getOrCreateCache(config);
    }
    
    public User getUser(Long userId) {
        return userCache.computeIfAbsent(userId, this::loadUserFromDatabase);
    }
    
    public void updateUser(User user) {
        // 先更新数据库
        saveUserToDatabase(user);
        
        // 再更新缓存
        userCache.put(user.getId(), user);
    }
    
    public void deleteUser(Long userId) {
        // 先删除数据库数据
        deleteUserFromDatabase(userId);
        
        // 再删除缓存
        userCache.remove(userId);
    }
}
```

### 7. 未来扩展方向

基于当前的一致性策略扩展点，未来可以实现以下策略：

#### 1) 写回策略 (Write-Back)

适用于写入频繁、对一致性要求不高的场景：

```java
public class WriteBackStrategy implements ConsistencyStrategy {
    private final ScheduledExecutorService executor;
    private final Map<Object, Object> writeBackQueue;
    
    @Override
    public <K, V> void onPut(String cacheName, Cache<K, V> localCache, Cache<K, V> remoteCache, 
                          K key, V value, Duration ttl) {
        // 1. 先写本地缓存
        localCache.put(key, value, ttl);
        
        // 2. 将更新加入写回队列
        writeBackQueue.put(key, value);
    }
    
    // 定期批量刷新远程缓存
    private void flushWriteBackQueue() {
        // 批量更新远程缓存
    }
}
```

#### 2) 只读策略 (Read-Only)

适用于读多写少的场景：

```java
public class ReadOnlyStrategy implements ConsistencyStrategy {
    @Override
    public <K, V> void onPut(String cacheName, Cache<K, V> localCache, Cache<K, V> remoteCache, 
                          K key, V value, Duration ttl) {
        // 1. 只更新远程缓存
        remoteCache.put(key, value, remoteExpire);
        
        // 2. 不主动更新本地缓存，只通过订阅机制使本地缓存失效
        pubSubService.publish(new CacheUpdateMessage(cacheName, key));
    }
}
```

## 十、最终设计方案

经过对模块结构的评估和优化，以下是cyx-cache框架的最终设计方案，特别强调了多级缓存的实现方式和各模块的职责分配。

### 1. 优化后的模块架构

cyx-cache依然保持三模块结构，但对职责进行了更合理的划分：

```
+---------------------------------------------+
| cyx-cache-core                              |
| - 缓存接口(Cache, CacheManager)             |
| - 本地缓存实现                               |
| - 多级缓存抽象(MultiLevelCache)              |
| - 一致性策略接口                             |
| - 缓存通知接口                               |
+---------------------------------------------+
                    ↑
                    |
+---------------------------------------------+
| cyx-cache-redis                             |
| - Redis缓存实现                              |
| - Redis消息通知实现                          |
| - Redis特有功能(分布式锁等)                   |
+---------------------------------------------+
                    ↑
                    |
+---------------------------------------------+
| cyx-cache-spring-boot-starter               |
| - Spring Boot集成                           |
| - 自动配置                                   |
| - 注解支持                                   |
+---------------------------------------------+
```

这种架构将多级缓存的核心逻辑放在core模块中，同时将具体实现（如Redis）作为支持的方式，实现了更好的关注点分离。

### 2. cyx-cache-core模块详细设计

#### 2.1 核心接口

```java
public interface Cache<K, V> {
    V get(K key);
    void put(K key, V value);
    void put(K key, V value, Duration ttl);
    boolean remove(K key);
    void clear();
    V computeIfAbsent(K key, Function<K, V> loader);
    V computeIfAbsent(K key, Function<K, V> loader, Duration ttl);
    boolean tryLock(K key, Duration timeout);
    void unlock(K key);
    boolean tryLockAndRun(K key, Duration timeout, Runnable action);
    CacheStats stats();
}

public interface CacheManager {
    <K, V> Cache<K, V> getCache(String name);
    <K, V> Cache<K, V> createCache(CacheConfig config);
    <K, V> Cache<K, V> getOrCreateCache(CacheConfig config);
    void removeCache(String name);
    Collection<String> getCacheNames();
}
```

#### 2.2 缓存通知接口

为了支持多级缓存的一致性，定义缓存事件通知接口：

```java
public interface CacheNotifier {
    void notifyUpdate(String cacheName, Object key);
    void notifyRemove(String cacheName, Object key);
    void subscribe(String cacheName, CacheEventListener listener);
}

public interface CacheEventListener {
    void onEvent(CacheEvent event);
}

public abstract class CacheEvent {
    private final String cacheName;
    private final Object key;
    private final CacheEventType eventType;
    private final String instanceId;
    
    // getter方法...
}

public enum CacheEventType {
    UPDATE, REMOVE
}
```

#### 2.3 多级缓存实现

```java
public class MultiLevelCache<K, V> implements Cache<K, V> {
    private final String name;
    private final Cache<K, V> localCache;
    private final Cache<K, V> remoteCache;
    private final ConsistencyStrategy consistencyStrategy;
    private final CacheConfig config;
    private final CacheNotifier notifier;
    
    public MultiLevelCache(String name, Cache<K, V> localCache, Cache<K, V> remoteCache,
                          ConsistencyStrategy strategy, CacheConfig config, CacheNotifier notifier) {
        this.name = name;
        this.localCache = localCache;
        this.remoteCache = remoteCache;
        this.consistencyStrategy = strategy;
        this.config = config;
        this.notifier = notifier;
    }
    
    @Override
    public V get(K key) {
        return consistencyStrategy.onGet(name, localCache, remoteCache, key);
    }
    
    @Override
    public void put(K key, V value, Duration ttl) {
        consistencyStrategy.onPut(name, localCache, remoteCache, key, value, ttl, notifier);
    }
    
    @Override
    public boolean remove(K key) {
        consistencyStrategy.onRemove(name, localCache, remoteCache, key, notifier);
        return true;
    }
    
    // 其他方法实现...
}
```

#### 2.4 一致性策略接口

```java
public interface ConsistencyStrategy {
    <K, V> void onPut(String cacheName, Cache<K, V> localCache, Cache<K, V> remoteCache, 
                      K key, V value, Duration ttl, CacheNotifier notifier);
    
    <K, V> void onRemove(String cacheName, Cache<K, V> localCache, Cache<K, V> remoteCache, 
                         K key, CacheNotifier notifier);
    
    <K, V> V onGet(String cacheName, Cache<K, V> localCache, Cache<K, V> remoteCache, K key);
}
```

#### 2.5 默认写同步策略实现

```java
public class WriteThroughStrategy implements ConsistencyStrategy {
    @Override
    public <K, V> void onPut(String cacheName, Cache<K, V> localCache, Cache<K, V> remoteCache, 
                           K key, V value, Duration ttl, CacheNotifier notifier) {
        // 1. 先写远程缓存
        remoteCache.put(key, value, ttl);
        
        // 2. 再写本地缓存
        localCache.put(key, value, ttl);
        
        // 3. 发布缓存更新消息，通知其他节点
        if (notifier != null) {
            notifier.notifyUpdate(cacheName, key);
        }
    }
    
    @Override
    public <K, V> void onRemove(String cacheName, Cache<K, V> localCache, Cache<K, V> remoteCache, 
                              K key, CacheNotifier notifier) {
        // 1. 先从远程缓存删除
        remoteCache.remove(key);
        
        // 2. 再从本地缓存删除
        localCache.remove(key);
        
        // 3. 发布缓存删除消息
        if (notifier != null) {
            notifier.notifyRemove(cacheName, key);
        }
    }
    
    @Override
    public <K, V> V onGet(String cacheName, Cache<K, V> localCache, Cache<K, V> remoteCache, K key) {
        // 1. 先查本地缓存
        V value = localCache.get(key);
        if (value != null) {
            return value;
        }
        
        // 2. 本地未命中，查远程缓存
        value = remoteCache.get(key);
        if (value != null) {
            // 将远程缓存的值放入本地缓存
            localCache.put(key, value);
        }
        
        return value;
    }
}
```

#### 2.6 缓存管理器构建器

为了灵活构建不同类型的缓存管理器，提供了构建器模式：

```java
public class CacheManagerBuilder {
    private CacheManager localCacheManager;
    private CacheManager remoteCacheManager;
    private CacheNotifier notifier;
    private ConsistencyStrategyFactory strategyFactory;
    
    public CacheManagerBuilder localCacheManager(CacheManager localCacheManager) {
        this.localCacheManager = localCacheManager;
        return this;
    }
    
    public CacheManagerBuilder remoteCacheManager(CacheManager remoteCacheManager) {
        this.remoteCacheManager = remoteCacheManager;
        return this;
    }
    
    public CacheManagerBuilder notifier(CacheNotifier notifier) {
        this.notifier = notifier;
        return this;
    }
    
    public CacheManagerBuilder strategyFactory(ConsistencyStrategyFactory factory) {
        this.strategyFactory = factory;
        return this;
    }
    
    public CacheManager build() {
        if (strategyFactory == null) {
            strategyFactory = new DefaultConsistencyStrategyFactory();
        }
        
        // 根据提供的组件选择合适的缓存管理器实现
        if (remoteCacheManager == null) {
            return localCacheManager; // 仅本地缓存
        } else if (localCacheManager == null) {
            return remoteCacheManager; // 仅远程缓存
        } else {
            return new MultiLevelCacheManager(
                localCacheManager, 
                remoteCacheManager, 
                notifier, 
                strategyFactory);
        }
    }
}
```

#### 2.7 一致性策略工厂

```java
public interface ConsistencyStrategyFactory {
    ConsistencyStrategy createStrategy(CacheConfig config);
}

public class DefaultConsistencyStrategyFactory implements ConsistencyStrategyFactory {
    @Override
    public ConsistencyStrategy createStrategy(CacheConfig config) {
        switch (config.getConsistencyMode()) {
            case WRITE_THROUGH:
                return new WriteThroughStrategy();
            case WRITE_BACK:
                return new WriteBackStrategy();
            case READ_ONLY:
                return new ReadOnlyStrategy();
            default:
                throw new IllegalArgumentException("不支持的一致性模式: " + config.getConsistencyMode());
        }
    }
}
```

#### 2.8 多级缓存管理器

```java
public class MultiLevelCacheManager implements CacheManager {
    private final CacheManager localCacheManager;
    private final CacheManager remoteCacheManager;
    private final CacheNotifier notifier;
    private final ConsistencyStrategyFactory strategyFactory;
    private final Map<String, Cache<?, ?>> caches = new ConcurrentHashMap<>();
    
    // 构造函数...
    
    @Override
    public <K, V> Cache<K, V> getOrCreateCache(CacheConfig config) {
        return (Cache<K, V>) caches.computeIfAbsent(config.getName(), name -> {
            // 创建本地和远程缓存
            Cache<K, V> localCache = localCacheManager.getOrCreateCache(config.localConfig());
            Cache<K, V> remoteCache = remoteCacheManager.getOrCreateCache(config.remoteConfig());
            
            // 选择一致性策略
            ConsistencyStrategy strategy = strategyFactory.createStrategy(config);
            
            // 创建多级缓存
            MultiLevelCache<K, V> multiLevelCache = 
                new MultiLevelCache<>(name, localCache, remoteCache, strategy, config, notifier);
            
            // 如果配置了同步本地缓存，订阅消息通知
            if (config.isSyncLocal() && config.getCacheType() == CacheType.BOTH && notifier != null) {
                notifier.subscribe(name, event -> {
                    if (event.getInstanceId().equals(getInstanceId())) {
                        return; // 忽略自己发出的事件
                    }
                    
                    switch (event.getEventType()) {
                        case UPDATE:
                        case REMOVE:
                            localCache.remove(event.getKey());
                            break;
                    }
                });
            }
            
            return multiLevelCache;
        });
    }
    
    // 其他方法实现...
}
```

### 3. cyx-cache-redis模块详细设计

#### 3.1 Redis缓存实现

```java
public class RedisCache<K, V> implements Cache<K, V> {
    private final RedisTemplate<String, V> redisTemplate;
    private final String name;
    private final Duration defaultTtl;
    private final KeyConvertor<K> keyConvertor;
    
    @Override
    public V get(K key) {
        String redisKey = generateRedisKey(key);
        return redisTemplate.opsForValue().get(redisKey);
    }
    
    @Override
    public void put(K key, V value, Duration ttl) {
        String redisKey = generateRedisKey(key);
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            redisTemplate.opsForValue().set(redisKey, value);
        } else {
            redisTemplate.opsForValue().set(redisKey, value, ttl);
        }
    }
    
    @Override
    public boolean remove(K key) {
        String redisKey = generateRedisKey(key);
        return Boolean.TRUE.equals(redisTemplate.delete(redisKey));
    }
    
    // 其他方法实现...
    
    private String generateRedisKey(K key) {
        return name + ":" + keyConvertor.convert(key);
    }
}
```

#### 3.2 Redis缓存管理器

```java
public class RedisCacheManager implements CacheManager {
    private final RedisConnectionFactory connectionFactory;
    private final Map<String, Cache<?, ?>> caches = new ConcurrentHashMap<>();
    private final KeyConvertorFactory keyConvertorFactory;
    private final ValueEncoderFactory valueEncoderFactory;
    private final ValueDecoderFactory valueDecoderFactory;
    
    // 构造函数...
    
    @Override
    public <K, V> Cache<K, V> getOrCreateCache(CacheConfig config) {
        return (Cache<K, V>) caches.computeIfAbsent(config.getName(), name -> {
            RedisTemplate<String, V> redisTemplate = createRedisTemplate(config);
            KeyConvertor<K> keyConvertor = keyConvertorFactory.getConvertor(config.getKeyConvertor());
            
            return new RedisCache<>(redisTemplate, name, config.getExpire(), keyConvertor);
        });
    }
    
    // 其他方法实现...
}
```

#### 3.3 Redis缓存通知实现

```java
public class RedisCacheNotifier implements CacheNotifier {
    private final StringRedisTemplate redisTemplate;
    private final Map<String, MessageListenerContainer> containers = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final String instanceId;
    
    public RedisCacheNotifier(RedisConnectionFactory factory) {
        this.redisTemplate = new StringRedisTemplate(factory);
        this.objectMapper = new ObjectMapper();
        this.instanceId = UUID.randomUUID().toString();
    }
    
    @Override
    public void notifyUpdate(String cacheName, Object key) {
        String channel = "cyx-cache:" + cacheName;
        CacheUpdateEvent event = new CacheUpdateEvent(cacheName, key, instanceId);
        try {
            redisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            throw new CacheException("发送缓存更新消息失败", e);
        }
    }
    
    @Override
    public void notifyRemove(String cacheName, Object key) {
        String channel = "cyx-cache:" + cacheName;
        CacheRemoveEvent event = new CacheRemoveEvent(cacheName, key, instanceId);
        try {
            redisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            throw new CacheException("发送缓存删除消息失败", e);
        }
    }
    
    @Override
    public void subscribe(String cacheName, CacheEventListener listener) {
        String channel = "cyx-cache:" + cacheName;
        
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisTemplate.getConnectionFactory());
        container.addMessageListener(new MessageListener() {
            @Override
            public void onMessage(Message message, byte[] pattern) {
                try {
                    CacheEvent event = objectMapper.readValue(message.getBody(), CacheEvent.class);
                    listener.onEvent(event);
                } catch (Exception e) {
                    log.error("处理缓存消息异常", e);
                }
            }
        }, new ChannelTopic(channel));
        
        container.afterPropertiesSet();
        container.start();
        
        containers.put(channel, container);
    }
}
```

### 4. cyx-cache-spring-boot-starter模块详细设计

#### 4.1 自动配置

```java
@Configuration
@EnableConfigurationProperties(cyx-cacheProperties.class)
public class cyx-cacheAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public LocalCacheManager localCacheManager(cyx-cacheProperties properties) {
        // 根据配置创建本地缓存管理器
        if ("caffeine".equals(properties.getLocal().getDefault().getType())) {
            return new CaffeineCacheManager();
        } else {
            return new LinkedHashMapCacheManager();
        }
    }
    
    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    @ConditionalOnMissingBean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory factory, cyx-cacheProperties properties) {
        // 创建Redis缓存管理器
        return new RedisCacheManager(factory, properties);
    }
    
    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    @ConditionalOnMissingBean
    public CacheNotifier redisCacheNotifier(RedisConnectionFactory factory) {
        // 创建Redis缓存通知器
        return new RedisCacheNotifier(factory);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ConsistencyStrategyFactory consistencyStrategyFactory() {
        return new DefaultConsistencyStrategyFactory();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public CacheManager cacheManager(
            LocalCacheManager localCacheManager,
            @Autowired(required = false) RedisCacheManager redisCacheManager,
            @Autowired(required = false) CacheNotifier cacheNotifier,
            ConsistencyStrategyFactory strategyFactory) {
        
        return new CacheManagerBuilder()
            .localCacheManager(localCacheManager)
            .remoteCacheManager(redisCacheManager)
            .notifier(cacheNotifier)
            .strategyFactory(strategyFactory)
            .build();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public CacheInterceptor cacheInterceptor(CacheManager cacheManager) {
        return new CacheInterceptor(cacheManager);
    }
    
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public BeanFactoryPointcutAdvisor cacheAdvisor(CacheInterceptor interceptor) {
        AnnotationMatchingPointcut pointcut = new AnnotationMatchingPointcut(null, Cached.class);
        BeanFactoryPointcutAdvisor advisor = new BeanFactoryPointcutAdvisor();
        advisor.setPointcut(pointcut);
        advisor.setAdvice(interceptor);
        return advisor;
    }
}
```

#### 4.2 属性配置类

```java
@ConfigurationProperties(prefix = "cyx-cache")
public class cyx-cacheProperties {
    private int statIntervalMinutes = 15;
    private boolean areaInCacheName = false;
    private Map<String, LocalConfig> local = new HashMap<>();
    private Map<String, RedisConfig> remote = new HashMap<>();
    
    // getter和setter方法...
    
    public static class LocalConfig {
        private String type = "caffeine";
        private int limit = 200;
        private String keyConvertor = "fastjson";
        
        // getter和setter方法...
    }
    
    public static class RedisConfig {
        private String type = "lettuce";
        private String host = "localhost";
        private int port = 6379;
        private int database = 0;
        private String keyConvertor = "fastjson";
        private String valueEncoder = "java";
        private String valueDecoder = "java";
        private String broadcastChannel;
        private PoolConfig poolConfig = new PoolConfig();
        
        // getter和setter方法...
        
        public static class PoolConfig {
            private int minIdle = 5;
            private int maxIdle = 20;
            private int maxTotal = 50;
            
            // getter和setter方法...
        }
    }
}
```

### 5. 不同场景的使用示例

#### 5.1 仅使用本地缓存

```java
// 创建本地缓存管理器
LocalCacheManager cacheManager = new CaffeineCacheManager();

// 获取或创建缓存
CacheConfig config = CacheConfig.newBuilder("userCache")
    .cacheType(CacheType.LOCAL)
    .localLimit(1000)
    .expire(Duration.ofMinutes(30))
    .build();

Cache<Long, User> userCache = cacheManager.getOrCreateCache(config);

// 使用缓存
User user = userCache.get(123L);
if (user == null) {
    user = loadUserFromDatabase(123L);
    userCache.put(123L, user);
}
```

#### 5.2 仅使用Redis缓存

```java
// 创建Redis连接工厂
RedisConnectionFactory factory = createRedisConnectionFactory();

// 创建Redis缓存管理器
RedisCacheManager cacheManager = new RedisCacheManager(factory);

// 获取或创建缓存
CacheConfig config = CacheConfig.newBuilder("userCache")
    .cacheType(CacheType.REMOTE)
    .expire(Duration.ofHours(1))
    .build();

Cache<Long, User> userCache = cacheManager.getOrCreateCache(config);

// 使用缓存
User user = userCache.computeIfAbsent(123L, this::loadUserFromDatabase);
```

#### 5.3 使用两级缓存（写同步策略）

```java
// 创建本地缓存管理器
LocalCacheManager localCacheManager = new CaffeineCacheManager();

// 创建Redis缓存管理器和通知器
RedisConnectionFactory factory = createRedisConnectionFactory();
RedisCacheManager remoteCacheManager = new RedisCacheManager(factory);
RedisCacheNotifier notifier = new RedisCacheNotifier(factory);

// 创建多级缓存管理器
CacheManager cacheManager = new CacheManagerBuilder()
    .localCacheManager(localCacheManager)
    .remoteCacheManager(remoteCacheManager)
    .notifier(notifier)
    .build();

// 配置两级缓存，使用写同步策略
CacheConfig config = CacheConfig.newBuilder("userCache")
    .cacheType(CacheType.BOTH)
    .localLimit(1000)
    .expire(Duration.ofHours(1))
    .consistencyMode(ConsistencyMode.WRITE_THROUGH)
    .syncLocal(true)
    .build();

Cache<Long, User> userCache = cacheManager.getOrCreateCache(config);

// 使用缓存
User user = userCache.get(123L); // 先查本地，再查远程
if (user == null) {
    user = loadUserFromDatabase(123L);
    userCache.put(123L, user); // 先更新远程，再更新本地，然后通知其他节点
}
```

#### 5.4 使用Spring Boot集成

```java
@SpringBootApplication
@EnableMethodCache(basePackages = "com.example.service")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

@Service
public class UserService {
    @Autowired
    private CacheManager cacheManager;
    private Cache<Long, User> userCache;
    
    @PostConstruct
    public void init() {
        CacheConfig config = CacheConfig.newBuilder("userCache")
            .cacheType(CacheType.BOTH)
            .expire(Duration.ofHours(1))
            .consistencyMode(ConsistencyMode.WRITE_THROUGH)
            .build();
            
        userCache = cacheManager.getOrCreateCache(config);
    }
    
    @Cached(name="user:", key="#userId", expire=3600)
    public User getUser(long userId) {
        return loadUserFromDatabase(userId);
    }
    
    @CacheUpdate(name="user:", key="#user.id", value="#user")
    public void updateUser(User user) {
        saveUserToDatabase(user);
    }
    
    @CacheInvalidate(name="user:", key="#userId")
    public void deleteUser(long userId) {
        deleteUserFromDatabase(userId);
    }
    
    // 也可以直接使用API
    public User getUserById(long userId) {
        return userCache.computeIfAbsent(userId, this::loadUserFromDatabase);
    }
}
```

### 6. 配置文件示例

```yaml
cyx-cache:
  stats-interval: 60  # 统计间隔(秒)
  local:
    default:
      type: caffeine   # 本地缓存类型
      limit: 1000      # 本地缓存大小限制
      key-convertor: fastjson
  remote:
    default:
      type: redis      # 远程缓存类型
      host: localhost
      port: 6379
      database: 0
      key-convertor: fastjson
      value-encoder: java
      value-decoder: java
      broadcast-channel: app1  # 用于多实例间通信
      pool-config:
        min-idle: 5
        max-idle: 20
        max-total: 50
```

### 7. 设计优势总结

1. **职责清晰**：
   - core模块包含所有抽象和基础功能
   - redis模块专注于Redis实现
   - spring-boot-starter模块提供自动配置

2. **灵活支持多种缓存场景**：
   - 仅本地缓存
   - 仅Redis缓存
   - 两级缓存（多种一致性策略）

3. **统一的API**：
   - 所有缓存实现共享相同的API
   - 无缝切换不同缓存实现

4. **可扩展性**：
   - 一致性策略扩展点
   - 缓存通知接口
   - 序列化扩展

5. **自动适应**：
   - 根据环境自动选择合适的缓存实现
   - 条件配置根据依赖自动激活

这种设计既满足了简单使用场景的需求，也为复杂场景提供了完整的解决方案，同时保持了良好的可扩展性和可维护性。
