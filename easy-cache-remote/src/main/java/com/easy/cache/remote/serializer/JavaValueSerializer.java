package com.easy.cache.remote.serializer;

import com.easy.cache.core.serializer.ValueSerializer;
import lombok.extern.slf4j.Slf4j;

import java.io.*;

/**
 * Java值序列化器
 * <p>
 * 使用Java内置的序列化机制
 *
 * @param <V> 值类型
 */
@Slf4j
public class JavaValueSerializer<V> implements ValueSerializer<V> {

    private final Class<V> valueType;

    /**
     * 创建JavaValueSerializer实例
     *
     * @param valueType 值类型
     */
    public JavaValueSerializer(Class<V> valueType) {
        this.valueType = valueType;
    }

    @Override
    public byte[] serialize(V value) {
        if (value == null) {
            return null;
        }

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(value);
            return bos.toByteArray();
        } catch (IOException e) {
            log.error("Java序列化失败: {}", e.getMessage(), e);
            throw new RuntimeException("Java序列化失败", e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public V deserialize(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
                ObjectInputStream ois = new ObjectInputStream(bis)) {
            Object obj = ois.readObject();
            if (valueType.isInstance(obj)) {
                return (V) obj;
            } else {
                log.warn("Java反序列化类型不匹配: 期望类型={}, 实际类型={}",
                        valueType.getName(), obj.getClass().getName());
                return null;
            }
        } catch (IOException | ClassNotFoundException e) {
            log.error("Java反序列化失败: {}", e.getMessage(), e);
            throw new RuntimeException("Java反序列化失败", e);
        }
    }

    @Override
    public Class<V> getValueType() {
        return valueType;
    }
}