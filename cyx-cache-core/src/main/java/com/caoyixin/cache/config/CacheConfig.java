package com.caoyixin.cache.config;

import com.caoyixin.cache.api.CacheType;
import com.caoyixin.cache.consistency.ConsistencyMode;
import com.caoyixin.cache.enums.ConsistencyType;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;

/**
 * 缓存配置
 */
@Getter
@Setter
public class CacheConfig {
    /**
     * 缓存名称
     */
    private String name;

    /**
     * 过期时间
     */
    private Duration expire;

    /**
     * 缓存类型
     */
    private CacheType cacheType = CacheType.REMOTE;

    /**
     * 本地缓存最大容量
     */
    private int localLimit = 200;

    /**
     * 获取缓存最大容量
     * 
     * @return 缓存最大容量
     */
    public int getMaxSize() {
        return localLimit;
    }

    /**
     * 本地缓存过期时间
     */
    private Duration localExpire;

    /**
     * 一致性模式
     */
    private ConsistencyMode consistencyMode = ConsistencyMode.WRITE_THROUGH;

    /**
     * 一致性类型
     */
    private ConsistencyType consistencyType;

    /**
     * 获取一致性类型
     * 
     * @return 一致性类型
     */
    public ConsistencyType getConsistencyType() {
        if (consistencyType != null) {
            return consistencyType;
        }

        // 从ConsistencyMode映射到ConsistencyType
        switch (consistencyMode) {
            case WRITE_THROUGH:
                return ConsistencyType.WRITE_THROUGH;
            case WRITE_BACK:
                return ConsistencyType.WRITE_BACK;
            case READ_ONLY:
                return ConsistencyType.READ_ONLY;
            default:
                return ConsistencyType.WRITE_THROUGH;
        }
    }

    /**
     * 是否同步本地缓存
     */
    private boolean syncLocal = true;

    /**
     * 键转换器类型
     */
    private String keyConvertor = "fastjson";

    /**
     * 值编码器类型
     */
    private String valueEncoder = "java";

    /**
     * 值解码器类型
     */
    private String valueDecoder = "java";

    /**
     * 是否启用缓存统计
     */
    private boolean statsEnabled = true;

    /**
     * 是否防止缓存击穿
     */
    private boolean penetrationProtect = false;

    /**
     * 刷新策略配置
     */
    private RefreshPolicy refreshPolicy;

    /**
     * 获取本地缓存配置
     *
     * @return 本地缓存配置
     */
    public CacheConfig localConfig() {
        CacheConfig config = new CacheConfig();
        config.setName(this.name + ":local");
        config.setCacheType(CacheType.LOCAL);
        config.setLocalLimit(this.localLimit);
        config.setExpire(this.localExpire != null ? this.localExpire : this.expire);
        config.setKeyConvertor(this.keyConvertor);
        config.setValueEncoder(this.valueEncoder);
        config.setValueDecoder(this.valueDecoder);
        config.setStatsEnabled(this.statsEnabled);
        config.setPenetrationProtect(this.penetrationProtect);
        config.setRefreshPolicy(this.refreshPolicy);
        return config;
    }

    /**
     * 获取远程缓存配置
     *
     * @return 远程缓存配置
     */
    public CacheConfig remoteConfig() {
        CacheConfig config = new CacheConfig();
        config.setName(this.name + ":remote");
        config.setCacheType(CacheType.REMOTE);
        config.setExpire(this.expire);
        config.setKeyConvertor(this.keyConvertor);
        config.setValueEncoder(this.valueEncoder);
        config.setValueDecoder(this.valueDecoder);
        config.setStatsEnabled(this.statsEnabled);
        config.setPenetrationProtect(this.penetrationProtect);
        config.setRefreshPolicy(this.refreshPolicy);
        return config;
    }

    /**
     * 创建缓存配置构建器
     * 
     * @param name 缓存名称
     * @return 构建器
     */
    public static Builder newBuilder(String name) {
        return new Builder(name);
    }

    /**
     * 缓存配置构建器
     */
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

        public Builder localExpire(Duration localExpire) {
            config.localExpire = localExpire;
            return this;
        }

        public Builder localLimit(int localLimit) {
            config.localLimit = localLimit;
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

        public Builder keyConvertor(String keyConvertor) {
            config.keyConvertor = keyConvertor;
            return this;
        }

        public Builder valueEncoder(String valueEncoder) {
            config.valueEncoder = valueEncoder;
            return this;
        }

        public Builder valueDecoder(String valueDecoder) {
            config.valueDecoder = valueDecoder;
            return this;
        }

        public Builder statsEnabled(boolean statsEnabled) {
            config.statsEnabled = statsEnabled;
            return this;
        }

        public Builder penetrationProtect(boolean penetrationProtect) {
            config.penetrationProtect = penetrationProtect;
            return this;
        }

        public Builder refreshPolicy(RefreshPolicy refreshPolicy) {
            config.refreshPolicy = refreshPolicy;
            return this;
        }

        public CacheConfig build() {
            return config;
        }
    }
}