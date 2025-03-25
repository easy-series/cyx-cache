package com.caoyixin.cache.spring;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 缓存配置属性
 */
@ConfigurationProperties(prefix = "cyx.cache")
@Data
public class CacheProperties {

    /**
     * 实例ID，默认会自动生成
     */
    private String instanceId;

    /**
     * 本地缓存配置
     */
    private LocalCacheProperties local = new LocalCacheProperties();

    /**
     * Redis缓存配置
     */
    private RedisCacheProperties redis = new RedisCacheProperties();

    /**
     * 本地缓存配置
     */
    @Data
    public static class LocalCacheProperties {
        /**
         * 是否启用本地缓存
         */
        private boolean enabled = true;

        /**
         * 本地缓存类型（simple或caffeine）
         */
        private String type = "simple";

        /**
         * 默认最大缓存条目数
         */
        private int defaultMaxSize = 1000;

        /**
         * 默认过期时间（秒）
         */
        private long defaultExpireSeconds = 300;
    }

    /**
     * Redis缓存配置
     */
    @Data
    public static class RedisCacheProperties {
        /**
         * 是否启用Redis缓存
         */
        private boolean enabled = false;

        /**
         * 键前缀，用于区分不同应用的缓存
         */
        private String keyPrefix = "cyx:cache:";

        /**
         * 主题前缀，用于发布订阅消息
         */
        private String topicPrefix = "cyx.cache";

        /**
         * 默认过期时间（秒）
         */
        private long defaultExpireSeconds = 1800;
    }
}