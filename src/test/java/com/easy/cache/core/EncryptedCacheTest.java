package com.easy.cache.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.easy.cache.security.Encryptor;

/**
 * 加密缓存测试类
 */
public class EncryptedCacheTest {
    
    private EncryptedCache<String, String> cache;
    
    @Mock
    private Cache<String, byte[]> delegate;
    
    @Mock
    private Encryptor encryptor;
    
    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        
        // 创建加密缓存
        cache = new EncryptedCache<>(delegate, encryptor);
        
        // 配置模拟行为
        when(encryptor.encrypt(any())).thenAnswer(invocation -> {
            byte[] data = invocation.getArgument(0);
            // 简单的模拟加密，实际上只是返回原始数据
            return data;
        });
        
        when(encryptor.decrypt(any())).thenAnswer(invocation -> {
            byte[] data = invocation.getArgument(0);
            // 简单的模拟解密，实际上只是返回原始数据
            return data;
        });
    }
    
    @Test
    public void testPutAndGet() throws Exception {
        // 准备数据
        String key = "key1";
        String value = "value1";
        byte[] serializedValue = SerializationUtils.serialize(value);
        
        // 配置模拟行为
        when(delegate.get(key)).thenReturn(serializedValue);
        
        // 放入缓存
        cache.put(key, value);
        
        // 验证put调用
        verify(encryptor).encrypt(any());
        verify(delegate).put(eq(key), any(byte[].class), anyLong(), any());
        
        // 获取缓存
        String cachedValue = cache.get(key);
        
        // 验证
        assertEquals(value, cachedValue);
        verify(delegate).get(key);
        verify(encryptor).decrypt(any());
    }
    
    @Test
    public void testGetWithLoader() throws Exception {
        // 准备数据
        String key = "key2";
        String value = "loaded-key2";
        
        // 配置模拟行为 - 键不存在
        when(delegate.get(key)).thenReturn(null);
        
        // 使用加载器获取不存在的键
        String cachedValue = cache.get(key, k -> "loaded-" + k);
        
        // 验证加载器被调用
        assertEquals(value, cachedValue);
        verify(delegate).get(key);
        // 加载的值应该被加密并放入缓存
        verify(encryptor).encrypt(any());
        verify(delegate).put(eq(key), any(byte[].class), anyLong(), any());
    }
    
    @Test
    public void testRemove() {
        // 准备数据
        String key = "key3";
        
        // 配置模拟行为
        when(delegate.remove(key)).thenReturn(true);
        
        // 移除缓存
        boolean removed = cache.remove(key);
        
        // 验证
        assertTrue(removed);
        verify(delegate).remove(key);
    }
} 