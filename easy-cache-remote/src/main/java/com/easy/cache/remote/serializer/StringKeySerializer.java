package com.easy.cache.remote.serializer;

import com.easy.cache.core.serializer.KeySerializer;
import java.nio.charset.StandardCharsets;

/**
 * 字符串键序列化器
 * <p>
 * 用于将字符串类型的键序列化为字节数组
 */
public class StringKeySerializer implements KeySerializer<String> {

    /**
     * 单例实例
     */
    public static final StringKeySerializer INSTANCE = new StringKeySerializer();

    private StringKeySerializer() {
        // 私有构造函数
    }

    @Override
    public byte[] serialize(String key) {
        if (key == null) {
            return null;
        }
        return key.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String deserialize(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public String toString(String key) {
        return key;
    }
}