package com.easy.cache.core.serializer;

/**
 * 缓存键序列化器接口
 *
 * @param <K> 键类型
 */
public interface KeySerializer<K> {

    /**
     * 将键序列化为字节数组
     *
     * @param key 键
     * @return 序列化后的字节数组
     */
    byte[] serialize(K key);

    /**
     * 将字节数组反序列化为键
     *
     * @param bytes 字节数组
     * @return 键对象
     */
    K deserialize(byte[] bytes);

    /**
     * 将键转换为字符串
     *
     * @param key 键
     * @return 字符串表示
     */
    String toString(K key);
} 