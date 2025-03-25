package com.easy.cache.remote.serializer;

import com.easy.cache.core.serializer.ValueSerializer;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Jackson值序列化器
 * <p>
 * 使用Jackson序列化库，将对象序列化为JSON格式
 *
 * @param <V> 值类型
 */
@Slf4j
public class JacksonValueSerializer<V> implements ValueSerializer<V> {

    private final Class<V> valueType;
    private final ObjectMapper objectMapper;

    /**
     * 创建JacksonValueSerializer实例
     *
     * @param valueType 值类型
     */
    public JacksonValueSerializer(Class<V> valueType) {
        this.valueType = valueType;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        this.objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    @Override
    public byte[] serialize(V value) {
        if (value == null) {
            return null;
        }

        try {
            String jsonString = objectMapper.writeValueAsString(value);
            return jsonString.getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            log.error("Jackson序列化失败: {}", e.getMessage(), e);
            throw new RuntimeException("Jackson序列化失败", e);
        }
    }

    @Override
    public V deserialize(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        try {
            String jsonString = new String(bytes, StandardCharsets.UTF_8);
            return objectMapper.readValue(jsonString, valueType);
        } catch (IOException e) {
            log.error("Jackson反序列化失败: {}", e.getMessage(), e);
            throw new RuntimeException("Jackson反序列化失败", e);
        }
    }

    @Override
    public Class<V> getValueType() {
        return valueType;
    }
}