package com.easy.cache.remote.serializer;

import com.easy.cache.core.serializer.KeySerializer;
import com.easy.cache.core.serializer.ValueSerializer;
import lombok.extern.slf4j.Slf4j;

/**
 * 序列化器工厂
 * <p>
 * 用于创建各种类型的序列化器
 */
@Slf4j
public class SerializerFactory {

    /**
     * 序列化器类型枚举
     */
    public enum SerializerType {
        /**
         * Java原生序列化
         */
        JAVA,
        /**
         * Kryo序列化
         */
        KRYO,
        /**
         * Jackson序列化
         */
        JACKSON
    }

    /**
     * 创建键序列化器
     *
     * @param <K> 键类型
     * @return 键序列化器
     */
    public static <K> KeySerializer<K> createStringKeySerializer() {
        return (KeySerializer<K>) new StringKeySerializer();
    }

    /**
     * 创建值序列化器
     *
     * @param valueType      值类型
     * @param serializerType 序列化器类型
     * @param <V>            值类型
     * @return 值序列化器
     */
    public static <V> ValueSerializer<V> createValueSerializer(Class<V> valueType, SerializerType serializerType) {
        if (valueType == null) {
            throw new IllegalArgumentException("值类型不能为空");
        }

        if (serializerType == null) {
            serializerType = SerializerType.JAVA;
            log.warn("序列化器类型为空，使用默认的Java序列化器");
        }

        switch (serializerType) {
            case JAVA:
                log.debug("使用Java序列化器");
                return new JavaValueSerializer<>(valueType);
            case KRYO:
                log.debug("使用Kryo序列化器");
                return new KryoValueSerializer<>(valueType);
            case JACKSON:
                log.debug("使用Jackson序列化器");
                return new JacksonValueSerializer<>(valueType);
            default:
                log.warn("未知的序列化器类型：{}，使用默认的Java序列化器", serializerType);
                return new JavaValueSerializer<>(valueType);
        }
    }
}