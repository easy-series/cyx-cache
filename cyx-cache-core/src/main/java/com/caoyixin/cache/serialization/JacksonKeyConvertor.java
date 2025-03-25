package com.caoyixin.cache.serialization;

import com.caoyixin.cache.exception.CacheException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 基于Jackson的键转换器
 *
 * @param <K> 键类型
 */
public class JacksonKeyConvertor<K> implements KeyConvertor<K> {

    private final ObjectMapper objectMapper;

    /**
     * 创建Jackson键转换器
     */
    public JacksonKeyConvertor() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 创建Jackson键转换器
     *
     * @param objectMapper 自定义的ObjectMapper
     */
    public JacksonKeyConvertor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 将键对象转换为字符串
     *
     * @param key 键对象
     * @return JSON字符串
     */
    @Override
    public String convert(K key) {
        if (key == null) {
            return "null";
        }

        if (key instanceof String || key instanceof Number || key instanceof Boolean) {
            return key.toString();
        }

        try {
            return objectMapper.writeValueAsString(key);
        } catch (JsonProcessingException e) {
            throw new CacheException("使用Jackson序列化键失败: " + key, e);
        }
    }
}