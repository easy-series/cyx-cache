package com.caoyixin.cache.serialization;

import com.caoyixin.cache.exception.CacheException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 基于Jackson2的值编码器
 *
 * @param <T> 值类型
 */
public class Jackson2ValueEncoder<T> implements ValueEncoder<T> {

    private final ObjectMapper objectMapper;

    /**
     * 创建Jackson2值编码器
     */
    public Jackson2ValueEncoder() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 创建Jackson2值编码器
     *
     * @param objectMapper 自定义的ObjectMapper
     */
    public Jackson2ValueEncoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 将值对象编码为字节数组
     *
     * @param value 值对象
     * @return 字节数组
     */
    @Override
    public byte[] encode(T value) {
        if (value == null) {
            return new byte[0];
        }

        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (Exception e) {
            throw new CacheException("使用Jackson2序列化值失败: " + value, e);
        }
    }
}