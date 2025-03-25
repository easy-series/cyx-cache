package com.caoyixin.cache.serialization;

/**
 * 字符串键转换器，直接使用toString方法
 */
public class StringKeyConvertor implements KeyConvertor<Object> {
    
    /**
     * 将键对象转换为字符串
     * 
     * @param key 键对象
     * @return 字符串表示
     */
    @Override
    public String convert(Object key) {
        return key == null ? "null" : key.toString();
    }
} 