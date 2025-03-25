package com.caoyixin.cache.serialization;

/**
 * 字符串键转换器，直接使用toString方法
 * 
 * @param <K> 键类型
 */
public class StringKeyConvertor<K> implements KeyConvertor<K> {

    /**
     * 将键对象转换为字符串
     * 
     * @param key 键对象
     * @return 字符串表示
     */
    @Override
    public String convert(K key) {
        return key == null ? "null" : key.toString();
    }
}