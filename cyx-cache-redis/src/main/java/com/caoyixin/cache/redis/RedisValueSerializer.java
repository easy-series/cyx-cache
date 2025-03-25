package com.caoyixin.cache.redis;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

/**
 * Redis值序列化器，直接传递字节数组，不进行任何转换
 */
public class RedisValueSerializer implements RedisSerializer<byte[]> {

    /**
     * 单例实例
     */
    public static final RedisValueSerializer INSTANCE = new RedisValueSerializer();

    private RedisValueSerializer() {
        // 私有构造函数
    }

    @Override
    public byte[] serialize(byte[] bytes) throws SerializationException {
        return bytes;
    }

    @Override
    public byte[] deserialize(byte[] bytes) throws SerializationException {
        return bytes;
    }
}