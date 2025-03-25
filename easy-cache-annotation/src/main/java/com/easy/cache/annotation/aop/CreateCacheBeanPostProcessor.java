package com.easy.cache.annotation.aop;

import com.easy.cache.annotation.CreateCache;
import com.easy.cache.core.Cache;
import com.easy.cache.core.CacheConfig;
import com.easy.cache.core.CacheLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;

/**
 * CreateCache注解的Bean后处理器
 * <p>
 * 处理@CreateCache注解，将缓存实例注入到字段中
 */
@Slf4j
public class CreateCacheBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware, Ordered {

    private BeanFactory beanFactory;
    private CacheManager cacheManager;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (cacheManager == null) {
            cacheManager = beanFactory.getBean(CacheManager.class);
        }

        ReflectionUtils.doWithFields(bean.getClass(), field -> {
            CreateCache annotation = field.getAnnotation(CreateCache.class);
            if (annotation != null) {
                processCreateCacheAnnotation(bean, field, annotation);
            }
        });

        return bean;
    }

    /**
     * 处理@CreateCache注解
     */
    private void processCreateCacheAnnotation(Object bean, Field field, CreateCache annotation) {
        // 获取缓存区域和名称
        String area = getArea(annotation.area(), field);
        String name = getName(annotation.name(), field);

        // 创建缓存配置
        CacheConfig config = createCacheConfig(annotation, field);

        // 检查是否有加载器
        String loaderName = annotation.loader();
        if (loaderName != null && !loaderName.isEmpty()) {
            try {
                CacheLoader<?, ?> loader = beanFactory.getBean(loaderName, CacheLoader.class);
                injectCacheWithLoader(bean, field, area, name, annotation.cacheType(), config, loader);
            } catch (BeansException e) {
                log.error("找不到指定的缓存加载器: {}", loaderName, e);
                injectCache(bean, field, area, name, annotation.cacheType());
            }
        } else {
            injectCache(bean, field, area, name, annotation.cacheType());
        }
    }

    /**
     * 创建缓存配置
     */
    private CacheConfig createCacheConfig(CreateCache annotation, Field field) {
        CacheConfig.CacheConfigBuilder builder = CacheConfig.builder()
                .name(getName(annotation.name(), field))
                .keyPrefix(annotation.keyPrefix())
                .allowNullValues(annotation.cacheNull());

        // 设置缓存类型
        com.easy.cache.core.CacheType cacheType;
        switch (annotation.cacheType()) {
            case LOCAL:
                cacheType = com.easy.cache.core.CacheType.LOCAL;
                break;
            case REMOTE:
                cacheType = com.easy.cache.core.CacheType.REMOTE;
                break;
            default:
                cacheType = com.easy.cache.core.CacheType.MULTILEVEL;
        }
        builder.cacheType(cacheType);

        // 设置过期时间
        long expire = annotation.expire();
        if (expire > 0) {
            TimeUnit timeUnit = annotation.timeUnit();
            long expireSeconds = TimeUnit.SECONDS.convert(expire, timeUnit);
            builder.localExpireSeconds(expireSeconds);
            builder.remoteExpireSeconds(expireSeconds);
        }

        // 设置本地缓存过期时间
        long localExpire = annotation.localExpire();
        if (localExpire > 0) {
            TimeUnit timeUnit = annotation.timeUnit();
            long localExpireSeconds = TimeUnit.SECONDS.convert(localExpire, timeUnit);
            builder.localExpireSeconds(localExpireSeconds);
        }

        // 设置远程缓存过期时间
        long remoteExpire = annotation.remoteExpire();
        if (remoteExpire > 0) {
            TimeUnit timeUnit = annotation.timeUnit();
            long remoteExpireSeconds = TimeUnit.SECONDS.convert(remoteExpire, timeUnit);
            builder.remoteExpireSeconds(remoteExpireSeconds);
        }

        // 设置本地缓存容量
        long localLimit = annotation.localLimit();
        if (localLimit > 0) {
            builder.localMaxSize(localLimit);
        }

        // 设置自动刷新
        if (annotation.refresh()) {
            long refreshInterval = annotation.refreshInterval();
            if (refreshInterval > 0) {
                TimeUnit refreshTimeUnit = annotation.refreshTimeUnit();
                long refreshSeconds = TimeUnit.SECONDS.convert(refreshInterval, refreshTimeUnit);
                builder.localRefreshSeconds(refreshSeconds);
                builder.remoteRefreshSeconds(refreshSeconds);
            }
        }

        // 设置序列化策略
        String serialPolicy = annotation.serialPolicy();
        if (serialPolicy != null && !serialPolicy.isEmpty()) {
            builder.remoteValueSerializer(serialPolicy);
        }

        // 设置统计
        builder.localRecordStats(annotation.enableStats());
        builder.remoteRecordStats(annotation.enableStats());

        // 设置弱引用键和软引用值
        builder.localWeakKeys(annotation.weakKeys());
        builder.localSoftValues(annotation.softValues());

        return builder.build();
    }

    /**
     * 注入缓存实例
     */
    private void injectCache(Object bean, Field field, String area, String name,
            com.easy.cache.annotation.CacheType cacheType) {
        try {
            // 获取缓存
            Cache<?, ?> cache = cacheManager.getCache(area, name, cacheType);

            // 设置字段可访问
            boolean isAccessible = field.isAccessible();
            try {
                field.setAccessible(true);
                field.set(bean, cache);
            } finally {
                field.setAccessible(isAccessible);
            }

            log.debug("已注入缓存: bean={}, field={}, area={}, name={}, type={}",
                    bean.getClass().getName(), field.getName(), area, name, cacheType);
        } catch (Exception e) {
            log.error("注入缓存失败: bean={}, field={}", bean.getClass().getName(), field.getName(), e);
        }
    }

    /**
     * 注入带加载器的缓存实例
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void injectCacheWithLoader(Object bean, Field field, String area, String name,
            com.easy.cache.annotation.CacheType cacheType,
            CacheConfig config, CacheLoader loader) {
        try {
            // 获取缓存泛型参数类型
            Type[] genericTypes = getGenericTypes(field);

            // 创建带加载器的缓存
            Cache<?, ?> cache;
            if (genericTypes != null && genericTypes.length == 2) {
                cache = cacheManager.getCache(area, name, cacheType);
            } else {
                log.warn("无法确定缓存泛型类型，使用普通缓存: bean={}, field={}",
                        bean.getClass().getName(), field.getName());
                cache = cacheManager.getCache(area, name, cacheType);
            }

            // 设置字段可访问
            boolean isAccessible = field.isAccessible();
            try {
                field.setAccessible(true);
                field.set(bean, cache);
            } finally {
                field.setAccessible(isAccessible);
            }

            log.debug("已注入带加载器的缓存: bean={}, field={}, area={}, name={}, type={}",
                    bean.getClass().getName(), field.getName(), area, name, cacheType);
        } catch (Exception e) {
            log.error("注入带加载器的缓存失败: bean={}, field={}", bean.getClass().getName(), field.getName(), e);
        }
    }

    /**
     * 获取缓存区域
     */
    private String getArea(String area, Field field) {
        if (area == null || area.isEmpty()) {
            return field.getDeclaringClass().getName();
        }
        return area;
    }

    /**
     * 获取缓存名称
     */
    private String getName(String name, Field field) {
        if (name == null || name.isEmpty()) {
            return field.getName();
        }
        return name;
    }

    /**
     * 获取字段的泛型类型
     */
    private Type[] getGenericTypes(Field field) {
        Type type = field.getGenericType();
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            return parameterizedType.getActualTypeArguments();
        }
        return null;
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }
}