package com.easy.cache.aop;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.easy.cache.annotation.Cached;
import com.easy.cache.annotation.CacheInvalidate;
import com.easy.cache.annotation.CacheUpdate;
import com.easy.cache.core.CacheManager;
import com.easy.cache.core.CacheProcessor;
import com.easy.cache.core.CacheType;

/**
 * 缓存切面测试类
 */
public class CacheAspectTest {
    
    private CacheAspect cacheAspect;
    
    @Mock
    private CacheManager cacheManager;
    
    @Mock
    private CacheProcessor cacheProcessor;
    
    @Mock
    private ProceedingJoinPoint joinPoint;
    
    @Mock
    private MethodSignature methodSignature;
    
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 创建缓存切面
        cacheAspect = new CacheAspect(cacheManager);
        
        // 配置模拟行为
        when(joinPoint.getSignature()).thenReturn(methodSignature);
    }
    
    @Test
    public void testAroundCached() throws Throwable {
        // 准备数据
        Object[] args = new Object[] { 1L };
        TestUser result = new TestUser(1L, "张三");
        
        // 获取带有@Cached注解的方法
        Method method = TestService.class.getMethod("getUserById", long.class);
        Cached cached = method.getAnnotation(Cached.class);
        
        // 配置模拟行为
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn(result);
        
        // 执行切面方法
        Object cachedResult = cacheAspect.aroundCached(joinPoint);
        
        // 验证
        assertEquals(result, cachedResult);
    }
    
    @Test
    public void testAroundCacheUpdate() throws Throwable {
        // 准备数据
        TestUser user = new TestUser(1L, "张三");
        Object[] args = new Object[] { user };
        
        // 获取带有@CacheUpdate注解的方法
        Method method = TestService.class.getMethod("updateUser", TestUser.class);
        CacheUpdate update = method.getAnnotation(CacheUpdate.class);
        
        // 配置模拟行为
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn(user);
        
        // 执行切面方法
        Object result = cacheAspect.aroundCacheUpdate(joinPoint);
        
        // 验证
        assertEquals(user, result);
    }
    
    @Test
    public void testAroundCacheInvalidate() throws Throwable {
        // 准备数据
        Object[] args = new Object[] { 1L };
        
        // 获取带有@CacheInvalidate注解的方法
        Method method = TestService.class.getMethod("deleteUser", long.class);
        CacheInvalidate invalidate = method.getAnnotation(CacheInvalidate.class);
        
        // 配置模拟行为
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn(true);
        
        // 执行切面方法
        Object result = cacheAspect.aroundCacheInvalidate(joinPoint);
        
        // 验证
        assertEquals(true, result);
    }
    
    /**
     * 测试用户类
     */
    static class TestUser {
        private long id;
        private String name;
        
        public TestUser(long id, String name) {
            this.id = id;
            this.name = name;
        }
        
        public long getId() {
            return id;
        }
        
        public String getName() {
            return name;
        }
    }
    
    /**
     * 测试服务类
     */
    static class TestService {
        
        @Cached(key = "#id", expire = 3600, cacheType = CacheType.LOCAL)
        public TestUser getUserById(long id) {
            return new TestUser(id, "用户" + id);
        }
        
        @CacheUpdate(key = "#user.id", value = "#user")
        public TestUser updateUser(TestUser user) {
            return user;
        }
        
        @CacheInvalidate(key = "#id")
        public boolean deleteUser(long id) {
            return true;
        }
    }
} 