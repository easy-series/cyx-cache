package com.caoyixin.cache.serialization;

import com.caoyixin.cache.exception.CacheException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * 基于Java序列化的值编码器
 */
public class JavaValueEncoder implements ValueEncoder<Object> {

    /**
     * 将值对象编码为字节数组
     * 
     * @param value 值对象
     * @return 字节数组
     */
    @Override
    public byte[] encode(Object value) {
        if (value == null) {
            return new byte[0];
        }

        if (!(value instanceof Serializable)) {
            throw new CacheException("值对象不可序列化: " + value.getClass().getName());
        }

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(value);
            oos.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new CacheException("使用Java序列化值失败: " + value, e);
        }
    }
}