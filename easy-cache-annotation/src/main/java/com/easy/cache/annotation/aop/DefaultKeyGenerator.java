package com.easy.cache.annotation.aop;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 默认缓存键生成器
 * <p>
 * 使用方法名和参数的哈希码生成缓存键
 */
public class DefaultKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Method method, Object[] arguments) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.getDeclaringClass().getName())
          .append(".")
          .append(method.getName());

        if (arguments != null && arguments.length > 0) {
            sb.append(":");
            
            for (Object arg : arguments) {
                if (arg == null) {
                    sb.append("null");
                } else if (arg.getClass().isArray()) {
                    // 处理数组参数
                    sb.append(arrayToString(arg));
                } else {
                    sb.append(arg.toString());
                }
                sb.append(",");
            }
            // 移除最后一个逗号
            sb.deleteCharAt(sb.length() - 1);
        }

        return sb.toString();
    }

    /**
     * 数组转字符串
     */
    private String arrayToString(Object array) {
        if (array instanceof Object[]) {
            return Arrays.deepToString((Object[]) array);
        } else if (array instanceof boolean[]) {
            return Arrays.toString((boolean[]) array);
        } else if (array instanceof byte[]) {
            return Arrays.toString((byte[]) array);
        } else if (array instanceof char[]) {
            return Arrays.toString((char[]) array);
        } else if (array instanceof double[]) {
            return Arrays.toString((double[]) array);
        } else if (array instanceof float[]) {
            return Arrays.toString((float[]) array);
        } else if (array instanceof int[]) {
            return Arrays.toString((int[]) array);
        } else if (array instanceof long[]) {
            return Arrays.toString((long[]) array);
        } else if (array instanceof short[]) {
            return Arrays.toString((short[]) array);
        }
        return array.toString();
    }
} 