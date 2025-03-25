package com.caoyixin.cache.serialization;

/**
 * 键转换器，将缓存键转换为字符串
 *
 * @param <K> 键类型
 */
public interface KeyConvertor<K> {
    /**
     * 将键转换为字符串
     *
     * @param key 源键
     * @return 转换后的字符串
     */
    String convert(K key);
}