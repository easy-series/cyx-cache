package com.caoyixin.cache.serialization;

import com.caoyixin.cache.exception.CacheException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * 基于Java反序列化的值解码器
 */
public class JavaValueDecoder implements ValueDecoder<Object> {

    /**
     * 将字节数组解码为值对象
     *
     * @param bytes 字节数组
     * @return 值对象
     */
    @Override
    public Object decode(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new CacheException("使用Java反序列化值失败", e);
        }
    }
}