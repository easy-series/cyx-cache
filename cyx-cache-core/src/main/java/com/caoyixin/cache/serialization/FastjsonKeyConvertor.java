package com.caoyixin.cache.serialization;

import com.alibaba.fastjson2.JSON;
import com.caoyixin.cache.exception.CacheException;

/**
 * 基于Fastjson的键转换器
 */
public class FastjsonKeyConvertor implements KeyConvertor<Object> {

    /**
     * 将键对象转换为字符串
     * 
     * @param key 键对象
     * @return JSON字符串
     */
    @Override
    public String convert(Object key) {
        if (key == null) {
            return "null";
        }

        if (key instanceof String || key instanceof Number || key instanceof Boolean) {
            return key.toString();
        }

        try {
            return JSON.toJSONString(key);
        } catch (Exception e) {
            throw new CacheException("使用Fastjson序列化键失败: " + key, e);
        }
    }
}