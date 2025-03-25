package com.easy.cache.remote.serializer;

import com.easy.cache.core.serializer.ValueSerializer;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Kryo值序列化器
 * <p>
 * 使用Kryo序列化库，性能高于Java内置序列化
 *
 * @param <V> 值类型
 */
@Slf4j
public class KryoValueSerializer<V> implements ValueSerializer<V> {

    private static final int BUFFER_SIZE = 1024;
    private static final int MAX_BUFFER_SIZE = 10 * 1024 * 1024; // 10MB

    private final Class<V> valueType;
    private final ThreadLocal<Kryo> kryoThreadLocal = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(false);
        kryo.register(valueType);
        return kryo;
    });

    /**
     * 创建KryoValueSerializer实例
     *
     * @param valueType 值类型
     */
    public KryoValueSerializer(Class<V> valueType) {
        this.valueType = valueType;
    }

    @Override
    public byte[] serialize(V value) {
        if (value == null) {
            return null;
        }

        Kryo kryo = kryoThreadLocal.get();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(BUFFER_SIZE);
        Output output = new Output(bos, MAX_BUFFER_SIZE);

        try {
            kryo.writeObject(output, value);
            output.flush();
            return bos.toByteArray();
        } catch (Exception e) {
            log.error("Kryo序列化失败: {}", e.getMessage(), e);
            throw new RuntimeException("Kryo序列化失败", e);
        } finally {
            output.close();
        }
    }

    @Override
    public V deserialize(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        Kryo kryo = kryoThreadLocal.get();
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        Input input = new Input(bis);

        try {
            return kryo.readObject(input, valueType);
        } catch (Exception e) {
            log.error("Kryo反序列化失败: {}", e.getMessage(), e);
            throw new RuntimeException("Kryo反序列化失败", e);
        } finally {
            input.close();
        }
    }

    @Override
    public Class<V> getValueType() {
        return valueType;
    }
}