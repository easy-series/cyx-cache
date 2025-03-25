package com.caoyixin.cache.serialization;

/**
 * 值解码器，将字节数组解码为缓存值
 *
 * @param <V> 值类型
 */
public interface ValueDecoder<V> {
    /**
     * 将字节数组解码为原始值
     *
     * @param bytes 字节数组
     * @return 解码后的原始值
     */
    V decode(byte[] bytes);
}