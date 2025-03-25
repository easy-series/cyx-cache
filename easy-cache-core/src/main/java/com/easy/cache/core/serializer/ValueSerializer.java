package com.easy.cache.core.serializer;

/**
 * 缓存值序列化器接口
 *
 * @param <V> 值类型
 */
public interface ValueSerializer<V> {

    /**
     * 将值序列化为字节数组
     *
     * @param value 值
     * @return 序列化后的字节数组
     */
    byte[] serialize(V value);

    /**
     * 将字节数组反序列化为值
     *
     * @param bytes 字节数组
     * @return 值对象
     */
    V deserialize(byte[] bytes);

    /**
     * 获取值的类型
     *
     * @return 值的类型
     */
    Class<V> getValueType();
}