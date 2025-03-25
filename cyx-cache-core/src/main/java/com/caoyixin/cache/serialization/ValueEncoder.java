package com.caoyixin.cache.serialization;

/**
 * 值编码器，将缓存值编码为字节数组
 *
 * @param <V> 值类型
 */
public interface ValueEncoder<V> {
    /**
     * 将值编码为字节数组
     *
     * @param value 原始值
     * @return 编码后的字节数组
     */
    byte[] encode(V value);
}