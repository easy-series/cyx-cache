package com.caoyixin.cache.config;

import lombok.Getter;
import lombok.Setter;

import java.time.Duration;

/**
 * 缓存刷新策略配置
 */
@Getter
@Setter
public class RefreshPolicy {
    /**
     * 是否启用自动刷新
     */
    private boolean enabled = false;

    /**
     * 刷新间隔时间
     */
    private Duration refreshInterval;

    /**
     * 最后一次访问后停止刷新的时间
     */
    private Duration stopRefreshAfterLastAccess;

    /**
     * 是否使用异步刷新
     */
    private boolean asyncRefresh = true;

    /**
     * 创建刷新策略
     *
     * @return 刷新策略
     */
    public static RefreshPolicy newPolicy() {
        return new RefreshPolicy();
    }

    /**
     * 设置刷新间隔
     *
     * @param refreshInterval 刷新间隔
     * @return 当前对象
     */
    public RefreshPolicy refreshInterval(Duration refreshInterval) {
        this.refreshInterval = refreshInterval;
        this.enabled = true;
        return this;
    }

    /**
     * 设置最后一次访问后停止刷新的时间
     *
     * @param duration 停止刷新的时间
     * @return 当前对象
     */
    public RefreshPolicy stopRefreshAfterLastAccess(Duration duration) {
        this.stopRefreshAfterLastAccess = duration;
        return this;
    }

    /**
     * 设置是否使用异步刷新
     *
     * @param asyncRefresh 是否异步刷新
     * @return 当前对象
     */
    public RefreshPolicy asyncRefresh(boolean asyncRefresh) {
        this.asyncRefresh = asyncRefresh;
        return this;
    }
}