package com.easy.cache.annotation.aop;

import com.easy.cache.annotation.CacheInvalidate;
import com.easy.cache.annotation.CacheUpdate;
import com.easy.cache.annotation.Cached;
import com.easy.cache.core.Cache;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 缓存拦截器
 * <p>
 * 处理@Cached、@CacheInvalidate、@CacheUpdate注解
 */
@Slf4j
public class CacheInterceptor implements MethodInterceptor {

    private final CacheManager cacheManager;
    private final KeyGenerator defaultKeyGenerator;
    private final Map<String, KeyGenerator> keyGenerators;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final LocalVariableTableParameterNameDiscoverer parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();
    private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>();
    private final Map<Method, Lock> syncLocks = new ConcurrentHashMap<>();

    public CacheInterceptor(CacheManager cacheManager, KeyGenerator defaultKeyGenerator,
            Map<String, KeyGenerator> keyGenerators) {
        this.cacheManager = cacheManager;
        this.defaultKeyGenerator = defaultKeyGenerator;
        this.keyGenerators = keyGenerators;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        Object[] arguments = invocation.getArguments();

        // 处理@Cached注解
        Cached cachedAnnotation = method.getAnnotation(Cached.class);
        if (cachedAnnotation != null) {
            return handleCachedAnnotation(invocation, cachedAnnotation, method, arguments);
        }

        // 执行方法
        Object result = invocation.proceed();

        // 处理@CacheUpdate注解
        CacheUpdate updateAnnotation = method.getAnnotation(CacheUpdate.class);
        if (updateAnnotation != null) {
            handleCacheUpdateAnnotation(updateAnnotation, method, arguments, result);
        }

        // 处理@CacheInvalidate注解
        CacheInvalidate invalidateAnnotation = method.getAnnotation(CacheInvalidate.class);
        if (invalidateAnnotation != null) {
            handleCacheInvalidateAnnotation(invalidateAnnotation, method, arguments);
        }

        return result;
    }

    /**
     * 处理@Cached注解
     */
    private Object handleCachedAnnotation(MethodInvocation invocation, Cached annotation, Method method,
            Object[] arguments)
            throws Throwable {
        // 检查条件
        if (!isConditionPassing(annotation.condition(), method, arguments, null)) {
            return invocation.proceed();
        }

        // 获取缓存区域和名称
        String area = getArea(annotation.area(), method);
        String name = getName(annotation.name(), method);

        // 生成缓存键
        Object key = generateKey(annotation.key(), annotation.keyGenerator(), method, arguments);
        if (key == null) {
            return invocation.proceed();
        }

        // 获取缓存
        Cache<Object, Object> cache = cacheManager.getCache(area, name, annotation.cacheType());
        if (cache == null) {
            log.warn("找不到缓存: area={}, name={}, type={}", area, name, annotation.cacheType());
            return invocation.proceed();
        }

        // 如果开启同步加载，使用锁保护方法执行
        if (annotation.syncLoad()) {
            return syncLoad(invocation, annotation, method, arguments, key, cache);
        } else {
            // 从缓存获取值
            Object value = cache.get(key);
            if (value != null) {
                return value;
            }

            // 执行方法
            value = invocation.proceed();

            // 检查unless条件
            if (value == null && !annotation.cacheNull()) {
                return value;
            }

            if (isConditionPassing(annotation.unless(), method, arguments, value)) {
                return value;
            }

            // 缓存结果
            long expireTime = annotation.expire();
            if (expireTime > 0) {
                cache.put(key, value, expireTime);
            } else {
                cache.put(key, value);
            }

            return value;
        }
    }

    /**
     * 同步加载缓存值
     */
    private Object syncLoad(MethodInvocation invocation, Cached annotation, Method method, Object[] arguments,
            Object key, Cache<Object, Object> cache) throws Throwable {
        // 获取或创建方法锁
        Lock lock = syncLocks.computeIfAbsent(method, k -> new ReentrantLock());

        try {
            // 先尝试从缓存获取
            Object value = cache.get(key);
            if (value != null) {
                return value;
            }

            // 获取锁并再次检查缓存
            boolean acquired = lock.tryLock(annotation.waitLockMillis(), TimeUnit.MILLISECONDS);
            if (!acquired) {
                log.warn("无法获取同步锁，回退到直接执行方法: method={}", method.getName());
                return invocation.proceed(); // 回退到直接执行
            }

            try {
                // 再次检查缓存
                value = cache.get(key);
                if (value != null) {
                    return value;
                }

                // 执行方法
                value = invocation.proceed();

                // 检查unless条件
                if (value == null && !annotation.cacheNull()) {
                    return value;
                }

                if (isConditionPassing(annotation.unless(), method, arguments, value)) {
                    return value;
                }

                // 缓存结果
                long expireTime = annotation.expire();
                if (expireTime > 0) {
                    cache.put(key, value, expireTime);
                } else {
                    cache.put(key, value);
                }

                return value;
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return invocation.proceed(); // 回退到直接执行
        }
    }

    /**
     * 处理@CacheUpdate注解
     */
    private void handleCacheUpdateAnnotation(CacheUpdate annotation, Method method, Object[] arguments, Object result) {
        // 检查条件
        if (!isConditionPassing(annotation.condition(), method, arguments, result)) {
            return;
        }

        // 检查unless条件
        if (isConditionPassing(annotation.unless(), method, arguments, result)) {
            return;
        }

        // 获取缓存区域和名称
        String area = getArea(annotation.area(), method);
        String name = getName(annotation.name(), method);

        // 生成缓存键
        Object key = generateKey(annotation.key(), annotation.keyGenerator(), method, arguments);
        if (key == null) {
            return;
        }

        // 生成缓存值
        Object value = evaluateExpression(annotation.value(), method, arguments, result);
        if (value == null) {
            return;
        }

        // 获取缓存
        Cache<Object, Object> cache = cacheManager.getCache(area, name, annotation.cacheType());
        if (cache == null) {
            log.warn("找不到缓存: area={}, name={}, type={}", area, name, annotation.cacheType());
            return;
        }

        // 更新缓存
        long expireTime = annotation.expire();
        if (expireTime > 0) {
            cache.put(key, value, expireTime);
        } else {
            cache.put(key, value);
        }
    }

    /**
     * 处理@CacheInvalidate注解
     */
    private void handleCacheInvalidateAnnotation(CacheInvalidate annotation, Method method, Object[] arguments) {
        // 检查条件
        if (!isConditionPassing(annotation.condition(), method, arguments, null)) {
            return;
        }

        // 获取缓存区域和名称
        String area = getArea(annotation.area(), method);
        String name = getName(annotation.name(), method);

        // 获取缓存
        Cache<Object, Object> cache = cacheManager.getCache(area, name, annotation.cacheType());
        if (cache == null) {
            log.warn("找不到缓存: area={}, name={}, type={}", area, name, annotation.cacheType());
            return;
        }

        if (annotation.allEntries()) {
            // 清空整个缓存
            cache.clear();
        } else {
            // 生成缓存键
            Object key = generateKey(annotation.key(), annotation.keyGenerator(), method, arguments);
            if (key == null) {
                return;
            }

            // 删除指定键的缓存
            cache.remove(key);
        }
    }

    /**
     * 检查条件是否满足
     */
    private boolean isConditionPassing(String condition, Method method, Object[] arguments, Object result) {
        if (condition == null || condition.isEmpty()) {
            return true;
        }

        Object value = evaluateExpression(condition, method, arguments, result);
        return value instanceof Boolean ? (Boolean) value : false;
    }

    /**
     * 获取缓存区域
     */
    private String getArea(String area, Method method) {
        if (area == null || area.isEmpty()) {
            return method.getDeclaringClass().getName();
        }
        return area;
    }

    /**
     * 获取缓存名称
     */
    private String getName(String name, Method method) {
        if (name == null || name.isEmpty()) {
            return method.getName();
        }
        return name;
    }

    /**
     * 生成缓存键
     */
    private Object generateKey(String key, String keyGeneratorName, Method method, Object[] arguments) {
        // 如果指定了键生成器，使用指定的键生成器
        if (keyGeneratorName != null && !keyGeneratorName.isEmpty()) {
            KeyGenerator keyGenerator = keyGenerators.get(keyGeneratorName);
            if (keyGenerator != null) {
                return keyGenerator.generate(method, arguments);
            } else {
                log.warn("找不到指定的键生成器: {}", keyGeneratorName);
            }
        }

        // 如果指定了键表达式，使用表达式计算键
        if (key != null && !key.isEmpty()) {
            return evaluateExpression(key, method, arguments, null);
        }

        // 使用默认键生成器
        return defaultKeyGenerator.generate(method, arguments);
    }

    /**
     * 计算表达式
     */
    private Object evaluateExpression(String expression, Method method, Object[] arguments, Object result) {
        if (expression == null || expression.isEmpty()) {
            return null;
        }

        // 创建表达式上下文
        EvaluationContext context = createEvaluationContext(method, arguments, result);

        // 获取或解析表达式
        Expression parsedExpression = expressionCache.computeIfAbsent(expression,
                expressionParser::parseExpression);

        try {
            return parsedExpression.getValue(context);
        } catch (Exception e) {
            log.error("计算表达式出错: {}", expression, e);
            return null;
        }
    }

    /**
     * 创建表达式上下文
     */
    private EvaluationContext createEvaluationContext(Method method, Object[] arguments, Object result) {
        StandardEvaluationContext context = new StandardEvaluationContext();

        // 添加方法参数
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], arguments[i]);
            }
        }

        // 添加方法参数，使用索引作为变量名
        for (int i = 0; i < arguments.length; i++) {
            context.setVariable("p" + i, arguments[i]);
        }

        // 添加方法参数数组
        context.setVariable("args", arguments);

        // 添加结果
        if (result != null) {
            context.setVariable("result", result);
        }

        return context;
    }
}