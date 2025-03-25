package com.caoyixin.cache.serialization;

import com.caoyixin.cache.exception.CacheException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 基于Jackson2的值解码器
 */
public class Jackson2ValueDecoder<T> implements ValueDecoder<T> {

    private final ObjectMapper objectMapper;
    private final JavaType javaType;

    /**
     * 创建Jackson2值解码器
     *
     * @param objectMapper 对象映射器
     * @param valueType    值类型
     */
    public Jackson2ValueDecoder(ObjectMapper objectMapper, Class<T> valueType) {
        this.objectMapper = objectMapper;
        this.javaType = objectMapper.getTypeFactory().constructType(valueType);
    }

    /**
     * 创建Jackson2值解码器
     *
     * @param valueType 值类型
     */
    public Jackson2ValueDecoder(Class<T> valueType) {
        this(new ObjectMapper(), valueType);
    }

    /**
     * 将字节数组解码为值对象
     *
     * @param bytes 字节数组
     * @return 值对象
     */
    @Override
    public T decode(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        try {
            return objectMapper.readValue(bytes, javaType);
        } catch (Exception e) {
            throw new CacheException("使用Jackson2反序列化值失败", e);
        }
    }
}