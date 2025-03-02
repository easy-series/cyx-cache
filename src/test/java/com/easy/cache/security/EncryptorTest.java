package com.easy.cache.security;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 加密器测试类
 */
public class EncryptorTest {
    
    private Encryptor encryptor;
    
    @BeforeEach
    public void setUp() throws Exception {
        // 创建AES加密器
        encryptor = new AesEncryptor("test-secret-key-12345");
    }
    
    @Test
    public void testEncryptAndDecrypt() throws Exception {
        // 准备测试数据
        String original = "敏感数据测试";
        byte[] originalBytes = original.getBytes(StandardCharsets.UTF_8);
        
        // 加密
        byte[] encrypted = encryptor.encrypt(originalBytes);
        
        // 验证加密后的数据与原始数据不同
        assertNotEquals(new String(originalBytes), new String(encrypted));
        
        // 解密
        byte[] decrypted = encryptor.decrypt(encrypted);
        
        // 验证解密后的数据与原始数据相同
        assertEquals(original, new String(decrypted, StandardCharsets.UTF_8));
    }
    
    @Test
    public void testEncryptionConsistency() throws Exception {
        // 准备测试数据
        String original = "一致性测试";
        byte[] originalBytes = original.getBytes(StandardCharsets.UTF_8);
        
        // 多次加密同一数据
        byte[] encrypted1 = encryptor.encrypt(originalBytes);
        byte[] encrypted2 = encryptor.encrypt(originalBytes);
        
        // 验证每次加密结果可能不同（由于IV或填充）
        // 但解密结果应该相同
        byte[] decrypted1 = encryptor.decrypt(encrypted1);
        byte[] decrypted2 = encryptor.decrypt(encrypted2);
        
        assertEquals(original, new String(decrypted1, StandardCharsets.UTF_8));
        assertEquals(original, new String(decrypted2, StandardCharsets.UTF_8));
    }
    
    @Test
    public void testLargeData() throws Exception {
        // 准备大量测试数据
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            sb.append("大量数据测试-").append(i).append(",");
        }
        String original = sb.toString();
        byte[] originalBytes = original.getBytes(StandardCharsets.UTF_8);
        
        // 加密
        byte[] encrypted = encryptor.encrypt(originalBytes);
        
        // 解密
        byte[] decrypted = encryptor.decrypt(encrypted);
        
        // 验证
        assertEquals(original, new String(decrypted, StandardCharsets.UTF_8));
    }
} 