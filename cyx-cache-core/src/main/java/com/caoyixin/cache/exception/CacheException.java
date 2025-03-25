package com.caoyixin.cache.exception;

/**
 * 缓存异常类
 */
public class CacheException extends RuntimeException {

    /**
     * 创建缓存异常
     * 
     * @param message 异常信息
     */
    public CacheException(String message) {
        super(message);
    }

    /**
     * 创建缓存异常
     * 
     * @param message 异常信息
     * @param cause   原始异常
     */
    public CacheException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 创建缓存异常
     * 
     * @param cause 原始异常
     */
    public CacheException(Throwable cause) {
        super(cause);
    }
}