package com.caoyixin.cache.spring;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring Bean工具类
 */
@Component
public class SpringBeanUtils implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    /**
     * 获取Bean
     *
     * @param type Bean类型
     * @param <T>  Bean类型
     * @return Bean对象
     */
    public static <T> T getBean(Class<T> type) {
        if (applicationContext == null) {
            return null;
        }
        try {
            return applicationContext.getBean(type);
        } catch (BeansException e) {
            return null;
        }
    }

    /**
     * 获取Bean
     *
     * @param name Bean名称
     * @param <T>  Bean类型
     * @return Bean对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T getBean(String name) {
        if (applicationContext == null) {
            return null;
        }
        try {
            return (T) applicationContext.getBean(name);
        } catch (BeansException e) {
            return null;
        }
    }

    /**
     * 获取应用上下文
     *
     * @return 应用上下文
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        applicationContext = context;
    }
}