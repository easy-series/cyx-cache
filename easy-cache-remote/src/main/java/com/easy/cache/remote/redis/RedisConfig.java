package com.easy.cache.remote.redis;

import lombok.Builder;
import lombok.Data;

/**
 * Redis缓存配置
 */
@Data
@Builder
public class RedisConfig {

    /**
     * Redis连接类型
     */
    public enum RedisType {
        /**
         * 单机模式
         */
        STANDALONE,
        /**
         * 哨兵模式
         */
        SENTINEL,
        /**
         * 集群模式
         */
        CLUSTER
    }

    /**
     * Redis客户端类型
     */
    public enum ClientType {
        /**
         * Jedis客户端
         */
        JEDIS,
        /**
         * Lettuce客户端
         */
        LETTUCE,
        /**
         * Redisson客户端
         */
        REDISSON
    }

    /**
     * Redis类型，默认为单机模式
     */
    @Builder.Default
    private RedisType redisType = RedisType.STANDALONE;

    /**
     * 客户端类型，默认为Jedis
     */
    @Builder.Default
    private ClientType clientType = ClientType.JEDIS;

    /**
     * Redis服务器地址
     */
    private String host;

    /**
     * Redis服务器端口
     */
    @Builder.Default
    private int port = 6379;

    /**
     * 密码
     */
    private String password;

    /**
     * 数据库索引
     */
    @Builder.Default
    private int database = 0;

    /**
     * 连接超时时间（毫秒）
     */
    @Builder.Default
    private int connectionTimeout = 2000;

    /**
     * 读取超时时间（毫秒）
     */
    @Builder.Default
    private int readTimeout = 2000;

    /**
     * 哨兵/集群节点，用逗号分隔，如 host1:port1,host2:port2
     */
    private String nodes;

    /**
     * 哨兵模式下的主节点名称
     */
    private String masterName;

    /**
     * 连接池最大连接数
     */
    @Builder.Default
    private int maxTotal = 8;

    /**
     * 连接池最大空闲连接数
     */
    @Builder.Default
    private int maxIdle = 8;

    /**
     * 连接池最小空闲连接数
     */
    @Builder.Default
    private int minIdle = 0;

    /**
     * 连接池中连接的最大等待时间（毫秒）
     */
    @Builder.Default
    private long maxWait = -1;
}